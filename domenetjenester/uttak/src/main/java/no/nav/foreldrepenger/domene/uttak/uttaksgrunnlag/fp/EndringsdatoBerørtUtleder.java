package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.TidsperiodeFarRundtFødsel;
import no.nav.foreldrepenger.domene.uttak.TidsperiodeForbeholdtMor;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

public final class EndringsdatoBerørtUtleder {

    private static final Logger LOG = LoggerFactory.getLogger(EndringsdatoBerørtUtleder.class);

    private EndringsdatoBerørtUtleder() {
    }

    public static Optional<LocalDate> utledEndringsdatoForBerørtBehandling(ForeldrepengerUttak utløsendeUttak,
                                                                           Optional<YtelseFordelingAggregat> utløsendeBehandlingYtelseFordeling,
                                                                           Behandlingsresultat utløsendeBehandlingsresultat,
                                                                           boolean negativSaldoNoenKonto,
                                                                           Optional<ForeldrepengerUttak> berørtUttakOpt,
                                                                           UttakInput uttakInput,
                                                                           String loggPrefix) {
        ForeldrepengerGrunnlag fpGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();
        var familieHendelser = fpGrunnlag.getFamilieHendelser();
        var kreverSammenhengendeUttak = uttakInput.getBehandlingReferanse().getSkjæringstidspunkt().kreverSammenhengendeUttak();
        var utenMinsterett = uttakInput.getBehandlingReferanse().getSkjæringstidspunkt().utenMinsterett();
        if (berørtUttakOpt.isEmpty() || finnMinAktivDato(berørtUttakOpt.get()).isEmpty() || finnMinAktivDato(utløsendeUttak, berørtUttakOpt.get()).isEmpty()) {
            return Optional.empty();
        }
        var berørtUttak = berørtUttakOpt.get();
        // Endring fra en søknadsperiode eller fra start?
        var endringsdato = utløsendeBehandlingYtelseFordeling.flatMap(YtelseFordelingAggregat::getGjeldendeEndringsdatoHvisEksisterer)
            .orElseGet(() -> {
                //mangler endringsdato ved utsatt oppstart behandlinger.
                return finnMinAktivDato(utløsendeUttak, berørtUttak).orElseThrow();
            });

        Set<LocalDate> berørtBehovDatoer = new HashSet<>();
        if (utløsendeBehandlingsresultat.isEndretStønadskonto()) {
            LOG.info("{}: EndretKonto endringsdato {}", loggPrefix, endringsdato);
            berørtBehovDatoer.add(berørtUttak.finnFørsteUttaksdato().orElseThrow());
        }

        if (negativSaldoNoenKonto) {
            LOG.info("{}: NegativKonto endringsdato {}", loggPrefix, endringsdato);
            berørtBehovDatoer.add(endringsdato);
            if (berørtUttak.sistDagMedTrekkdager().isBefore(endringsdato)) {
                //Vurdere om vi skal sette endringsdato til første i berørt uttak, eller ikke opprette noen berørt
                LOG.info("Berørt uttak har ikke trekkdager etter utløsende behandlings endringsdato {}. Siste dag med trekkdager {}",
                    endringsdato, berørtUttak.sistDagMedTrekkdager());
            }
        }

        var periodeTom = finnMaxAktivDato(utløsendeUttak, berørtUttak).filter(endringsdato::isBefore).orElse(endringsdato);
        var periodeFomEndringsdato = new LocalDateInterval(endringsdato, periodeTom);

        var overlapp = overlappSomIkkeErFulltSamtidigUttak(familieHendelser, utenMinsterett, periodeFomEndringsdato, utløsendeUttak,
            berørtUttak);
        if (overlapp.isPresent()) {
            LOG.info("{}: OverlappUtenSamtidig fom {} endringsdato {}", loggPrefix, overlapp.get(), endringsdato);
            berørtBehovDatoer.add(overlapp.get());
        }

        var fellesTidslinjeForSammenheng = tidslinjeForSammenhengendeUttaksplan(utløsendeUttak, berørtUttak);
        // Sikre at periode reservert mor er komplett med uttak, utsettelser, overføringer
        if (familieHendelser.gjelderTerminFødsel()) {
            var familieHendelseDato = familieHendelser.getGjeldendeFamilieHendelse().getFamilieHendelseDato();
            var førsteSeksUker = new LocalDateInterval(VirkedagUtil.lørdagSøndagTilMandag(familieHendelseDato),
                TidsperiodeForbeholdtMor.tilOgMed(familieHendelseDato));
            if (!fellesTidslinjeForSammenheng.isContinuous(førsteSeksUker)) {
                var tidslinjeFørsteSeksUker = fellesTidslinjeForSammenheng.intersection(førsteSeksUker);
                var tidligsteGap = Optional.ofNullable(tidslinjeFørsteSeksUker.firstDiscontinuity()).map(LocalDateInterval::getFomDato).orElse(endringsdato);
                LOG.info("{}: Første 6 uker gap fom {} endringsdato {}", loggPrefix, tidligsteGap, endringsdato);
                berørtBehovDatoer.add(tidligsteGap);
            }
        }

        var opprett = kreverSammenhengendeUttak && !fellesTidslinjeForSammenheng.intersection(periodeFomEndringsdato).isEmpty() &&
            !fellesTidslinjeForSammenheng.isContinuous(periodeFomEndringsdato);
        if (opprett) {
            var tidslinjeFomEndringsdato = fellesTidslinjeForSammenheng.intersection(periodeFomEndringsdato);
            var tidligsteGap = Optional.ofNullable(tidslinjeFomEndringsdato.firstDiscontinuity()).map(LocalDateInterval::getFomDato).orElse(endringsdato);
            LOG.info("{}: Sammenhengende etter uke 6 gap fom {} endringsdato {}", loggPrefix, tidligsteGap, endringsdato);
            berørtBehovDatoer.add(tidligsteGap);
        }
        return berørtBehovDatoer.stream().min(Comparator.naturalOrder());
    }

    private static Optional<LocalDate> overlappSomIkkeErFulltSamtidigUttak(FamilieHendelser familieHendelser,
                                                                           boolean utenMinsterett,
                                                                           LocalDateInterval periodeFomEndringsdato,
                                                                           ForeldrepengerUttak brukersUttak,
                                                                           ForeldrepengerUttak annenpartsUttak) {
        var tidslinjeBruker = lagTidslinje(brukersUttak, p -> !p.isOpphold(), EndringsdatoBerørtUtleder::helgFomMandagSegment);
        var tidslinjeAnnenpart = lagTidslinje(annenpartsUttak, p -> !p.isOpphold() && p.erFraSøknad(), EndringsdatoBerørtUtleder::helgFomMandagSegment);
        // Tidslinje der begge har uttak - fom endringsdato.
        var tidslinjeOverlappendeUttakFomEndringsdato = tidslinjeAnnenpart.intersection(tidslinjeBruker).intersection(periodeFomEndringsdato);
        if (tidslinjeOverlappendeUttakFomEndringsdato.isEmpty()) {
            return Optional.empty();
        }

        var farRundtFødsel = TidsperiodeFarRundtFødsel.intervallFarRundtFødsel(familieHendelser, utenMinsterett).orElse(null);
        var tidslinjeBrukerSamtidig = lagTidslinje(brukersUttak, p -> akseptertFulltSamtidigUttak(p, farRundtFødsel), EndringsdatoBerørtUtleder::helgFomMandagSegment);
        var tidslinjeAnnenpartSamtidig = lagTidslinje(annenpartsUttak, p -> akseptertFulltSamtidigUttak(p, farRundtFødsel), EndringsdatoBerørtUtleder::helgFomMandagSegment);
        var tidslinjeSamtidigUttak = slåSammenTidslinjer(tidslinjeBrukerSamtidig, tidslinjeAnnenpartSamtidig);

        var overlappUtenomAkseptertFulltSamtidigUttak = tidslinjeOverlappendeUttakFomEndringsdato.disjoint(tidslinjeSamtidigUttak);
        return overlappUtenomAkseptertFulltSamtidigUttak.isEmpty() ? Optional.empty() : Optional.of(overlappUtenomAkseptertFulltSamtidigUttak.getMinLocalDate());
    }

    private static boolean akseptertFulltSamtidigUttak(ForeldrepengerUttakPeriode periode, LocalDateInterval farRundtFødsel) {
        var periodeRundtFødsel = Optional.ofNullable(farRundtFødsel).filter(f -> f.contains(periode.getTidsperiode())).isPresent();
        return periode.isSamtidigUttak() && (periode.isFlerbarnsdager() || periodeRundtFødsel);
    }

    private static LocalDateTimeline<Boolean> tidslinjeForSammenhengendeUttaksplan(ForeldrepengerUttak brukersUttak, ForeldrepengerUttak annenpartsUttak) {
        var tidslinjeBruker = lagTidslinje(brukersUttak, p -> true, EndringsdatoBerørtUtleder::helgTomSøndagSegment);
        var tidslinjeAnnenpart = lagTidslinje(annenpartsUttak, p -> true, EndringsdatoBerørtUtleder::helgTomSøndagSegment);
        return slåSammenTidslinjer(tidslinjeBruker, tidslinjeAnnenpart);
    }

    private static LocalDateTimeline<Boolean> slåSammenTidslinjer(LocalDateTimeline<Boolean> tidslinje1, LocalDateTimeline<Boolean> tidslinje2) {
        // Slår sammen uttaksplanene med TRUE der en eller begge har uttak.
        return tidslinje1.crossJoin(tidslinje2, StandardCombinators::alwaysTrueForMatch).compress();
    }

    private static LocalDateTimeline<Boolean> lagTidslinje(ForeldrepengerUttak uttak, Predicate<ForeldrepengerUttakPeriode> periodefilter,
                                                    Function<ForeldrepengerUttakPeriode, LocalDateSegment<Boolean>> segmentMapper) {
        var segmenter = uttak.getGjeldendePerioder().stream()
            .filter(EndringsdatoBerørtUtleder::isAktivtUttak)
            .filter(periodefilter)
            .map(segmentMapper)
            .toList();
        return new LocalDateTimeline<>(segmenter, StandardCombinators::alwaysTrueForMatch).compress();
    }

    private static boolean isAktivtUttak(ForeldrepengerUttakPeriode p) {
        return p.harAktivtUttak() || p.isInnvilgetOpphold();
    }

    private static LocalDateSegment<Boolean> helgFomMandagSegment(ForeldrepengerUttakPeriode periode) {
        var fom = VirkedagUtil.lørdagSøndagTilMandag(periode.getFom());
        var tom = periode.getTom();
        var brukFom = fom.isAfter(tom) ? periode.getFom() : fom;
        return new LocalDateSegment<>(brukFom, tom, Boolean.TRUE);
    }

    private static LocalDateSegment<Boolean> helgTomSøndagSegment(ForeldrepengerUttakPeriode periode) {
        var fom = periode.getFom();
        var tom = VirkedagUtil.fredagLørdagTilSøndag(periode.getTom());
        return new LocalDateSegment<>(fom, tom, Boolean.TRUE);
    }

    private static Optional<LocalDate> finnMinAktivDato(ForeldrepengerUttak uttak) {
        return uttak.getGjeldendePerioder().stream()
            .filter(EndringsdatoBerørtUtleder::isAktivtUttak)
            .map(ForeldrepengerUttakPeriode::getFom)
            .min(Comparator.naturalOrder());
    }

    private static Optional<LocalDate> finnMinAktivDato(ForeldrepengerUttak bruker, ForeldrepengerUttak annenpart) {
        return Stream.concat(finnMinAktivDato(bruker).stream(), finnMinAktivDato(annenpart).stream())
            .min(Comparator.naturalOrder());
    }

    private static Optional<LocalDate> finnMaxAktivDato(ForeldrepengerUttak bruker, ForeldrepengerUttak annenpart) {
        return Stream.concat(bruker.getGjeldendePerioder().stream(), annenpart.getGjeldendePerioder().stream())
            .filter(EndringsdatoBerørtUtleder::isAktivtUttak)
            .map(ForeldrepengerUttakPeriode::getTom)
            .max(Comparator.naturalOrder());
    }

}

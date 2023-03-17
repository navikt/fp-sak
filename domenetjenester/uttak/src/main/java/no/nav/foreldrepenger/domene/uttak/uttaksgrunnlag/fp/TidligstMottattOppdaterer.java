package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.Årsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

public class TidligstMottattOppdaterer {

    private static final Logger LOG = LoggerFactory.getLogger(TidligstMottattOppdaterer.class);

    private TidligstMottattOppdaterer() {
    }

    public static List<OppgittPeriodeEntitet> oppdaterTidligstMottattDato(List<OppgittPeriodeEntitet> nysøknad,
                                                                          LocalDate mottattDato,
                                                                          List<OppgittFordelingEntitet> tidligereFordelinger,
                                                                          Optional<UttakResultatEntitet> forrigeUttak) {
        if (nysøknad.isEmpty()) {
            return nysøknad;
        }

        // Først sett mottatt dato
        nysøknad.stream().filter(p -> p.getMottattDato() == null).forEach(p -> p.setMottattDato(mottattDato));
        nysøknad.stream().filter(p -> p.getTidligstMottattDato().isEmpty()).forEach(p -> p.setTidligstMottattDato(mottattDato));

        var tidligstedato = nysøknad.stream().map(OppgittPeriodeEntitet::getFom).min(Comparator.naturalOrder()).orElseThrow();
        var tidslinjeSammenlignNysøknad =  lagSammenligningTimeline(nysøknad);
        var nysøknadTidslinje = lagSøknadsTimeline(nysøknad);

        for (var f : tidligereFordelinger) {
            var perioder = perioderForFordeling(f.getPerioder(), mottattDato, tidligstedato);
            if (!perioder.isEmpty()) {
                try {
                    nysøknadTidslinje = oppdaterTidligstMottattDato(nysøknadTidslinje, tidslinjeSammenlignNysøknad, perioder);
                } catch (Exception e) {
                    LOG.warn("TidligstMottatt: Feil ved sjekk av tidligere fordeling {} - se bort fra enkelttilfelle, varsle dersom mange", f.getId());
                }
            }
        }

        // Vedtaksperioder fra forrige uttaksresultat - bruker sammenhengende = true for å få med avslåtte
        var perioderForrigeUttak = forrigeUttak
            .map(uttak -> VedtaksperioderHelper.opprettOppgittePerioder(uttak, List.of(), tidligstedato, true))
            .map(l -> perioderForFordeling(l, mottattDato, tidligstedato))
            .orElse(List.of());
        if (!perioderForrigeUttak.isEmpty()) {
            nysøknadTidslinje = oppdaterTidligstMottattDato(nysøknadTidslinje, tidslinjeSammenlignNysøknad, perioderForrigeUttak);
        }

        return nysøknadTidslinje.toSegments().stream().map(LocalDateSegment::getValue).filter(Objects::nonNull).toList();
    }

    private static List<OppgittPeriodeEntitet> perioderForFordeling(List<OppgittPeriodeEntitet> fordeling, LocalDate mottattDato, LocalDate tidligstedato) {
        return fordeling.stream()
            .filter(op -> !op.getTom().isBefore(tidligstedato))
            .filter(p -> !p.isOpphold())
            .filter(p -> p.getTidligstMottattDato().orElseGet(p::getMottattDato) != null)
            .filter(p -> p.getTidligstMottattDato().orElseGet(p::getMottattDato).isBefore(mottattDato))
            .toList();
    }

    private static LocalDateTimeline<OppgittPeriodeEntitet> oppdaterTidligstMottattDato(LocalDateTimeline<OppgittPeriodeEntitet> tidslinje,
                                                                                        LocalDateTimeline<SammenligningPeriodeForMottatt> sammenlignTidslinje,
                                                                                        List<OppgittPeriodeEntitet> perioder) {

        if (perioder.isEmpty()) {
            return tidslinje;
        }

        // Bygg tidslinjer for uttaksperioder
        var tidslinjeSammenlignForrigeSøknad =  lagSammenligningTimeline(perioder);

        // Finn sammenfallende perioder - søkt likt innen samme peride
        var tidslinjeSammenfallForrigeSøknad = sammenlignTidslinje.combine(tidslinjeSammenlignForrigeSøknad, TidligstMottattOppdaterer::leftIfEqualsRight, LocalDateTimeline.JoinStyle.INNER_JOIN);

        // Bygg tidslinjer over tidligst mottatt - men kun de som finnes for sammenfallende (like nok) perioder
        var tidslinjeTidligstMottattForrigeSøknad = new LocalDateTimeline<>(tidligstMottattFraOppgittePerioderJusterHelg(perioder), StandardCombinators::min)
            .intersection(tidslinjeSammenfallForrigeSøknad).filterValue(Objects::nonNull).compress();

        if (tidslinjeTidligstMottattForrigeSøknad.isEmpty()) {
            return tidslinje;
        }

        var oppdatertTidslinje = tidslinje.combine(tidslinjeTidligstMottattForrigeSøknad,
            TidligstMottattOppdaterer::oppdaterMedTidligstMottatt, LocalDateTimeline.JoinStyle.LEFT_JOIN);
        return new LocalDateTimeline<>(oppdatertTidslinje.toSegments(), TidligstMottattOppdaterer::oppgittPeriodeSplitter);
    }

    private static LocalDateTimeline<OppgittPeriodeEntitet> lagSøknadsTimeline(List<OppgittPeriodeEntitet> søknad) {
        var nysøknadSegmenter = søknad.stream().map(p -> new LocalDateSegment<>(new LocalDateInterval(p.getFom(), p.getTom()), p)).toList();
        return new LocalDateTimeline<>(nysøknadSegmenter, TidligstMottattOppdaterer::oppgittPeriodeSplitter);
    }

    private static LocalDateTimeline<SammenligningPeriodeForMottatt> lagSammenligningTimeline(List<OppgittPeriodeEntitet> søknad) {
        return new LocalDateTimeline<>(fraOppgittePerioder(søknad));
    }

    private static LocalDateSegment<OppgittPeriodeEntitet> oppgittPeriodeSplitter(LocalDateInterval di, LocalDateSegment<OppgittPeriodeEntitet> seg) {
        return di.equals(seg.getLocalDateInterval()) ? seg : new LocalDateSegment<>(di,
            OppgittPeriodeBuilder.fraEksisterende(seg.getValue()).medPeriode(di.getFomDato(), di.getTomDato()).build());
    }

    // Combinator som oppdaterer tidligst mottatt dato
    private static LocalDateSegment<OppgittPeriodeEntitet> oppdaterMedTidligstMottatt(LocalDateInterval di, LocalDateSegment<OppgittPeriodeEntitet> periode, LocalDateSegment<LocalDate> dato) {
        periode.getValue().setTidligstMottattDato(dato != null && dato.getValue() != null ? dato.getValue() :
            periode.getValue().getTidligstMottattDato().orElseGet(() -> periode.getValue().getMottattDato()));
        return periode;
    }

    private static LocalDateSegment<SammenligningPeriodeForMottatt> leftIfEqualsRight(LocalDateInterval dateInterval,
                                                                                      LocalDateSegment<SammenligningPeriodeForMottatt> lhs,
                                                                                      LocalDateSegment<SammenligningPeriodeForMottatt> rhs) {
        return lhs != null && rhs != null && Objects.equals(lhs.getValue(), rhs.getValue()) ?
            new LocalDateSegment<>(dateInterval, lhs.getValue()) : null;
    }

    private static List<LocalDateSegment<SammenligningPeriodeForMottatt>> fraOppgittePerioder(List<OppgittPeriodeEntitet> perioder) {
        return perioder.stream().map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), new SammenligningPeriodeForMottatt(p))).toList();
    }

    private static List<LocalDateSegment<LocalDate>> tidligstMottattFraOppgittePerioderJusterHelg(List<OppgittPeriodeEntitet> perioder) {
        return perioder.stream()
            .filter(p -> p.getTidligstMottattDato().orElseGet(p::getMottattDato) != null)
            .map(p -> new LocalDateSegment<>(p.getFom(), VirkedagUtil.fredagLørdagTilSøndag(p.getTom()), p.getTidligstMottattDato().orElseGet(p::getMottattDato)))
            .toList();
    }

    private record SammenligningPeriodeForMottatt(Årsak årsak, UttakPeriodeType periodeType, SamtidigUttaksprosent samtidigUttaksprosent, SammenligningGraderingForMottatt gradering) {
        SammenligningPeriodeForMottatt(OppgittPeriodeEntitet periode) {
            this(periode.getÅrsak(), periode.isUtsettelse() ? UttakPeriodeType.UDEFINERT : periode.getPeriodeType(), periode.getSamtidigUttaksprosent(), periode.isGradert() ? new SammenligningGraderingForMottatt(periode) : null);
        }

    }

    private record SammenligningGraderingForMottatt(GraderingAktivitetType graderingAktivitetType, Stillingsprosent arbeidsprosent, Arbeidsgiver arbeidsgiver) {
        SammenligningGraderingForMottatt(OppgittPeriodeEntitet periode) {
            this(periode.getGraderingAktivitetType(), periode.getArbeidsprosentSomStillingsprosent(), periode.getArbeidsgiver());
        }
    }

}

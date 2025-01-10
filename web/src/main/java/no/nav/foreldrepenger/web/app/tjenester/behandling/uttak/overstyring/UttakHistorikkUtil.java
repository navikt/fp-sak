package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.overstyring;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.LINJESKIFT;
import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.format;
import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;
import static no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.overstyring.EndreUttakUtil.finnGjeldendePeriodeFor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.foreldrepenger.domene.uttak.KodeMapper;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeAktivitetLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeLagreDto;
import no.nav.fpsak.tidsserie.LocalDateInterval;

public final class UttakHistorikkUtil {

    private static final KodeMapper<OppholdÅrsak, StønadskontoType> OPPHOLD_ÅRSAK_STØNADSKONTO_TYPE_KODE_MAPPER = initOppholdÅrsakMapper();

    private final boolean erOverstyring;

    public UttakHistorikkUtil(boolean erOverstyring) {
        this.erOverstyring = erOverstyring;
    }

    public static UttakHistorikkUtil forFastsetting() {
        return new UttakHistorikkUtil(false);
    }

    public Optional<Historikkinnslag> lagHistorikkinnslag(BehandlingReferanse behandling,
                                                          List<UttakResultatPeriodeLagreDto> uttakResultat,
                                                          List<ForeldrepengerUttakPeriode> gjeldende) {
        var endringer = uttakResultat.stream()
            .map(periode -> lagHistorikkinnslagForPeriode(periode, gjeldende))
            .flatMap(Collection::stream)
            .toList();

        if (endringer.isEmpty()) {
            return Optional.empty();
        }

        var historikkinnslag = new Historikkinnslag.Builder().medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medBehandlingId(behandling.behandlingId())
            .medFagsakId(behandling.fagsakId())
            .medTittel(SkjermlenkeType.UTTAK)
            .addLinje(new HistorikkinnslagLinjeBuilder().bold(erOverstyring ? "Overstyring av uttak" : "Manuell vurdering av uttak"))
            .addLinje(LINJESKIFT);
        endringer.forEach(historikkinnslag::addLinje);
        return Optional.of(historikkinnslag.build());
    }

    private List<HistorikkinnslagLinjeBuilder> lagHistorikkinnslagForPeriode(UttakResultatPeriodeLagreDto periode, List<ForeldrepengerUttakPeriode> gjeldende) {
        var gjeldendePeriode = finnGjeldendePeriodeFor(gjeldende, new LocalDateInterval(periode.getFom(), periode.getTom()));
        var endrignerOpphold = lagHistorikkinnslagTekstForOppholdsperiode(gjeldendePeriode, periode);
        var endringerPeriode = endringerPåPerioden(gjeldendePeriode, periode);
        var harFlereAktiviteter = periode.getAktiviteter().size() > 1;
        var endringerPerAktivitet =  periode.getAktiviteter().stream()
            .map(nyAktivitet -> endringerPåAktivitet(nyAktivitet, gjeldendePeriode, harFlereAktiviteter))
            .flatMap(Collection::stream)
            .toList();

        var alleEndringer = Stream.concat(endrignerOpphold.stream(), Stream.concat(endringerPeriode.stream(), endringerPerAktivitet.stream()))
            .filter(Objects::nonNull)
            .toList();
        if (alleEndringer.isEmpty()) {
            return List.of();
        }
        var endringer = new ArrayList<HistorikkinnslagLinjeBuilder>();
        endringer.add(periodeErManueltVurdertLinje(periode));
        endringer.addAll(alleEndringer);
        endringer.add(begrunnelseLinje(periode));
        endringer.add(LINJESKIFT);
        return endringer;
    }

    private List<HistorikkinnslagLinjeBuilder> endringerPåAktivitet(UttakResultatPeriodeAktivitetLagreDto nyAktivitet, ForeldrepengerUttakPeriode gjeldendePeriode, boolean harFlereAktiviteter) {
        List<HistorikkinnslagLinjeBuilder> endringerAktivtet = new ArrayList<>();
        var gjeldendeAktivitet = EndreUttakUtil.finnGjeldendeAktivitetFor(gjeldendePeriode, nyAktivitet.getArbeidsgiver().orElse(null), nyAktivitet.getArbeidsforholdId(), nyAktivitet.getUttakArbeidType());
        if (harFlereAktiviteter) {
            var navnAktivitet = nyAktivitet.getArbeidsgiver().map(Arbeidsgiver::getIdentifikator).orElseGet(() -> nyAktivitet.getUttakArbeidType().getNavn());
            endringerAktivtet.add(fraTilEquals("Stønadskontotype for " + navnAktivitet, gjeldendeAktivitet.getTrekkonto(), nyAktivitet.getStønadskontoType()));
            endringerAktivtet.add(trekkdagerLinje("Trekkdager for " + navnAktivitet, gjeldendeAktivitet, nyAktivitet));
            endringerAktivtet.add(utbetalingsgradLinje("Utbetalingsgrad for " + navnAktivitet, gjeldendeAktivitet, nyAktivitet));
        } else {
            endringerAktivtet.add(fraTilEquals("Stønadskontotype", gjeldendeAktivitet.getTrekkonto(), nyAktivitet.getStønadskontoType()));
            endringerAktivtet.add(trekkdagerLinje("Trekkdager", gjeldendeAktivitet, nyAktivitet));
            endringerAktivtet.add(utbetalingsgradLinje("Utbetalingsgrad", gjeldendeAktivitet, nyAktivitet));
        }
        return endringerAktivtet;
    }


    private List<HistorikkinnslagLinjeBuilder> endringerPåPerioden(ForeldrepengerUttakPeriode gjeldendePeriode, UttakResultatPeriodeLagreDto nyPeriode) {
        var linjer = new ArrayList<HistorikkinnslagLinjeBuilder>();
        linjer.add(fraTilEquals("Resultatet", gjeldendePeriode.getResultatType(), nyPeriode.getPeriodeResultatType()));
        linjer.add(fraTilEquals("Årsak", gjeldendePeriode.getResultatÅrsak(), nyPeriode.getPeriodeResultatÅrsak()));
        linjer.add(fraTilEquals("Flerbarnsdager", gjeldendePeriode.isFlerbarnsdager(), nyPeriode.isFlerbarnsdager()));
        linjer.add(fraTilEquals("Samtidig uttak", gjeldendePeriode.isSamtidigUttak(), nyPeriode.isSamtidigUttak()));
        linjer.add(samtidigUttaksprosentLinje(nyPeriode, gjeldendePeriode));
        linjer.add(graderingLinje(nyPeriode, gjeldendePeriode));
        linjer.add(fraTilEquals("Årsak avslag gradering", gjeldendePeriode.getGraderingAvslagÅrsak(), nyPeriode.getGraderingAvslagÅrsak()));
        return linjer;
    }

    private static HistorikkinnslagLinjeBuilder begrunnelseLinje(UttakResultatPeriodeLagreDto nyPeriode) {
        var endretBegrunnelse = nyPeriode.getBegrunnelse() != null && !nyPeriode.getBegrunnelse().equals(" ");
        return endretBegrunnelse ? new HistorikkinnslagLinjeBuilder().tekst(nyPeriode.getBegrunnelse()) : null;
    }

    private static HistorikkinnslagLinjeBuilder graderingLinje(UttakResultatPeriodeLagreDto nyPeriode, ForeldrepengerUttakPeriode gjeldendePeriode) {
        return fraTilEquals("Gradering av arbeidsforhold", gjeldendePeriode.isGraderingInnvilget() ? "Oppfylt" : "Ikke oppfylt",
            nyPeriode.isGraderingInnvilget() ? "Oppfylt" : "Ikke oppfylt");
    }

    private static HistorikkinnslagLinjeBuilder samtidigUttaksprosentLinje(UttakResultatPeriodeLagreDto nyPeriode,
                                                                           ForeldrepengerUttakPeriode gjeldendePeriode) {
        if (!nyPeriode.isSamtidigUttak()) {
            return null;
        }
        var fraTekst =
            gjeldendePeriode.getSamtidigUttaksprosent() == null ? null : gjeldendePeriode.getSamtidigUttaksprosent().decimalValue().toString() + "%";
        var tilTekst = nyPeriode.getSamtidigUttaksprosent() == null ? null : nyPeriode.getSamtidigUttaksprosent().decimalValue().toString() + "%";
        return fraTilEquals("Samtidig uttak", fraTekst, tilTekst);
    }

    private HistorikkinnslagLinjeBuilder utbetalingsgradLinje(String navn,
                                                              ForeldrepengerUttakPeriodeAktivitet gjeldendeAktivitet,
                                                              UttakResultatPeriodeAktivitetLagreDto nyAktivitet) {
        var til = gjeldendeAktivitet.getUtbetalingsgrad();
        return fraTilEquals(navn, til == null ? null : til.decimalValue().toString() + "%",
            nyAktivitet.getUtbetalingsgrad().decimalValue().toString() + "%");
    }

    private static HistorikkinnslagLinjeBuilder trekkdagerLinje(String navn,
                                                                ForeldrepengerUttakPeriodeAktivitet gjeldendeAktivitet,
                                                                UttakResultatPeriodeAktivitetLagreDto nyAktivitet) {
        var gjeldendeAktivitetTrekkdager = gjeldendeAktivitet.getTrekkdager() == null ? null : gjeldendeAktivitet.getTrekkdager().toString();
        var nyAktivitetTrekkdager = nyAktivitet.getTrekkdagerDesimaler() == null ? null : nyAktivitet.getTrekkdagerDesimaler().toString();
        return fraTilEquals(navn, gjeldendeAktivitetTrekkdager, nyAktivitetTrekkdager);
    }

    private HistorikkinnslagLinjeBuilder periodeErManueltVurdertLinje(UttakResultatPeriodeLagreDto nyPeriode) {
        return new HistorikkinnslagLinjeBuilder().tekst("Vurdering av perioden")
            .tekst(format(new LocalDateInterval(nyPeriode.getFom(), nyPeriode.getTom())));
    }

    private List<HistorikkinnslagLinjeBuilder> lagHistorikkinnslagTekstForOppholdsperiode(ForeldrepengerUttakPeriode gjeldendePeriode, UttakResultatPeriodeLagreDto nyPeriode) {
        var list = new ArrayList<HistorikkinnslagLinjeBuilder>();
        if (!erOppholdsPeriode(nyPeriode)) {
            return list;
        }

        var stønadskontoTypeOpt = OPPHOLD_ÅRSAK_STØNADSKONTO_TYPE_KODE_MAPPER.map(gjeldendePeriode.getOppholdÅrsak());
        var nyStønadskontoTypeOpt = OPPHOLD_ÅRSAK_STØNADSKONTO_TYPE_KODE_MAPPER.map(nyPeriode.getOppholdÅrsak());
        if (!Objects.equals(stønadskontoTypeOpt, nyStønadskontoTypeOpt)) {
            list.add(new HistorikkinnslagLinjeBuilder().fraTil("Stønadskontotype", stønadskontoTypeOpt.orElse(null), nyStønadskontoTypeOpt.orElse(null)));
            list.add(new HistorikkinnslagLinjeBuilder().tekst(nyPeriode.getBegrunnelse()));
        }

        return list;
    }

    public static UttakHistorikkUtil forOverstyring() {
        return new UttakHistorikkUtil(true);
    }

    private boolean erOppholdsPeriode(UttakResultatPeriodeLagreDto periode) {
        return !OppholdÅrsak.UDEFINERT.equals(periode.getOppholdÅrsak());
    }

    private static KodeMapper<OppholdÅrsak, StønadskontoType> initOppholdÅrsakMapper() {
        return KodeMapper.medMapping(OppholdÅrsak.KVOTE_FELLESPERIODE_ANNEN_FORELDER, StønadskontoType.FELLESPERIODE)
            .medMapping(OppholdÅrsak.MØDREKVOTE_ANNEN_FORELDER, StønadskontoType.MØDREKVOTE)
            .medMapping(OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER, StønadskontoType.FEDREKVOTE)
            .medMapping(OppholdÅrsak.KVOTE_FORELDREPENGER_ANNEN_FORELDER, StønadskontoType.FORELDREPENGER)
            .build();
    }
}

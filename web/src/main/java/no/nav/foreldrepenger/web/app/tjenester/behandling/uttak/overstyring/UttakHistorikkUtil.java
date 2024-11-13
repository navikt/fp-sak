package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.overstyring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
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

    public List<HistorikkinnslagV2> lagHistorikkinnslag(BehandlingReferanse behandling,
                                                        List<UttakResultatPeriodeLagreDto> uttakResultat,
                                                        List<ForeldrepengerUttakPeriode> gjeldende) {
        return lagHistorikkinnslagFraPeriodeEndringer(behandling, uttakResultat, gjeldende);
    }

    private List<HistorikkinnslagV2> lagHistorikkinnslagFraPeriodeEndringer(BehandlingReferanse behandling,
                                                                            List<UttakResultatPeriodeLagreDto> perioder,
                                                                            List<ForeldrepengerUttakPeriode> gjeldende) {
        return perioder.stream().map(periode -> lagHistorikkinnslagForPeriode(behandling, periode, gjeldende)).flatMap(Collection::stream).toList();
    }

    private List<HistorikkinnslagV2> lagHistorikkinnslagForPeriode(BehandlingReferanse behandling,
                                                                   UttakResultatPeriodeLagreDto periode,
                                                                   List<ForeldrepengerUttakPeriode> gjeldende) {
        List<HistorikkinnslagV2> list = new ArrayList<>();
        if (erOppholdsPeriode(periode) && periodeHarEndringer(gjeldende, periode)) {
            list.add(lagHistorikkinnslagForOppholdsperiode(behandling, gjeldende, periode));
        }
        for (var aktivitet : periode.getAktiviteter()) {
            if (aktivitetHarEndringer(gjeldende, periode, aktivitet)) {
                list.add(lagHistorikkinnslag(behandling, gjeldende, periode, aktivitet));
            }
        }
        return list;
    }

    private HistorikkinnslagV2 lagHistorikkinnslag(BehandlingReferanse behandling,
                                                   List<ForeldrepengerUttakPeriode> gjeldende,
                                                   UttakResultatPeriodeLagreDto nyPeriode,
                                                   UttakResultatPeriodeAktivitetLagreDto nyAktivitet) {
        return new HistorikkinnslagV2.Builder().medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medBehandlingId(behandling.behandlingId())
            .medFagsakId(behandling.fagsakId())
            .medTittel(SkjermlenkeType.UTTAK)
            .medTekstlinjer(lagTekstlinjer(gjeldende, nyPeriode, nyAktivitet))
            .build();
    }

    private HistorikkinnslagV2 lagHistorikkinnslagForOppholdsperiode(BehandlingReferanse behandling,
                                                                     List<ForeldrepengerUttakPeriode> gjeldende,
                                                                     UttakResultatPeriodeLagreDto nyPeriode) {
        return new HistorikkinnslagV2.Builder().medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medBehandlingId(behandling.behandlingId())
            .medFagsakId(behandling.fagsakId())
            .medTittel(SkjermlenkeType.UTTAK)
            .medTekstlinjer(lagHistorikkinnslagTekstForOppholdsperiode(gjeldende, nyPeriode))
            .build();
    }

    private boolean aktivitetHarEndringer(List<ForeldrepengerUttakPeriode> gjeldende,
                                          UttakResultatPeriodeLagreDto nyPeriode,
                                          UttakResultatPeriodeAktivitetLagreDto nyAktivitet) {
        if (nyPeriode.getBegrunnelse() == null || nyPeriode.getBegrunnelse().isEmpty()) {
            return false;
        }
        return lagTekstlinjer(gjeldende, nyPeriode, nyAktivitet).size() > 1;
    }

    private boolean periodeHarEndringer(List<ForeldrepengerUttakPeriode> gjeldende, UttakResultatPeriodeLagreDto nyPeriode) {
        return lagHistorikkinnslagTekstForOppholdsperiode(gjeldende, nyPeriode).size() > 1;
    }

    private List<HistorikkinnslagV2.Tekstlinje> lagTekstlinjer(List<ForeldrepengerUttakPeriode> gjeldende,
                                                               UttakResultatPeriodeLagreDto nyPeriode,
                                                               UttakResultatPeriodeAktivitetLagreDto nyAktivitet) {
        var gjeldendePeriode = EndreUttakUtil.finnGjeldendePeriodeFor(gjeldende, new LocalDateInterval(nyPeriode.getFom(), nyPeriode.getTom()));
        var gjeldendeAktivitet = EndreUttakUtil.finnGjeldendeAktivitetFor(gjeldendePeriode, nyAktivitet.getArbeidsgiver().orElse(null),
            nyAktivitet.getArbeidsforholdId(), nyAktivitet.getUttakArbeidType());

        var list = new ArrayList<HistorikkinnslagV2.Tekstlinje>();

        list.add(periodeErManueltVurdertTekstlinje(nyPeriode));

        var gjeldendeAktivitetTrekkdager = gjeldendeAktivitet.getTrekkdager() == null ? null : gjeldendeAktivitet.getTrekkdager().toString();
        var nyAktivitetTrekkdager = nyAktivitet.getTrekkdagerDesimaler() == null ? null : nyAktivitet.getTrekkdagerDesimaler().toString();
        if (!Objects.equals(gjeldendeAktivitetTrekkdager, nyAktivitetTrekkdager)) {
            list.add(new HistorikkinnslagV2.Tekstlinje().fraTil("Trekkdager", gjeldendeAktivitetTrekkdager, nyAktivitetTrekkdager));
        }
        if (!Objects.equals(gjeldendeAktivitet.getTrekkonto(), nyAktivitet.getStønadskontoType())) {
            list.add(
                new HistorikkinnslagV2.Tekstlinje().fraTil("Stønadskontotype", gjeldendeAktivitet.getTrekkonto(), nyAktivitet.getStønadskontoType()));
        }
        if (!Objects.equals(gjeldendePeriode.getResultatType(), nyPeriode.getPeriodeResultatType())) {
            list.add(
                new HistorikkinnslagV2.Tekstlinje().fraTil("Resultatet", gjeldendePeriode.getResultatType(), nyPeriode.getPeriodeResultatType()));
        }
        if (!Objects.equals(gjeldendeAktivitet.getUtbetalingsgrad(), nyAktivitet.getUtbetalingsgrad())) {
            var til = gjeldendeAktivitet.getUtbetalingsgrad();
            list.add(new HistorikkinnslagV2.Tekstlinje().fraTil("Utbetalingsgrad", til == null ? null : til.decimalValue().toString() + "%",
                nyAktivitet.getUtbetalingsgrad().decimalValue().toString() + "%"));
        }
        if (!Objects.equals(gjeldendePeriode.getResultatÅrsak(), nyPeriode.getPeriodeResultatÅrsak())) {
            list.add(new HistorikkinnslagV2.Tekstlinje().fraTil("Årsak resultat", gjeldendePeriode.getResultatÅrsak(),
                nyPeriode.getPeriodeResultatÅrsak()));
        }
        if (!Objects.equals(gjeldendePeriode.isFlerbarnsdager(), nyPeriode.isFlerbarnsdager())) {
            list.add(new HistorikkinnslagV2.Tekstlinje().fraTil("Flerbarnsdager", gjeldendePeriode.isFlerbarnsdager(), nyPeriode.isFlerbarnsdager()));
        }
        if (!Objects.equals(gjeldendePeriode.isSamtidigUttak(), nyPeriode.isSamtidigUttak())) {
            list.add(new HistorikkinnslagV2.Tekstlinje().fraTil("Samtidig uttak", gjeldendePeriode.isSamtidigUttak(), nyPeriode.isSamtidigUttak()));
        }
        if (!Objects.equals(gjeldendePeriode.getSamtidigUttaksprosent(), nyPeriode.getSamtidigUttaksprosent())) {
            var fraTekst = gjeldendePeriode.getSamtidigUttaksprosent() == null ? null :
                gjeldendePeriode.getSamtidigUttaksprosent().decimalValue().toString() + "%";
            var tilTekst = nyPeriode.getSamtidigUttaksprosent() == null ? null : nyPeriode.getSamtidigUttaksprosent().decimalValue().toString() + "%";
            list.add(new HistorikkinnslagV2.Tekstlinje().fraTil("Samtidig uttak", fraTekst, tilTekst));
        }
        if (!Objects.equals(gjeldendePeriode.isGraderingInnvilget(), nyPeriode.isGraderingInnvilget())) {
            var fraTekst = gjeldendePeriode.isGraderingInnvilget() ? "Oppfylt" : "Ikke oppfylt";
            var tilTekst = nyPeriode.isGraderingInnvilget() ? "Oppfylt" : "Ikke oppfylt";
            list.add(new HistorikkinnslagV2.Tekstlinje().fraTil("Gradering av arbeidsforhold", fraTekst, tilTekst));
        }
        if (!Objects.equals(gjeldendePeriode.getGraderingAvslagÅrsak(), nyPeriode.getGraderingAvslagÅrsak())) {
            list.add(new HistorikkinnslagV2.Tekstlinje().fraTil("Årsak avslag gradering", gjeldendePeriode.getGraderingAvslagÅrsak(),
                nyPeriode.getGraderingAvslagÅrsak()));
        }
        if (!Objects.equals(gjeldendePeriode.getBegrunnelse(), nyPeriode.getBegrunnelse()) && nyPeriode.getBegrunnelse() != null
            && !nyPeriode.getBegrunnelse().equals(" ")) { //Frontend sender inn space på splitt
            list.add(new HistorikkinnslagV2.Tekstlinje().t(nyPeriode.getBegrunnelse()));
        }
        return list;
    }

    private HistorikkinnslagV2.Tekstlinje periodeErManueltVurdertTekstlinje(UttakResultatPeriodeLagreDto nyPeriode) {
        var introTekst = erOverstyring ? "Overstyrt vurdering" : "Manuell vurdering";
        return new HistorikkinnslagV2.Tekstlinje().b(introTekst).t("av perioden").t(nyPeriode.getFom()).t("-").t(nyPeriode.getTom()).p();
    }

    private List<HistorikkinnslagV2.Tekstlinje> lagHistorikkinnslagTekstForOppholdsperiode(List<ForeldrepengerUttakPeriode> gjeldende,
                                                                                           UttakResultatPeriodeLagreDto nyPeriode) {
        var gjeldendePeriode = EndreUttakUtil.finnGjeldendePeriodeFor(gjeldende, new LocalDateInterval(nyPeriode.getFom(), nyPeriode.getTom()));

        var list = new ArrayList<HistorikkinnslagV2.Tekstlinje>();
        list.add(periodeErManueltVurdertTekstlinje(nyPeriode));

        var stønadskontoTypeOpt = OPPHOLD_ÅRSAK_STØNADSKONTO_TYPE_KODE_MAPPER.map(gjeldendePeriode.getOppholdÅrsak());
        var nyStønadskontoTypeOpt = OPPHOLD_ÅRSAK_STØNADSKONTO_TYPE_KODE_MAPPER.map(nyPeriode.getOppholdÅrsak());
        if (!Objects.equals(stønadskontoTypeOpt, nyStønadskontoTypeOpt)) {
            list.add(
                new HistorikkinnslagV2.Tekstlinje().fraTil("Stønadskontotype", stønadskontoTypeOpt.orElse(null), nyStønadskontoTypeOpt.orElse(null)));
        }
        if (!Objects.equals(gjeldendePeriode.getBegrunnelse(), nyPeriode.getBegrunnelse()) && nyPeriode.getBegrunnelse() != null
            && !nyPeriode.getBegrunnelse().equals(" ")) {
            list.add(new HistorikkinnslagV2.Tekstlinje().t(nyPeriode.getBegrunnelse()));
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

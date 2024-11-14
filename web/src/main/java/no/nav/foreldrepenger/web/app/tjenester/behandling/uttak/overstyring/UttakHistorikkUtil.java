package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.overstyring;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder.fraTilEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
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

    public List<Historikkinnslag2> lagHistorikkinnslag(BehandlingReferanse behandling,
                                                       List<UttakResultatPeriodeLagreDto> uttakResultat,
                                                       List<ForeldrepengerUttakPeriode> gjeldende) {
        return lagHistorikkinnslagFraPeriodeEndringer(behandling, uttakResultat, gjeldende);
    }

    private List<Historikkinnslag2> lagHistorikkinnslagFraPeriodeEndringer(BehandlingReferanse behandling,
                                                                            List<UttakResultatPeriodeLagreDto> perioder,
                                                                            List<ForeldrepengerUttakPeriode> gjeldende) {
        return perioder.stream().map(periode -> lagHistorikkinnslagForPeriode(behandling, periode, gjeldende)).flatMap(Collection::stream).toList();
    }

    private List<Historikkinnslag2> lagHistorikkinnslagForPeriode(BehandlingReferanse behandling,
                                                                   UttakResultatPeriodeLagreDto periode,
                                                                   List<ForeldrepengerUttakPeriode> gjeldende) {
        List<Historikkinnslag2> list = new ArrayList<>();
        if (erOppholdsPeriode(periode)) {
            list.add(lagHistorikkinnslagForOppholdsperiode(behandling, gjeldende, periode));
        }
        for (var aktivitet : periode.getAktiviteter()) {
            list.add(lagHistorikkinnslag(behandling, gjeldende, periode, aktivitet));
        }
        return list.stream().filter(h -> h.getTekstlinjer().size() > 1).toList();
    }

    private Historikkinnslag2 lagHistorikkinnslag(BehandlingReferanse behandling,
                                                   List<ForeldrepengerUttakPeriode> gjeldende,
                                                   UttakResultatPeriodeLagreDto nyPeriode,
                                                   UttakResultatPeriodeAktivitetLagreDto nyAktivitet) {
        var gjeldendePeriode = EndreUttakUtil.finnGjeldendePeriodeFor(gjeldende, new LocalDateInterval(nyPeriode.getFom(), nyPeriode.getTom()));
        var gjeldendeAktivitet = EndreUttakUtil.finnGjeldendeAktivitetFor(gjeldendePeriode, nyAktivitet.getArbeidsgiver().orElse(null),
            nyAktivitet.getArbeidsforholdId(), nyAktivitet.getUttakArbeidType());

        var builder = new Historikkinnslag2.Builder()
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medBehandlingId(behandling.behandlingId())
            .medFagsakId(behandling.fagsakId())
            .medTittel(SkjermlenkeType.UTTAK)
            .addTekstlinje(periodeErManueltVurdertTekstlinje(nyPeriode))
            .addTekstlinje(fraTilEquals("Stønadskontotype", gjeldendeAktivitet.getTrekkonto(), nyAktivitet.getStønadskontoType()))
            .addTekstlinje(fraTilEquals("Resultatet", gjeldendePeriode.getResultatType(), nyPeriode.getPeriodeResultatType()))
            .addTekstlinje(trekkdagerTekstlinje(nyAktivitet, gjeldendeAktivitet))
            .addTekstlinje(utbetalingsgradTekstlinje(nyAktivitet, gjeldendeAktivitet))
            .addTekstlinje(fraTilEquals("Årsak resultat", gjeldendePeriode.getResultatÅrsak(), nyPeriode.getPeriodeResultatÅrsak()))
            .addTekstlinje(fraTilEquals("Flerbarnsdager", gjeldendePeriode.isFlerbarnsdager(), nyPeriode.isFlerbarnsdager()))
            .addTekstlinje(fraTilEquals("Samtidig uttak", gjeldendePeriode.isSamtidigUttak(), nyPeriode.isSamtidigUttak()))
            .addTekstlinje(samtidigUttaksprosentTekstlinje(nyPeriode, gjeldendePeriode))
            .addTekstlinje(graderingTekstlinje(nyPeriode, gjeldendePeriode))
            .addTekstlinje(fraTilEquals("Årsak avslag gradering", gjeldendePeriode.getGraderingAvslagÅrsak(), nyPeriode.getGraderingAvslagÅrsak()));

        if (builder.antallLagtTilLinjer() > 1 || !Objects.equals(gjeldendePeriode.getBegrunnelse(), nyPeriode.getBegrunnelse())) {
            builder.addTekstlinje(begrunnelseTekstlinje(nyPeriode));
        }

        return builder.build();
    }

    private static HistorikkinnslagTekstlinjeBuilder begrunnelseTekstlinje(UttakResultatPeriodeLagreDto nyPeriode) {
        var endretBegrunnelse =  nyPeriode.getBegrunnelse() != null && !nyPeriode.getBegrunnelse().equals(" ");
        return endretBegrunnelse ? new HistorikkinnslagTekstlinjeBuilder().t(nyPeriode.getBegrunnelse()) : null;
    }

    private static HistorikkinnslagTekstlinjeBuilder graderingTekstlinje(UttakResultatPeriodeLagreDto nyPeriode,
                                                                         ForeldrepengerUttakPeriode gjeldendePeriode) {
        return fraTilEquals("Gradering av arbeidsforhold",
            gjeldendePeriode.isGraderingInnvilget() ? "Oppfylt" : "Ikke oppfylt", nyPeriode.isGraderingInnvilget() ? "Oppfylt" : "Ikke oppfylt");
    }

    private static HistorikkinnslagTekstlinjeBuilder samtidigUttaksprosentTekstlinje(UttakResultatPeriodeLagreDto nyPeriode,
                                                                                     ForeldrepengerUttakPeriode gjeldendePeriode) {
        if (!nyPeriode.isSamtidigUttak()) {
            return null;
        }
        var fraTekst =
            gjeldendePeriode.getSamtidigUttaksprosent() == null ? null : gjeldendePeriode.getSamtidigUttaksprosent().decimalValue().toString() + "%";
        var tilTekst = nyPeriode.getSamtidigUttaksprosent() == null ? null : nyPeriode.getSamtidigUttaksprosent().decimalValue().toString() + "%";
        return fraTilEquals("Samtidig uttak", fraTekst, tilTekst);
    }

    private HistorikkinnslagTekstlinjeBuilder utbetalingsgradTekstlinje(UttakResultatPeriodeAktivitetLagreDto nyAktivitet,
                                                                        ForeldrepengerUttakPeriodeAktivitet gjeldendeAktivitet) {
        var til = gjeldendeAktivitet.getUtbetalingsgrad();
        return fraTilEquals("Utbetalingsgrad", til == null ? null : til.decimalValue().toString() + "%",
            nyAktivitet.getUtbetalingsgrad().decimalValue().toString() + "%");
    }

    private static HistorikkinnslagTekstlinjeBuilder trekkdagerTekstlinje(UttakResultatPeriodeAktivitetLagreDto nyAktivitet,
                                                                          ForeldrepengerUttakPeriodeAktivitet gjeldendeAktivitet) {
        var gjeldendeAktivitetTrekkdager = gjeldendeAktivitet.getTrekkdager() == null ? null : gjeldendeAktivitet.getTrekkdager().toString();
        var nyAktivitetTrekkdager = nyAktivitet.getTrekkdagerDesimaler() == null ? null : nyAktivitet.getTrekkdagerDesimaler().toString();
        return fraTilEquals("Trekkdager", gjeldendeAktivitetTrekkdager, nyAktivitetTrekkdager);
    }

    private Historikkinnslag2 lagHistorikkinnslagForOppholdsperiode(BehandlingReferanse behandling,
                                                                     List<ForeldrepengerUttakPeriode> gjeldende,
                                                                     UttakResultatPeriodeLagreDto nyPeriode) {
        return new Historikkinnslag2.Builder().medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medBehandlingId(behandling.behandlingId())
            .medFagsakId(behandling.fagsakId())
            .medTittel(SkjermlenkeType.UTTAK)
            .medTekstlinjer(lagHistorikkinnslagTekstForOppholdsperiode(gjeldende, nyPeriode))
            .build();
    }

    private HistorikkinnslagTekstlinjeBuilder periodeErManueltVurdertTekstlinje(UttakResultatPeriodeLagreDto nyPeriode) {
        var introTekst = erOverstyring ? "Overstyrt vurdering" : "Manuell vurdering";
        return new HistorikkinnslagTekstlinjeBuilder().b(introTekst).t("av perioden").t(nyPeriode.getFom()).t("-").t(nyPeriode.getTom()).p();
    }

    private List<HistorikkinnslagTekstlinjeBuilder> lagHistorikkinnslagTekstForOppholdsperiode(List<ForeldrepengerUttakPeriode> gjeldende,
                                                                                               UttakResultatPeriodeLagreDto nyPeriode) {
        var gjeldendePeriode = EndreUttakUtil.finnGjeldendePeriodeFor(gjeldende, new LocalDateInterval(nyPeriode.getFom(), nyPeriode.getTom()));

        var list = new ArrayList<HistorikkinnslagTekstlinjeBuilder>();
        list.add(periodeErManueltVurdertTekstlinje(nyPeriode));

        var stønadskontoTypeOpt = OPPHOLD_ÅRSAK_STØNADSKONTO_TYPE_KODE_MAPPER.map(gjeldendePeriode.getOppholdÅrsak());
        var nyStønadskontoTypeOpt = OPPHOLD_ÅRSAK_STØNADSKONTO_TYPE_KODE_MAPPER.map(nyPeriode.getOppholdÅrsak());
        if (!Objects.equals(stønadskontoTypeOpt, nyStønadskontoTypeOpt)) {
            list.add(new HistorikkinnslagTekstlinjeBuilder().fraTil("Stønadskontotype", stønadskontoTypeOpt.orElse(null), nyStønadskontoTypeOpt.orElse(null)));
            list.add(new HistorikkinnslagTekstlinjeBuilder().t(nyPeriode.getBegrunnelse()));
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

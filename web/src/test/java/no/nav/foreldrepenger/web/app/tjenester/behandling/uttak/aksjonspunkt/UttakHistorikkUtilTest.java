package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstBuilderFormater;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.ArbeidsgiverLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeAktivitetLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.overstyring.UttakHistorikkUtil;
import no.nav.fpsak.tidsserie.LocalDateInterval;

class UttakHistorikkUtilTest {

    private static final BehandlingReferanse BEHANDLING = mockBehandling();
    private static final LocalDate DEFAULT_FOM = LocalDate.now();
    private static final LocalDate DEFAULT_TOM = LocalDate.now().plusWeeks(1);
    public static final String ORGNR = KUNSTIG_ORG;
    public static final InternArbeidsforholdRef ARBEIDSFORHOLD_REF = InternArbeidsforholdRef.namedRef("TEST-REF");

    @Test
    void skalLageHistorikkInnslagForPeriodeResultatTypeHvisEndring() {
        var manuell = PeriodeResultatType.MANUELL_BEHANDLING;
        var gjeldende = enkeltPeriode(manuell);

        var perioder = nyMedResultatType(PeriodeResultatType.INNVILGET, new ArbeidsgiverLagreDto(ORGNR));

        var historikkinnslag = UttakHistorikkUtil.forOverstyring().lagHistorikkinnslag(BEHANDLING,
                perioder, List.of(gjeldende));

        assertThat(historikkinnslag).hasSize(1);
        var innslag = historikkinnslag.get(0);
        assertThat(innslag.getBehandlingId()).isEqualTo(BEHANDLING.behandlingId());
        assertThat(innslag.getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
        assertThat(innslag.getTekstlinjer()).hasSize(3);
        assertThat(innslag.getTekstlinjer().get(0).getTekst()).contains("__Overstyrt vurdering__ av perioden");
        assertThat(innslag.getTekstlinjer().get(1).getTekst()).isEqualTo("__Resultatet__ er endret fra Til manuell behandling til __Innvilget__");
        assertThat(innslag.getTekstlinjer().get(2).getTekst()).isEqualTo(perioder.get(0).getBegrunnelse());
    }

    //TODO TFP-5554 fix
    @Disabled
    @Test
    void skalIkkeLageHistorikkInnslagForPeriodeResultatTypeHvisIngenEndring() {
        var gjeldende = enkeltPeriode(PeriodeResultatType.INNVILGET);

        var perioder = nyMedResultatType(PeriodeResultatType.INNVILGET, new ArbeidsgiverLagreDto(ORGNR));

        var historikkinnslag = UttakHistorikkUtil.forOverstyring().lagHistorikkinnslag(BEHANDLING,
                perioder, List.of(gjeldende));

        assertThat(historikkinnslag).isEmpty();
    }

//    @Test
//    void skalLageHistorikkinnslagAvSplitting() {
//        var fom = LocalDate.now();
//        var tom = fom.plusWeeks(3);
//        var tomSplitPeriode1 = fom.plusWeeks(1);
//        var fomSplitPeriode2 = tomSplitPeriode1.plusDays(1);
//        var gjeldende = periodeMedFørOgEtter(PeriodeResultatType.INNVILGET,
//                fom, tom, fom.minusMonths(1), fom.minusDays(1), tom.plusDays(1), tom.plusWeeks(1));
//
//        var uendretFør = nyPeriodeMedType(PeriodeResultatType.INNVILGET, fom.minusMonths(1), fom.minusDays(1),
//                new ArbeidsgiverLagreDto(ORGNR));
//        var splittetFørste = nyPeriodeMedType(PeriodeResultatType.INNVILGET, fom, tomSplitPeriode1,
//                new ArbeidsgiverLagreDto(ORGNR));
//        var splittetAndre = nyPeriodeMedType(PeriodeResultatType.INNVILGET, fomSplitPeriode2, tom,
//                new ArbeidsgiverLagreDto(ORGNR));
//        var uendretEtter = nyPeriodeMedType(PeriodeResultatType.AVSLÅTT, tom.plusDays(1), tom.plusWeeks(1),
//                new ArbeidsgiverLagreDto(ORGNR));
//        var perioder = List.of(uendretFør, splittetFørste, splittetAndre, uendretEtter);
//
//        var historikkinnslag = UttakHistorikkUtil.forOverstyring().lagHistorikkinnslag(BEHANDLING,
//                perioder, gjeldende);
//
//        assertThat(historikkinnslag).hasSize(1);
//        assertThat(historikkinnslag.get(0).getBehandlingId()).isEqualTo(BEHANDLING.behandlingId());
//        assertThat(historikkinnslag.get(0).getType()).isEqualTo(HistorikkinnslagType.OVST_UTTAK_SPLITT);
//        assertThat(historikkinnslag.get(0).getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
//        assertThat(historikkinnslag.get(0).getHistorikkinnslagDeler()).hasSize(1);
//        var del = historikkinnslag.get(0).getHistorikkinnslagDeler().get(0);
//        assertThat(del.getEndredeFelt()).hasSize(2);
//        assertThat(del.getEndredeFelt().get(0).getFeltType()).isEqualTo(HistorikkinnslagFeltType.ENDRET_FELT);
//        assertThat(del.getEndredeFelt().get(0).getFraVerdi()).isEqualTo(asHistorikkVerdiString(fom, tom));
//        assertThat(del.getEndredeFelt().get(0).getTilVerdi()).isEqualTo(asHistorikkVerdiString(fom, tomSplitPeriode1));
//        assertThat(del.getEndredeFelt().get(1).getFeltType()).isEqualTo(HistorikkinnslagFeltType.ENDRET_FELT);
//        assertThat(del.getEndredeFelt().get(1).getFraVerdi()).isEqualTo(asHistorikkVerdiString(fom, tom));
//        assertThat(del.getEndredeFelt().get(1).getTilVerdi()).isEqualTo(asHistorikkVerdiString(fomSplitPeriode2, tom));
//    }
//
//    @Test
//    void skalLageHistorikkinnslagAvBådeSplittingEndringAvPeriode() {
//        var fom = LocalDate.now();
//        var tom = fom.plusWeeks(3);
//        var tomSplitPeriode1 = fom.plusWeeks(1);
//        var fomSplitPeriode2 = tomSplitPeriode1.plusDays(1);
//        var gjeldende = enkeltPeriode(PeriodeResultatType.INNVILGET, fom, tom, Arbeidsgiver.virksomhet(ORGNR));
//
//        var splittetFørste = nyPeriodeMedType(PeriodeResultatType.INNVILGET, fom, tomSplitPeriode1,
//                new ArbeidsgiverLagreDto(ORGNR));
//        var splittetAndre = nyPeriodeMedType(PeriodeResultatType.AVSLÅTT, fomSplitPeriode2, tom,
//                new ArbeidsgiverLagreDto(ORGNR));
//        var perioder = List.of(splittetFørste, splittetAndre);
//
//        var historikkinnslag = UttakHistorikkUtil.forOverstyring().lagHistorikkinnslag(BEHANDLING,
//                perioder, List.of(gjeldende));
//
//        assertThat(historikkinnslag).hasSize(2);
//        assertThat(historikkinnslag.get(0).getType()).isEqualTo(HistorikkinnslagType.OVST_UTTAK_SPLITT);
//        assertThat(historikkinnslag.get(1).getType()).isEqualTo(HistorikkinnslagType.OVST_UTTAK);
//    }
//
//    @Test
//    void skalLageHistorikkinnslagAvBådeSplittingEndringAvPeriodeVedFastsetting() {
//        var fom = LocalDate.now();
//        var tom = fom.plusWeeks(3);
//        var tomSplitPeriode1 = fom.plusWeeks(1);
//        var fomSplitPeriode2 = tomSplitPeriode1.plusDays(1);
//        var gjeldende = enkeltPeriode(PeriodeResultatType.INNVILGET, fom, tom, Arbeidsgiver.virksomhet(ORGNR));
//
//        var splittetFørste = nyPeriodeMedType(PeriodeResultatType.INNVILGET, fom, tomSplitPeriode1,
//                new ArbeidsgiverLagreDto(ORGNR));
//        var splittetAndre = nyPeriodeMedType(PeriodeResultatType.AVSLÅTT, fomSplitPeriode2, tom,
//                new ArbeidsgiverLagreDto(ORGNR));
//        var perioder = List.of(splittetFørste, splittetAndre);
//
//        var historikkinnslag = UttakHistorikkUtil.forFastsetting().lagHistorikkinnslag(BEHANDLING,
//                perioder, List.of(gjeldende));
//
//        assertThat(historikkinnslag).hasSize(2);
//        assertThat(historikkinnslag.get(0).getType()).isEqualTo(HistorikkinnslagType.FASTSATT_UTTAK_SPLITT);
//        assertThat(historikkinnslag.get(1).getType()).isEqualTo(HistorikkinnslagType.FASTSATT_UTTAK);
//    }
//
//    @Test
//    void skalLageHistorikkInnslagForPerioderMedPrivatpersonSomArbeidsgiver() {
//        var manuell = PeriodeResultatType.MANUELL_BEHANDLING;
//        var arbeidsgiverAktørId = AktørId.dummy();
//        var privateperson = Arbeidsgiver.person(arbeidsgiverAktørId);
//        var gjeldende = enkeltPeriode(manuell, privateperson);
//
//        var perioder = nyMedResultatType(PeriodeResultatType.INNVILGET, new ArbeidsgiverLagreDto(arbeidsgiverAktørId));
//
//        var historikkinnslag = UttakHistorikkUtil.forOverstyring().lagHistorikkinnslag(BEHANDLING,
//                perioder, List.of(gjeldende));
//
//        assertThat(historikkinnslag).hasSize(1);
//        assertThat(historikkinnslag.get(0).getBehandlingId()).isEqualTo(BEHANDLING.behandlingId());
//        assertThat(historikkinnslag.get(0).getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
//        assertThat(historikkinnslag.get(0).getHistorikkinnslagDeler()).hasSize(1);
//        var endretFelt = historikkinnslag.get(0).getHistorikkinnslagDeler().get(0)
//                .getEndretFelt(HistorikkEndretFeltType.UTTAK_PERIODE_RESULTAT_TYPE);
//        assertThat(endretFelt).isNotEmpty();
//        assertThat(endretFelt.get().getFraVerdi()).isEqualTo(gjeldende.getResultatType().getKode());
//        assertThat(endretFelt.get().getTilVerdi()).isEqualTo(perioder.get(0).getPeriodeResultatType().getKode());
//    }

    private List<ForeldrepengerUttakPeriode> periodeMedFørOgEtter(PeriodeResultatType type,
            LocalDate fom,
            LocalDate tom,
            LocalDate førFom,
            LocalDate førTom,
            LocalDate etterFom,
            LocalDate etterTom) {
        var aktiviteter = List.of(periodeAktivitet());
        var periode = periode(type, fom, tom, aktiviteter);
        var førPeriode = periode(PeriodeResultatType.INNVILGET, førFom, førTom, aktiviteter);
        var etterPeriode = periode(PeriodeResultatType.AVSLÅTT, etterFom, etterTom, aktiviteter);
        return List.of(periode, førPeriode, etterPeriode);
    }

    private ForeldrepengerUttakPeriode periode(PeriodeResultatType type,
            LocalDate fom,
            LocalDate tom,
            List<ForeldrepengerUttakPeriodeAktivitet> aktiviteter) {
        return new ForeldrepengerUttakPeriode.Builder()
                .medTidsperiode(fom, tom)
                .medResultatType(type)
                .medAktiviteter(aktiviteter)
                .medResultatÅrsak(PeriodeResultatÅrsak.UKJENT)
                .build();
    }

    private ForeldrepengerUttakPeriodeAktivitet periodeAktivitet() {
        return new ForeldrepengerUttakPeriodeAktivitet.Builder()
                .medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.ORDINÆRT_ARBEID, Arbeidsgiver.virksomhet(ORGNR), ARBEIDSFORHOLD_REF))
                .medArbeidsprosent(BigDecimal.ZERO)
                .medUtbetalingsgrad(Utbetalingsgrad.TEN)
                .build();
    }

    private String asHistorikkVerdiString(LocalDate fom, LocalDate tom) {
        return HistorikkinnslagTekstBuilderFormater.formatString(new LocalDateInterval(fom, tom));
    }

    private ForeldrepengerUttakPeriode enkeltPeriode(PeriodeResultatType type, LocalDate fom, LocalDate tom, Arbeidsgiver arbeidsgiver) {
        var periodeAktivitet = new ForeldrepengerUttakPeriodeAktivitet.Builder()
                .medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.ORDINÆRT_ARBEID, arbeidsgiver, ARBEIDSFORHOLD_REF))
                .medArbeidsprosent(BigDecimal.ZERO)
                .medUtbetalingsgrad(Utbetalingsgrad.TEN)
                .build();
        return periode(type, fom, tom, List.of(periodeAktivitet));
    }

    private List<UttakResultatPeriodeLagreDto> nyMedResultatType(PeriodeResultatType type, ArbeidsgiverLagreDto arbeidsgiver) {
        var nyPeriode = nyPeriodeMedType(type, arbeidsgiver);
        return Collections.singletonList(nyPeriode);
    }

    private UttakResultatPeriodeLagreDto nyPeriodeMedType(PeriodeResultatType type, ArbeidsgiverLagreDto arbeidsgiver) {
        return nyPeriodeMedType(type, DEFAULT_FOM, DEFAULT_TOM, arbeidsgiver);
    }

    private UttakResultatPeriodeLagreDto nyPeriodeMedType(PeriodeResultatType resultatType, LocalDate fom, LocalDate tom,
            ArbeidsgiverLagreDto arbeidsgiver) {
        List<UttakResultatPeriodeAktivitetLagreDto> aktiviteter = new ArrayList<>();
        var aktivitetLagreDto = new UttakResultatPeriodeAktivitetLagreDto.Builder()
                .medArbeidsgiver(arbeidsgiver)
                .medArbeidsforholdId(ARBEIDSFORHOLD_REF)
                .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
                .medTrekkdager(BigDecimal.ZERO)
                .medUtbetalingsgrad(Utbetalingsgrad.TEN)
                .build();
        aktiviteter.add(aktivitetLagreDto);
        return new UttakResultatPeriodeLagreDto.Builder()
                .medTidsperiode(fom, tom)
                .medAktiviteter(aktiviteter)
                .medPeriodeResultatType(resultatType)
                .medPeriodeResultatÅrsak(PeriodeResultatÅrsak.UKJENT)
                .medFlerbarnsdager(false)
                .medSamtidigUttak(false)
                .medBegrunnelse("abc")
                .build();
    }

    private ForeldrepengerUttakPeriode enkeltPeriode(PeriodeResultatType type) {
        return enkeltPeriode(type, Arbeidsgiver.virksomhet(ORGNR));
    }

    private ForeldrepengerUttakPeriode enkeltPeriode(PeriodeResultatType type, Arbeidsgiver virksomhet) {
        return enkeltPeriode(type, DEFAULT_FOM, DEFAULT_TOM, virksomhet);
    }

    private static BehandlingReferanse mockBehandling() {
        var mock = mock(BehandlingReferanse.class);
        when(mock.behandlingId()).thenReturn(123L);
        return mock;
    }

}

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
import java.util.Optional;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFelt;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstBuilderFormater;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.ArbeidsgiverLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeAktivitetLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.overstyring.UttakHistorikkUtil;
import no.nav.fpsak.tidsserie.LocalDateInterval;

public class UttakHistorikkUtilTest {

    private static final Behandling BEHANDLING = mockBehandling();
    private static final LocalDate DEFAULT_FOM = LocalDate.now();
    private static final LocalDate DEFAULT_TOM = LocalDate.now().plusWeeks(1);
    public static final String ORGNR = KUNSTIG_ORG;
    public static final InternArbeidsforholdRef ARBEIDSFORHOLD_REF = InternArbeidsforholdRef.namedRef("TEST-REF");

    @Test
    public void skalLageHistorikkInnslagForPeriodeResultatTypeHvisEndring() {
        PeriodeResultatType ikkeFastsatt = PeriodeResultatType.IKKE_FASTSATT;
        var gjeldende = enkeltPeriode(ikkeFastsatt);

        List<UttakResultatPeriodeLagreDto> perioder = nyMedResultatType(PeriodeResultatType.INNVILGET, new ArbeidsgiverLagreDto(ORGNR));

        List<Historikkinnslag> historikkinnslag = UttakHistorikkUtil.forOverstyring().lagHistorikkinnslag(BEHANDLING,
            perioder, List.of(gjeldende));

        assertThat(historikkinnslag).hasSize(1);
        assertThat(historikkinnslag.get(0).getBehandlingId()).isEqualTo(BEHANDLING.getId());
        assertThat(historikkinnslag.get(0).getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
        assertThat(historikkinnslag.get(0).getHistorikkinnslagDeler()).hasSize(1);
        Optional<HistorikkinnslagFelt> endretFelt = historikkinnslag.get(0).getHistorikkinnslagDeler().get(0)
            .getEndretFelt(HistorikkEndretFeltType.UTTAK_PERIODE_RESULTAT_TYPE);
        assertThat(endretFelt).isNotEmpty();
        assertThat(endretFelt.get().getFraVerdi()).isEqualTo(gjeldende.getResultatType().getKode());
        assertThat(endretFelt.get().getTilVerdi()).isEqualTo(perioder.get(0).getPeriodeResultatType().getKode());
    }

    @Test
    public void skalIkkeLageHistorikkInnslagForPeriodeResultatTypeHvisIngenEndring() {
        var gjeldende = enkeltPeriode(PeriodeResultatType.INNVILGET);

        List<UttakResultatPeriodeLagreDto> perioder = nyMedResultatType(PeriodeResultatType.INNVILGET, new ArbeidsgiverLagreDto(ORGNR));

        List<Historikkinnslag> historikkinnslag = UttakHistorikkUtil.forOverstyring().lagHistorikkinnslag(BEHANDLING,
            perioder, List.of(gjeldende));

        assertThat(historikkinnslag).isEmpty();
    }

    @Test
    public void skalLageHistorikkinnslagAvSplitting() {
        LocalDate fom = LocalDate.now();
        LocalDate tom = fom.plusWeeks(3);
        LocalDate tomSplitPeriode1 = fom.plusWeeks(1);
        LocalDate fomSplitPeriode2 = tomSplitPeriode1.plusDays(1);
        var gjeldende = periodeMedFørOgEtter(PeriodeResultatType.INNVILGET,
            fom, tom, fom.minusMonths(1), fom.minusDays(1), tom.plusDays(1), tom.plusWeeks(1));

        UttakResultatPeriodeLagreDto uendretFør = nyPeriodeMedType(PeriodeResultatType.INNVILGET, fom.minusMonths(1), fom.minusDays(1), new ArbeidsgiverLagreDto(ORGNR));
        UttakResultatPeriodeLagreDto splittetFørste = nyPeriodeMedType(PeriodeResultatType.INNVILGET, fom, tomSplitPeriode1, new ArbeidsgiverLagreDto(ORGNR));
        UttakResultatPeriodeLagreDto splittetAndre = nyPeriodeMedType(PeriodeResultatType.INNVILGET, fomSplitPeriode2, tom, new ArbeidsgiverLagreDto(ORGNR));
        UttakResultatPeriodeLagreDto uendretEtter = nyPeriodeMedType(PeriodeResultatType.AVSLÅTT, tom.plusDays(1), tom.plusWeeks(1), new ArbeidsgiverLagreDto(ORGNR));
        List<UttakResultatPeriodeLagreDto> perioder = List.of(uendretFør, splittetFørste, splittetAndre, uendretEtter);

        List<Historikkinnslag> historikkinnslag = UttakHistorikkUtil.forOverstyring().lagHistorikkinnslag(BEHANDLING,
            perioder, gjeldende);

        assertThat(historikkinnslag).hasSize(1);
        assertThat(historikkinnslag.get(0).getBehandlingId()).isEqualTo(BEHANDLING.getId());
        assertThat(historikkinnslag.get(0).getType()).isEqualTo(HistorikkinnslagType.OVST_UTTAK_SPLITT);
        assertThat(historikkinnslag.get(0).getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
        assertThat(historikkinnslag.get(0).getHistorikkinnslagDeler()).hasSize(1);
        HistorikkinnslagDel del = historikkinnslag.get(0).getHistorikkinnslagDeler().get(0);
        assertThat(del.getEndredeFelt()).hasSize(2);
        assertThat(del.getEndredeFelt().get(0).getFeltType()).isEqualTo(HistorikkinnslagFeltType.ENDRET_FELT);
        assertThat(del.getEndredeFelt().get(0).getFraVerdi()).isEqualTo(asHistorikkVerdiString(fom, tom));
        assertThat(del.getEndredeFelt().get(0).getTilVerdi()).isEqualTo(asHistorikkVerdiString(fom, tomSplitPeriode1));
        assertThat(del.getEndredeFelt().get(1).getFeltType()).isEqualTo(HistorikkinnslagFeltType.ENDRET_FELT);
        assertThat(del.getEndredeFelt().get(1).getFraVerdi()).isEqualTo(asHistorikkVerdiString(fom, tom));
        assertThat(del.getEndredeFelt().get(1).getTilVerdi()).isEqualTo(asHistorikkVerdiString(fomSplitPeriode2, tom));
    }

    @Test
    public void skalLageHistorikkinnslagAvBådeSplittingEndringAvPeriode() {
        LocalDate fom = LocalDate.now();
        LocalDate tom = fom.plusWeeks(3);
        LocalDate tomSplitPeriode1 = fom.plusWeeks(1);
        LocalDate fomSplitPeriode2 = tomSplitPeriode1.plusDays(1);
        var gjeldende = enkeltPeriode(PeriodeResultatType.INNVILGET, fom, tom, Arbeidsgiver.virksomhet(ORGNR));

        UttakResultatPeriodeLagreDto splittetFørste = nyPeriodeMedType(PeriodeResultatType.INNVILGET, fom, tomSplitPeriode1, new ArbeidsgiverLagreDto(ORGNR));
        UttakResultatPeriodeLagreDto splittetAndre = nyPeriodeMedType(PeriodeResultatType.AVSLÅTT, fomSplitPeriode2, tom, new ArbeidsgiverLagreDto(ORGNR));
        List<UttakResultatPeriodeLagreDto> perioder = List.of(splittetFørste, splittetAndre);

        List<Historikkinnslag> historikkinnslag = UttakHistorikkUtil.forOverstyring().lagHistorikkinnslag(BEHANDLING,
            perioder, List.of(gjeldende));

        assertThat(historikkinnslag).hasSize(2);
        assertThat(historikkinnslag.get(0).getType()).isEqualTo(HistorikkinnslagType.OVST_UTTAK_SPLITT);
        assertThat(historikkinnslag.get(1).getType()).isEqualTo(HistorikkinnslagType.OVST_UTTAK);
    }

    @Test
    public void skalLageHistorikkinnslagAvBådeSplittingEndringAvPeriodeVedFastsetting() {
        LocalDate fom = LocalDate.now();
        LocalDate tom = fom.plusWeeks(3);
        LocalDate tomSplitPeriode1 = fom.plusWeeks(1);
        LocalDate fomSplitPeriode2 = tomSplitPeriode1.plusDays(1);
        var gjeldende = enkeltPeriode(PeriodeResultatType.INNVILGET, fom, tom, Arbeidsgiver.virksomhet(ORGNR));

        UttakResultatPeriodeLagreDto splittetFørste = nyPeriodeMedType(PeriodeResultatType.INNVILGET, fom, tomSplitPeriode1, new ArbeidsgiverLagreDto(ORGNR));
        UttakResultatPeriodeLagreDto splittetAndre = nyPeriodeMedType(PeriodeResultatType.AVSLÅTT, fomSplitPeriode2, tom, new ArbeidsgiverLagreDto(ORGNR));
        List<UttakResultatPeriodeLagreDto> perioder = List.of(splittetFørste, splittetAndre);

        List<Historikkinnslag> historikkinnslag = UttakHistorikkUtil.forFastsetting().lagHistorikkinnslag(BEHANDLING,
            perioder, List.of(gjeldende));

        assertThat(historikkinnslag).hasSize(2);
        assertThat(historikkinnslag.get(0).getType()).isEqualTo(HistorikkinnslagType.FASTSATT_UTTAK_SPLITT);
        assertThat(historikkinnslag.get(1).getType()).isEqualTo(HistorikkinnslagType.FASTSATT_UTTAK);
    }

    @Test
    public void skalLageHistorikkInnslagForPerioderMedPrivatpersonSomArbeidsgiver() {
        PeriodeResultatType ikkeFastsatt = PeriodeResultatType.IKKE_FASTSATT;
        AktørId arbeidsgiverAktørId = AktørId.dummy();
        Arbeidsgiver privateperson = Arbeidsgiver.person(arbeidsgiverAktørId);
        var gjeldende = enkeltPeriode(ikkeFastsatt, privateperson);

        List<UttakResultatPeriodeLagreDto> perioder = nyMedResultatType(PeriodeResultatType.INNVILGET, new ArbeidsgiverLagreDto(arbeidsgiverAktørId));

        List<Historikkinnslag> historikkinnslag = UttakHistorikkUtil.forOverstyring().lagHistorikkinnslag(BEHANDLING,
            perioder, List.of(gjeldende));

        assertThat(historikkinnslag).hasSize(1);
        assertThat(historikkinnslag.get(0).getBehandlingId()).isEqualTo(BEHANDLING.getId());
        assertThat(historikkinnslag.get(0).getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
        assertThat(historikkinnslag.get(0).getHistorikkinnslagDeler()).hasSize(1);
        Optional<HistorikkinnslagFelt> endretFelt = historikkinnslag.get(0).getHistorikkinnslagDeler().get(0)
            .getEndretFelt(HistorikkEndretFeltType.UTTAK_PERIODE_RESULTAT_TYPE);
        assertThat(endretFelt).isNotEmpty();
        assertThat(endretFelt.get().getFraVerdi()).isEqualTo(gjeldende.getResultatType().getKode());
        assertThat(endretFelt.get().getTilVerdi()).isEqualTo(perioder.get(0).getPeriodeResultatType().getKode());
    }

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
            .build();
    }

    private String asHistorikkVerdiString(LocalDate fom, LocalDate tom) {
        return HistorikkinnslagTekstBuilderFormater.formatString(new LocalDateInterval(fom, tom));
    }

    private ForeldrepengerUttakPeriode enkeltPeriode(PeriodeResultatType type, LocalDate fom, LocalDate tom, Arbeidsgiver arbeidsgiver) {
        var periodeAktivitet = new ForeldrepengerUttakPeriodeAktivitet.Builder()
           .medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.ORDINÆRT_ARBEID, arbeidsgiver, ARBEIDSFORHOLD_REF))
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();
        return periode(type, fom, tom, List.of(periodeAktivitet));
    }

    private List<UttakResultatPeriodeLagreDto> nyMedResultatType(PeriodeResultatType type, ArbeidsgiverLagreDto arbeidsgiver) {
        UttakResultatPeriodeLagreDto nyPeriode = nyPeriodeMedType(type, arbeidsgiver);
        return Collections.singletonList(nyPeriode);
    }

    private UttakResultatPeriodeLagreDto nyPeriodeMedType(PeriodeResultatType type, ArbeidsgiverLagreDto arbeidsgiver) {
        return nyPeriodeMedType(type, DEFAULT_FOM, DEFAULT_TOM, arbeidsgiver);
    }

    private UttakResultatPeriodeLagreDto nyPeriodeMedType(PeriodeResultatType resultatType, LocalDate fom, LocalDate tom, ArbeidsgiverLagreDto arbeidsgiver) {
        List<UttakResultatPeriodeAktivitetLagreDto> aktiviteter = new ArrayList<>();
        UttakResultatPeriodeAktivitetLagreDto aktivitetLagreDto = new UttakResultatPeriodeAktivitetLagreDto.Builder()
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdId(ARBEIDSFORHOLD_REF)
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medTrekkdager(BigDecimal.ZERO)
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

    private static Behandling mockBehandling() {
        Behandling mock = mock(Behandling.class);
        when(mock.getId()).thenReturn(123L);
        return mock;
    }

}

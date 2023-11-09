package no.nav.foreldrepenger.ytelse.beregning.regelmodell.fastsett;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatAndel;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatGrunnlag;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatRegler;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Beregningsgrunnlag;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPrArbeidsforhold;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPrStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.uttakresultat.UttakAktivitet;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.uttakresultat.UttakResultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.uttakresultat.UttakResultatPeriode;
import no.nav.fpsak.tidsserie.LocalDateInterval;

class RegelFastsettBeregningsresultatFPTest {
    private static final LocalDate TRE_UKER_FØR_FØDSEL_DT = LocalDate.now().minusWeeks(3);
    private static final LocalDate FØDSELSDATO = LocalDate.now();
    private static final LocalDate DAGEN_ETTER_FØDSEL = LocalDate.now().plusDays(1);
    private static final LocalDate TI_UKER_ETTER_FØDSEL_DT = LocalDate.now().plusWeeks(10);
    private static final LocalDate FØRTI_SEKS_UKER_ETTER_FØDSEL_DT = LocalDate.now().plusWeeks(46);
    private static final LocalDateInterval FELLESPERIODE_FØR_FØDSEL = new LocalDateInterval(TRE_UKER_FØR_FØDSEL_DT, FØDSELSDATO);
    private static final LocalDateInterval MØDREKVOTE_PERIODE = new LocalDateInterval(DAGEN_ETTER_FØDSEL, TI_UKER_ETTER_FØDSEL_DT);
    private static final LocalDateInterval FELLESPERIODE = new LocalDateInterval(TI_UKER_ETTER_FØDSEL_DT.plusDays(1),
            FØRTI_SEKS_UKER_ETTER_FØDSEL_DT);
    private static final LocalDateInterval BG_PERIODE_1 = new LocalDateInterval(TRE_UKER_FØR_FØDSEL_DT, FØDSELSDATO.plusWeeks(4));
    private static final LocalDateInterval BG_PERIODE_2 = new LocalDateInterval(DAGEN_ETTER_FØDSEL.plusWeeks(4), LocalDate.MAX);
    private static final Arbeidsforhold ANONYMT_ARBEIDSFORHOLD = null;
    private static final Arbeidsforhold ARBEIDSFORHOLD_2 = Arbeidsforhold.nyttArbeidsforholdHosVirksomhet("222", UUID.randomUUID().toString());
    private static final Arbeidsforhold ARBEIDSFORHOLD_1 = Arbeidsforhold.nyttArbeidsforholdHosVirksomhet("111", UUID.randomUUID().toString());
    private static final Arbeidsforhold ARBEIDSFORHOLD_3 = Arbeidsforhold.nyttArbeidsforholdHosVirksomhet("333",UUID.randomUUID().toString());

    @Test
    void skalLageAndelForBrukerOgArbeidsgiverForEnPeriode() {
        // Arrange
        var modell = opprettRegelmodellEnPeriode();

        // Act
        var resultat = BeregningsresultatRegler.fastsettBeregningsresultat(modell);

        // Assert
        var perioder = resultat.beregningsresultat().getBeregningsresultatPerioder();
        assertThat(perioder).hasSize(1);
        var periode = perioder.get(0);
        assertThat(periode.getFom()).isEqualTo(DAGEN_ETTER_FØDSEL);
        assertThat(periode.getTom()).isEqualTo(TI_UKER_ETTER_FØDSEL_DT);
        var andelList = periode.getBeregningsresultatAndelList();
        assertThat(andelList).hasSize(2);

        var brukerAndeler = andelList.stream().filter(BeregningsresultatAndel::erBrukerMottaker)
                .toList();
        var arbAndeler = andelList.stream().filter(a -> !a.erBrukerMottaker()).toList();
        assertThat(brukerAndeler).hasSize(1);
        assertThat(brukerAndeler.get(0).getArbeidsforhold().identifikator()).isEqualTo("111");
        assertThat(brukerAndeler.get(0).getDagsats()).isEqualTo(1000);

        assertThat(arbAndeler).hasSize(1);
        assertThat(arbAndeler.get(0).getArbeidsforhold().identifikator()).isEqualTo("111");
        assertThat(arbAndeler.get(0).getDagsats()).isEqualTo(1000);
    }

    @Test
    void skalPeriodisereFlereUttaksPerioder() {
        // Arrange
        var intervalList = List.of(
                FELLESPERIODE_FØR_FØDSEL,
                MØDREKVOTE_PERIODE);
        var modell = opprettRegelmodell(intervalList, AktivitetStatus.ATFL);

        // Act
        var resultat = BeregningsresultatRegler.fastsettBeregningsresultat(modell);

        // Assert
        var perioder = resultat.beregningsresultat().getBeregningsresultatPerioder();
        assertThat(perioder).hasSize(2);
        var periode0 = perioder.get(0);
        assertThat(periode0.getFom()).isEqualTo(TRE_UKER_FØR_FØDSEL_DT);
        assertThat(periode0.getTom()).isEqualTo(FØDSELSDATO);

        var periode1 = perioder.get(1);
        assertThat(periode1.getFom()).isEqualTo(DAGEN_ETTER_FØDSEL);
        assertThat(periode1.getTom()).isEqualTo(TI_UKER_ETTER_FØDSEL_DT);
    }

    @Test
    void skalLageAndelerForFlereArbeidsforhold() {
        // Arrange
        var arb1 = lagPrArbeidsforhold(2000.0, 0.0, ARBEIDSFORHOLD_1);
        var arb2 = lagPrArbeidsforhold(0.0, 1500.0, ARBEIDSFORHOLD_2);
        var arb3 = lagPrArbeidsforhold(1000.0, 500.0, ARBEIDSFORHOLD_3);

        var modell = opprettRegelmodellMedArbeidsforhold(arb1, arb2, arb3);

        // Act
        var resultat = BeregningsresultatRegler.fastsettBeregningsresultat(modell);

        // Assert
        var perioder = resultat.beregningsresultat().getBeregningsresultatPerioder();

        assertThat(perioder).hasSize(1);
        var periode = perioder.get(0);
        assertThat(periode.getFom()).isEqualTo(DAGEN_ETTER_FØDSEL);
        assertThat(periode.getTom()).isEqualTo(TI_UKER_ETTER_FØDSEL_DT);

        var andelList = periode.getBeregningsresultatAndelList();
        assertThat(andelList).hasSize(5);

        var brukerAndeler = andelList.stream().filter(BeregningsresultatAndel::erBrukerMottaker)
                .toList();
        var arbAndeler = andelList.stream().filter(a -> !a.erBrukerMottaker()).toList();
        assertThat(brukerAndeler).hasSize(3);

        assertThat(brukerAndeler.stream().filter(af -> af.getArbeidsforhold().identifikator().equals("111")).toList().get(0)
                .getDagsats()).isEqualTo(2000);
        assertThat(brukerAndeler.stream().filter(af -> af.getArbeidsforhold().identifikator().equals("222")).toList().get(0)
                .getDagsats()).isZero();
        assertThat(brukerAndeler.stream().filter(af -> af.getArbeidsforhold().identifikator().equals("333")).toList().get(0)
                .getDagsats()).isEqualTo(1000);

        assertThat(arbAndeler).hasSize(2);
        assertThat(arbAndeler.stream().filter(af -> af.getArbeidsforhold().identifikator().equals("222")).toList().get(0)
                .getDagsats()).isEqualTo(1500);
        assertThat(arbAndeler.stream().filter(af -> af.getArbeidsforhold().identifikator().equals("333")).toList().get(0)
                .getDagsats()).isEqualTo(500);
    }

    @Test
    void skalLageAndelerForAnonymtArbeidsforhold() {
        // Arrange
        var arb1 = lagPrArbeidsforhold(2000.0, 0.0, ANONYMT_ARBEIDSFORHOLD);
        var arb2 = lagPrArbeidsforhold(0.0, 1500.0, ARBEIDSFORHOLD_2);

        var modell = opprettRegelmodellMedArbeidsforhold(arb1, arb2);

        // Act
        var resultat = BeregningsresultatRegler.fastsettBeregningsresultat(modell);

        // Assert
        var perioder = resultat.beregningsresultat().getBeregningsresultatPerioder();

        assertThat(perioder).hasSize(1);
        var periode = perioder.get(0);
        assertThat(periode.getFom()).isEqualTo(DAGEN_ETTER_FØDSEL);
        assertThat(periode.getTom()).isEqualTo(TI_UKER_ETTER_FØDSEL_DT);

        var andelList = periode.getBeregningsresultatAndelList();
        assertThat(andelList).hasSize(3);

        var brukerAndeler = andelList.stream().filter(BeregningsresultatAndel::erBrukerMottaker)
                .toList();
        var arbAndeler = andelList.stream().filter(a -> !a.erBrukerMottaker()).toList();
        assertThat(brukerAndeler).hasSize(2);

        assertThat(brukerAndeler.stream().filter(af -> af.getArbeidsforhold() == null).toList().get(0).getDagsats())
                .isEqualTo(2000);
        assertThat(brukerAndeler.stream().filter(af -> "222".equals(af.getArbeidsgiverId())).toList().get(0).getDagsats())
                .isZero();

        assertThat(arbAndeler).hasSize(1);
        assertThat(arbAndeler.stream().filter(af -> "222".equals(af.getArbeidsforhold().identifikator())).toList().get(0)
                .getDagsats()).isEqualTo(1500);
    }

    @Test
    void skalPeriodisereFlereUttaksPerioderOgBeregningsgrunnlagPerioder() {
        // Arrange
        var modell = opprettRegelmodellMedFlereBGOgUttakPerioder();

        // Act
        var resultat = BeregningsresultatRegler.fastsettBeregningsresultat(modell);

        // Assert
        var perioder = resultat.beregningsresultat().getBeregningsresultatPerioder();

        assertThat(perioder).hasSize(4);

        var periode1 = perioder.get(0);
        assertThat(periode1.getFom()).isEqualTo(TRE_UKER_FØR_FØDSEL_DT);
        assertThat(periode1.getTom()).isEqualTo(FØDSELSDATO);
        assertThat(periode1.getBeregningsresultatAndelList()).hasSize(2);
        assertThat(periode1.getBeregningsresultatAndelList().stream().filter(BeregningsresultatAndel::erBrukerMottaker)
                .toList().get(0).getDagsats()).isEqualTo(1000);
        assertThat(periode1.getBeregningsresultatAndelList().stream().filter(a -> !a.erBrukerMottaker())
                .toList().get(0).getDagsats()).isEqualTo(800);

        var periode2 = perioder.get(1);
        assertThat(periode2.getFom()).isEqualTo(DAGEN_ETTER_FØDSEL);
        assertThat(periode2.getTom()).isEqualTo(FØDSELSDATO.plusWeeks(4));
        assertThat(periode2.getBeregningsresultatAndelList()).hasSize(2);
        assertThat(periode2.getBeregningsresultatAndelList().stream().filter(BeregningsresultatAndel::erBrukerMottaker)
                .toList().get(0).getDagsats()).isEqualTo(1000);
        assertThat(periode2.getBeregningsresultatAndelList().stream().filter(a -> !a.erBrukerMottaker())
                .toList().get(0).getDagsats()).isEqualTo(800);

        var periode3 = perioder.get(2);
        assertThat(periode3.getFom()).isEqualTo(DAGEN_ETTER_FØDSEL.plusWeeks(4));
        assertThat(periode3.getTom()).isEqualTo(MØDREKVOTE_PERIODE.getTomDato());
        assertThat(periode3.getBeregningsresultatAndelList()).hasSize(1);
        assertThat(periode3.getBeregningsresultatAndelList().get(0).getDagsats()).isEqualTo(2000);
        assertThat(periode3.getBeregningsresultatAndelList().get(0).erBrukerMottaker()).isTrue();

        var periode4 = perioder.get(3);
        assertThat(periode4.getFom()).isEqualTo(FELLESPERIODE.getFomDato());
        assertThat(periode4.getTom()).isEqualTo(FELLESPERIODE.getTomDato());
        assertThat(periode4.getBeregningsresultatAndelList()).hasSize(1);
        assertThat(periode4.getBeregningsresultatAndelList().get(0).getDagsats()).isEqualTo(2000);
        assertThat(periode3.getBeregningsresultatAndelList().get(0).erBrukerMottaker()).isTrue();
    }

    @Test
    void skalRundeAvAndelerRiktig() {
        // Arrange
        var arb1 = lagPrArbeidsforhold(2165.49, 0.00, ARBEIDSFORHOLD_1);
        var arb2 = lagPrArbeidsforhold(0.455, 1550.50, ARBEIDSFORHOLD_2);
        var arb3 = lagPrArbeidsforhold(1001.50, 500.49, ARBEIDSFORHOLD_3);

        var modell = opprettRegelmodellMedArbeidsforhold(arb1, arb2, arb3);

        // Act
        var resultat = BeregningsresultatRegler.fastsettBeregningsresultat(modell);

        // Assert
        var perioder = resultat.beregningsresultat().getBeregningsresultatPerioder();

        var andelList = perioder.get(0).getBeregningsresultatAndelList();
        assertThat(andelList).hasSize(5);

        var brukerAndeler = andelList.stream().filter(BeregningsresultatAndel::erBrukerMottaker)
                .toList();
        var arbAndeler = andelList.stream().filter(a -> !a.erBrukerMottaker()).toList();
        assertThat(brukerAndeler).hasSize(3);

        assertThat(brukerAndeler.stream().filter(af -> af.getArbeidsforhold().identifikator().equals("111")).toList().get(0)
                .getDagsats()).isEqualTo(2165);
        assertThat(brukerAndeler.stream().filter(af -> af.getArbeidsforhold().identifikator().equals("222")).toList().get(0)
                .getDagsats()).isZero();
        assertThat(brukerAndeler.stream().filter(af -> af.getArbeidsforhold().identifikator().equals("333")).toList().get(0)
                .getDagsats()).isEqualTo(1002);

        assertThat(arbAndeler).hasSize(2);
        assertThat(arbAndeler.stream().filter(af -> af.getArbeidsforhold().identifikator().equals("222")).toList().get(0)
                .getDagsats()).isEqualTo(1551);
        assertThat(arbAndeler.stream().filter(af -> af.getArbeidsforhold().identifikator().equals("333")).toList().get(0)
                .getDagsats()).isEqualTo(500);
    }

    private BeregningsresultatGrunnlag opprettRegelmodellEnPeriode() {
        var perioder = Collections.singletonList(MØDREKVOTE_PERIODE);
        return opprettRegelmodell(perioder, AktivitetStatus.ATFL);
    }

    private BeregningsresultatGrunnlag opprettRegelmodell(List<LocalDateInterval> perioder, AktivitetStatus aktivitetsStatus) {
        var beregningsgrunnlag = opprettBeregningsgrunnlag();
        var uttakResultat = opprettUttak(perioder, aktivitetsStatus, Collections.emptyList());
        return new BeregningsresultatGrunnlag(beregningsgrunnlag, uttakResultat);
    }

    private BeregningsresultatGrunnlag opprettRegelmodellMedFlereBGOgUttakPerioder() {
        var beregningsgrunnlag = opprettBeregningsgrunnlagForFlerePerioder();
        var uttakPerioder = List.of(
                FELLESPERIODE_FØR_FØDSEL,
                MØDREKVOTE_PERIODE,
                FELLESPERIODE);
        var arbeidsforholdList = List.of(ARBEIDSFORHOLD_1, ARBEIDSFORHOLD_2);
        var uttakResultat = opprettUttak(uttakPerioder, AktivitetStatus.ATFL, arbeidsforholdList);
        return new BeregningsresultatGrunnlag(beregningsgrunnlag, uttakResultat);
    }

    private BeregningsresultatGrunnlag opprettRegelmodellMedArbeidsforhold(BeregningsgrunnlagPrArbeidsforhold... arbeidsforhold) {
        var beregningsgrunnlag = opprettBeregningsgrunnlag(arbeidsforhold);
        var arbeidsforholdList = Arrays.stream(arbeidsforhold).map(BeregningsgrunnlagPrArbeidsforhold::arbeidsforhold)
                .toList();
        var uttakResultat = opprettUttak(Collections.singletonList(MØDREKVOTE_PERIODE), AktivitetStatus.ATFL, arbeidsforholdList);
        return new BeregningsresultatGrunnlag(beregningsgrunnlag, uttakResultat);
    }

    private BeregningsgrunnlagPrArbeidsforhold lagPrArbeidsforhold(double dagsatsBruker, double dagsatsArbeidsgiver, Arbeidsforhold arbeidsforhold) {
        return BeregningsgrunnlagPrArbeidsforhold.opprett(arbeidsforhold, null)
            .medRedusertRefusjonPrÅr(BigDecimal.valueOf(260 * dagsatsArbeidsgiver))
            .medRedusertBrukersAndelPrÅr(BigDecimal.valueOf(260 * dagsatsBruker));
    }

    private Beregningsgrunnlag opprettBeregningsgrunnlag(BeregningsgrunnlagPrArbeidsforhold... ekstraArbeidsforhold) {
        BeregningsgrunnlagPrStatus prStatus;
        if (ekstraArbeidsforhold.length == 0) {
            prStatus = new BeregningsgrunnlagPrStatus(AktivitetStatus.ATFL, List.of(lagPrArbeidsforhold(1000.0, 1000.0, ARBEIDSFORHOLD_1)));
        } else {
            prStatus = new BeregningsgrunnlagPrStatus(AktivitetStatus.ATFL, Arrays.asList(ekstraArbeidsforhold));
        }
        var periode = new BeregningsgrunnlagPeriode(TRE_UKER_FØR_FØDSEL_DT, LocalDate.MAX, List.of(prStatus));
        return Beregningsgrunnlag.enkelPeriode(periode);
    }

    private Beregningsgrunnlag opprettBeregningsgrunnlagForFlerePerioder() {

        var prStatus1 = new BeregningsgrunnlagPrStatus(AktivitetStatus.ATFL,
                List.of(lagPrArbeidsforhold(1000.0, 800.0, ARBEIDSFORHOLD_1)));
        var prStatus2 = new BeregningsgrunnlagPrStatus(AktivitetStatus.ATFL,
                List.of(lagPrArbeidsforhold(2000.0, 0.0, ARBEIDSFORHOLD_2)));

        var periode1 = new BeregningsgrunnlagPeriode(BG_PERIODE_1.getFomDato(), BG_PERIODE_1.getTomDato(), List.of(prStatus1));

        var periode2 = new BeregningsgrunnlagPeriode(BG_PERIODE_2.getFomDato(), BG_PERIODE_2.getTomDato(), List.of(prStatus2));

        return new Beregningsgrunnlag(List.of(periode1, periode2));
    }

    private UttakResultat opprettUttak(List<LocalDateInterval> perioder, AktivitetStatus aktivitetsStatus, List<Arbeidsforhold> arbeidsforhold) {
        List<UttakResultatPeriode> periodeListe = new ArrayList<>();
        for (var periode : perioder) {
            var uttakAktiviteter = lagUttakAktiviteter(BigDecimal.valueOf(100), BigDecimal.valueOf(0), BigDecimal.valueOf(100),
                    aktivitetsStatus, arbeidsforhold);
            periodeListe.add(new UttakResultatPeriode(periode.getFomDato(), periode.getTomDato(), uttakAktiviteter, false));
        }
        return new UttakResultat(periodeListe);
    }

    private List<UttakAktivitet> lagUttakAktiviteter(BigDecimal stillingsgrad, BigDecimal arbeidstidsgrad, BigDecimal utbetalingsgrad,
            AktivitetStatus aktivitetsStatus, List<Arbeidsforhold> arbeidsforholdList) {
        var erGradering = false;
        if (arbeidsforholdList.isEmpty()) {
            return Collections.singletonList(new UttakAktivitet(stillingsgrad, null, utbetalingsgrad,
                true, aktivitetsStatus.equals(AktivitetStatus.ATFL) ? ARBEIDSFORHOLD_1 : null, aktivitetsStatus, erGradering, stillingsgrad));
        }
        return arbeidsforholdList.stream()
                .map(arb -> {
                    var arbeidsforhold = aktivitetsStatus.equals(AktivitetStatus.ATFL) ? arb : null;
                    return new UttakAktivitet(stillingsgrad, null, utbetalingsgrad, true, arbeidsforhold, aktivitetsStatus, erGradering, stillingsgrad);
                }).toList();
    }
}

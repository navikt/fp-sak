package no.nav.foreldrepenger.ytelse.beregning.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.Beregningsresultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatAndel;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatRegelmodell;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakAktivitet;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakResultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakResultatPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Beregningsgrunnlag;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPrArbeidsforhold;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPrStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Periode;
import no.nav.foreldrepenger.ytelse.beregning.regler.RegelFastsettBeregningsresultat;
import no.nav.fpsak.tidsserie.LocalDateInterval;

public class RegelFastsettBeregningsresultatTest {
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
    private static final Arbeidsforhold ARBEIDSFORHOLD_2 = Arbeidsforhold.nyttArbeidsforholdHosVirksomhet("222",
            InternArbeidsforholdRef.nyRef().getReferanse());
    private static final Arbeidsforhold ARBEIDSFORHOLD_1 = Arbeidsforhold.nyttArbeidsforholdHosVirksomhet("111",
            InternArbeidsforholdRef.nyRef().getReferanse());
    private static final Arbeidsforhold ARBEIDSFORHOLD_3 = Arbeidsforhold.nyttArbeidsforholdHosVirksomhet("333",
            InternArbeidsforholdRef.nyRef().getReferanse());

    private RegelFastsettBeregningsresultat regel;

    @BeforeEach
    public void setup() {
        regel = new RegelFastsettBeregningsresultat();
    }

    @Test
    public void skalLageAndelForBrukerOgArbeidsgiverForEnPeriode() {
        // Arrange
        var modell = opprettRegelmodellEnPeriode();
        var output = new Beregningsresultat();

        // Act
        regel.evaluer(modell, output);

        // Assert
        var perioder = output.getBeregningsresultatPerioder();
        assertThat(perioder).hasSize(1);
        var periode = perioder.get(0);
        assertThat(periode.getFom()).isEqualTo(DAGEN_ETTER_FØDSEL);
        assertThat(periode.getTom()).isEqualTo(TI_UKER_ETTER_FØDSEL_DT);
        var andelList = periode.getBeregningsresultatAndelList();
        assertThat(andelList).hasSize(2);

        var brukerAndeler = andelList.stream().filter(BeregningsresultatAndel::erBrukerMottaker)
                .collect(Collectors.toList());
        var arbAndeler = andelList.stream().filter(a -> !a.erBrukerMottaker()).collect(Collectors.toList());
        assertThat(brukerAndeler).hasSize(1);
        assertThat(brukerAndeler.get(0).getArbeidsforhold().getIdentifikator()).isEqualTo("111");
        assertThat(brukerAndeler.get(0).getDagsats()).isEqualTo(1000);

        assertThat(arbAndeler).hasSize(1);
        assertThat(arbAndeler.get(0).getArbeidsforhold().getIdentifikator()).isEqualTo("111");
        assertThat(arbAndeler.get(0).getDagsats()).isEqualTo(1000);
    }

    @Test
    public void skalPeriodisereFlereUttaksPerioder() {
        // Arrange
        var intervalList = List.of(
                FELLESPERIODE_FØR_FØDSEL,
                MØDREKVOTE_PERIODE);
        var modell = opprettRegelmodell(intervalList, AktivitetStatus.ATFL);
        var output = new Beregningsresultat();

        // Act
        regel.evaluer(modell, output);

        // Assert
        var perioder = output.getBeregningsresultatPerioder();
        assertThat(perioder).hasSize(2);
        var periode0 = perioder.get(0);
        assertThat(periode0.getFom()).isEqualTo(TRE_UKER_FØR_FØDSEL_DT);
        assertThat(periode0.getTom()).isEqualTo(FØDSELSDATO);

        var periode1 = perioder.get(1);
        assertThat(periode1.getFom()).isEqualTo(DAGEN_ETTER_FØDSEL);
        assertThat(periode1.getTom()).isEqualTo(TI_UKER_ETTER_FØDSEL_DT);
    }

    @Test
    public void skalLageAndelerForFlereArbeidsforhold() {
        // Arrange
        var arb1 = lagPrArbeidsforhold(2000.0, 0.0, ARBEIDSFORHOLD_1);
        var arb2 = lagPrArbeidsforhold(0.0, 1500.0, ARBEIDSFORHOLD_2);
        var arb3 = lagPrArbeidsforhold(1000.0, 500.0, ARBEIDSFORHOLD_3);

        var modell = opprettRegelmodellMedArbeidsforhold(arb1, arb2, arb3);
        var output = new Beregningsresultat();

        // Act
        regel.evaluer(modell, output);

        // Assert
        var perioder = output.getBeregningsresultatPerioder();
        assertThat(perioder).hasSize(1);
        var periode = perioder.get(0);
        assertThat(periode.getFom()).isEqualTo(DAGEN_ETTER_FØDSEL);
        assertThat(periode.getTom()).isEqualTo(TI_UKER_ETTER_FØDSEL_DT);

        var andelList = periode.getBeregningsresultatAndelList();
        assertThat(andelList).hasSize(5);

        var brukerAndeler = andelList.stream().filter(BeregningsresultatAndel::erBrukerMottaker)
                .collect(Collectors.toList());
        var arbAndeler = andelList.stream().filter(a -> !a.erBrukerMottaker()).collect(Collectors.toList());
        assertThat(brukerAndeler).hasSize(3);

        assertThat(brukerAndeler.stream().filter(af -> af.getArbeidsforhold().getIdentifikator().equals("111")).collect(Collectors.toList()).get(0)
                .getDagsats()).isEqualTo(2000);
        assertThat(brukerAndeler.stream().filter(af -> af.getArbeidsforhold().getIdentifikator().equals("222")).collect(Collectors.toList()).get(0)
                .getDagsats()).isEqualTo(0);
        assertThat(brukerAndeler.stream().filter(af -> af.getArbeidsforhold().getIdentifikator().equals("333")).collect(Collectors.toList()).get(0)
                .getDagsats()).isEqualTo(1000);

        assertThat(arbAndeler).hasSize(2);
        assertThat(arbAndeler.stream().filter(af -> af.getArbeidsforhold().getIdentifikator().equals("222")).collect(Collectors.toList()).get(0)
                .getDagsats()).isEqualTo(1500);
        assertThat(arbAndeler.stream().filter(af -> af.getArbeidsforhold().getIdentifikator().equals("333")).collect(Collectors.toList()).get(0)
                .getDagsats()).isEqualTo(500);
    }

    @Test
    public void skalLageAndelerForAnonymtArbeidsforhold() {
        // Arrange
        var arb1 = lagPrArbeidsforhold(2000.0, 0.0, ANONYMT_ARBEIDSFORHOLD);
        var arb2 = lagPrArbeidsforhold(0.0, 1500.0, ARBEIDSFORHOLD_2);

        var modell = opprettRegelmodellMedArbeidsforhold(arb1, arb2);
        var output = new Beregningsresultat();

        // Act
        regel.evaluer(modell, output);

        // Assert
        var perioder = output.getBeregningsresultatPerioder();
        assertThat(perioder).hasSize(1);
        var periode = perioder.get(0);
        assertThat(periode.getFom()).isEqualTo(DAGEN_ETTER_FØDSEL);
        assertThat(periode.getTom()).isEqualTo(TI_UKER_ETTER_FØDSEL_DT);

        var andelList = periode.getBeregningsresultatAndelList();
        assertThat(andelList).hasSize(3);

        var brukerAndeler = andelList.stream().filter(BeregningsresultatAndel::erBrukerMottaker)
                .collect(Collectors.toList());
        var arbAndeler = andelList.stream().filter(a -> !a.erBrukerMottaker()).collect(Collectors.toList());
        assertThat(brukerAndeler).hasSize(2);

        assertThat(brukerAndeler.stream().filter(af -> af.getArbeidsforhold() == null).collect(Collectors.toList()).get(0).getDagsats())
                .isEqualTo(2000);
        assertThat(brukerAndeler.stream().filter(af -> "222".equals(af.getArbeidsgiverId())).collect(Collectors.toList()).get(0).getDagsats())
                .isEqualTo(0);

        assertThat(arbAndeler).hasSize(1);
        assertThat(arbAndeler.stream().filter(af -> "222".equals(af.getArbeidsforhold().getIdentifikator())).collect(Collectors.toList()).get(0)
                .getDagsats()).isEqualTo(1500);
    }

    @Test
    public void skalPeriodisereFlereUttaksPerioderOgBeregningsgrunnlagPerioder() {
        // Arrange
        var modell = opprettRegelmodellMedFlereBGOgUttakPerioder();
        var output = new Beregningsresultat();

        // Act
        regel.evaluer(modell, output);

        // Assert
        var perioder = output.getBeregningsresultatPerioder();
        assertThat(perioder).hasSize(4);

        var periode1 = perioder.get(0);
        assertThat(periode1.getFom()).isEqualTo(TRE_UKER_FØR_FØDSEL_DT);
        assertThat(periode1.getTom()).isEqualTo(FØDSELSDATO);
        assertThat(periode1.getBeregningsresultatAndelList()).hasSize(2);
        assertThat(periode1.getBeregningsresultatAndelList().stream().filter(BeregningsresultatAndel::erBrukerMottaker)
                .collect(Collectors.toList()).get(0).getDagsats()).isEqualTo(1000);
        assertThat(periode1.getBeregningsresultatAndelList().stream().filter(a -> !a.erBrukerMottaker())
                .collect(Collectors.toList()).get(0).getDagsats()).isEqualTo(800);

        var periode2 = perioder.get(1);
        assertThat(periode2.getFom()).isEqualTo(DAGEN_ETTER_FØDSEL);
        assertThat(periode2.getTom()).isEqualTo(FØDSELSDATO.plusWeeks(4));
        assertThat(periode2.getBeregningsresultatAndelList()).hasSize(2);
        assertThat(periode2.getBeregningsresultatAndelList().stream().filter(BeregningsresultatAndel::erBrukerMottaker)
                .collect(Collectors.toList()).get(0).getDagsats()).isEqualTo(1000);
        assertThat(periode2.getBeregningsresultatAndelList().stream().filter(a -> !a.erBrukerMottaker())
                .collect(Collectors.toList()).get(0).getDagsats()).isEqualTo(800);

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
    public void skalRundeAvAndelerRiktig() {
        // Arrange
        var arb1 = lagPrArbeidsforhold(2165.49, 0.00, ARBEIDSFORHOLD_1);
        var arb2 = lagPrArbeidsforhold(0.455, 1550.50, ARBEIDSFORHOLD_2);
        var arb3 = lagPrArbeidsforhold(1001.50, 500.49, ARBEIDSFORHOLD_3);

        var modell = opprettRegelmodellMedArbeidsforhold(arb1, arb2, arb3);
        var output = new Beregningsresultat();

        // Act
        regel.evaluer(modell, output);

        // Assert
        var andelList = output.getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList();
        assertThat(andelList).hasSize(5);

        var brukerAndeler = andelList.stream().filter(BeregningsresultatAndel::erBrukerMottaker)
                .collect(Collectors.toList());
        var arbAndeler = andelList.stream().filter(a -> !a.erBrukerMottaker()).collect(Collectors.toList());
        assertThat(brukerAndeler).hasSize(3);

        assertThat(brukerAndeler.stream().filter(af -> af.getArbeidsforhold().getIdentifikator().equals("111")).collect(Collectors.toList()).get(0)
                .getDagsats()).isEqualTo(2165);
        assertThat(brukerAndeler.stream().filter(af -> af.getArbeidsforhold().getIdentifikator().equals("222")).collect(Collectors.toList()).get(0)
                .getDagsats()).isEqualTo(0);
        assertThat(brukerAndeler.stream().filter(af -> af.getArbeidsforhold().getIdentifikator().equals("333")).collect(Collectors.toList()).get(0)
                .getDagsats()).isEqualTo(1002);

        assertThat(arbAndeler).hasSize(2);
        assertThat(arbAndeler.stream().filter(af -> af.getArbeidsforhold().getIdentifikator().equals("222")).collect(Collectors.toList()).get(0)
                .getDagsats()).isEqualTo(1551);
        assertThat(arbAndeler.stream().filter(af -> af.getArbeidsforhold().getIdentifikator().equals("333")).collect(Collectors.toList()).get(0)
                .getDagsats()).isEqualTo(500);
    }

    private BeregningsresultatRegelmodell opprettRegelmodellEnPeriode() {
        var perioder = Collections.singletonList(MØDREKVOTE_PERIODE);
        return opprettRegelmodell(perioder, AktivitetStatus.ATFL);
    }

    private BeregningsresultatRegelmodell opprettRegelmodell(List<LocalDateInterval> perioder, AktivitetStatus aktivitetsStatus) {
        var beregningsgrunnlag = opprettBeregningsgrunnlag();
        var uttakResultat = opprettUttak(perioder, aktivitetsStatus, Collections.emptyList());
        return new BeregningsresultatRegelmodell(beregningsgrunnlag, uttakResultat);
    }

    private BeregningsresultatRegelmodell opprettRegelmodellMedFlereBGOgUttakPerioder() {
        var beregningsgrunnlag = opprettBeregningsgrunnlagForFlerePerioder();
        var uttakPerioder = List.of(
                FELLESPERIODE_FØR_FØDSEL,
                MØDREKVOTE_PERIODE,
                FELLESPERIODE);
        var arbeidsforholdList = List.of(ARBEIDSFORHOLD_1, ARBEIDSFORHOLD_2);
        var uttakResultat = opprettUttak(uttakPerioder, AktivitetStatus.ATFL, arbeidsforholdList);
        return new BeregningsresultatRegelmodell(beregningsgrunnlag, uttakResultat);
    }

    private BeregningsresultatRegelmodell opprettRegelmodellMedArbeidsforhold(BeregningsgrunnlagPrArbeidsforhold... arbeidsforhold) {
        var beregningsgrunnlag = opprettBeregningsgrunnlag(arbeidsforhold);
        var arbeidsforholdList = Arrays.stream(arbeidsforhold).map(BeregningsgrunnlagPrArbeidsforhold::getArbeidsforhold)
                .collect(Collectors.toList());
        var uttakResultat = opprettUttak(Collections.singletonList(MØDREKVOTE_PERIODE), AktivitetStatus.ATFL, arbeidsforholdList);
        return new BeregningsresultatRegelmodell(beregningsgrunnlag, uttakResultat);
    }

    private BeregningsgrunnlagPrArbeidsforhold lagPrArbeidsforhold(double dagsatsBruker, double dagsatsArbeidsgiver, Arbeidsforhold arbeidsforhold) {
        return BeregningsgrunnlagPrArbeidsforhold.builder()
                .medArbeidsforhold(arbeidsforhold)
                .medRedusertRefusjonPrÅr(BigDecimal.valueOf(260 * dagsatsArbeidsgiver))
                .medRedusertBrukersAndelPrÅr(BigDecimal.valueOf(260 * dagsatsBruker))
                .build();
    }

    private Beregningsgrunnlag opprettBeregningsgrunnlag(BeregningsgrunnlagPrArbeidsforhold... ekstraArbeidsforhold) {
        var prStatusBuilder = BeregningsgrunnlagPrStatus.builder()
                .medAktivitetStatus(AktivitetStatus.ATFL);
        if (ekstraArbeidsforhold.length == 0) {
            prStatusBuilder.medArbeidsforhold(lagPrArbeidsforhold(1000.0, 1000.0, ARBEIDSFORHOLD_1));
        }
        for (var arbeidsforhold : ekstraArbeidsforhold) {
            prStatusBuilder.medArbeidsforhold(arbeidsforhold);
        }

        var prStatus = prStatusBuilder.build();
        var periode = BeregningsgrunnlagPeriode.builder()
                .medPeriode(Periode.of(TRE_UKER_FØR_FØDSEL_DT, LocalDate.MAX))
                .medBeregningsgrunnlagPrStatus(prStatus)
                .build();
        return Beregningsgrunnlag.builder()
                .medAktivitetStatuser(Collections.singletonList(AktivitetStatus.ATFL))
                .medSkjæringstidspunkt(LocalDate.now())
                .medBeregningsgrunnlagPeriode(periode)
                .build();
    }

    private Beregningsgrunnlag opprettBeregningsgrunnlagForFlerePerioder() {

        var prStatus1 = BeregningsgrunnlagPrStatus.builder().medAktivitetStatus(AktivitetStatus.ATFL)
                .medArbeidsforhold(lagPrArbeidsforhold(1000.0, 800.0, ARBEIDSFORHOLD_1)).build();
        var prStatus2 = BeregningsgrunnlagPrStatus.builder().medAktivitetStatus(AktivitetStatus.ATFL)
                .medArbeidsforhold(lagPrArbeidsforhold(2000.0, 0.0, ARBEIDSFORHOLD_2)).build();

        var periode1 = BeregningsgrunnlagPeriode.builder()
                .medPeriode(Periode.of(BG_PERIODE_1.getFomDato(), BG_PERIODE_1.getTomDato()))
                .medBeregningsgrunnlagPrStatus(prStatus1)
                .build();

        var periode2 = BeregningsgrunnlagPeriode.builder()
                .medPeriode(Periode.of(BG_PERIODE_2.getFomDato(), BG_PERIODE_2.getTomDato()))
                .medBeregningsgrunnlagPrStatus(prStatus2)
                .build();

        return Beregningsgrunnlag.builder()
                .medAktivitetStatuser(Collections.singletonList(AktivitetStatus.ATFL))
                .medSkjæringstidspunkt(LocalDate.now())
                .medBeregningsgrunnlagPeriode(periode1)
                .medBeregningsgrunnlagPeriode(periode2)
                .build();
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
                    aktivitetsStatus.equals(AktivitetStatus.ATFL) ? ARBEIDSFORHOLD_1 : null, aktivitetsStatus, erGradering, stillingsgrad));
        }
        return arbeidsforholdList.stream()
                .map(arb -> {
                    var arbeidsforhold = aktivitetsStatus.equals(AktivitetStatus.ATFL) ? arb : null;
                    return new UttakAktivitet(stillingsgrad, null, utbetalingsgrad, arbeidsforhold, aktivitetsStatus, erGradering, stillingsgrad);
                }).collect(Collectors.toList());
    }
}

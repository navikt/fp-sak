package no.nav.foreldrepenger.ytelse.beregning.regler.feriepenger;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatAndel;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Dekningsgrad;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;
import no.nav.fpsak.nare.evaluation.summary.EvaluationSerializer;
import no.nav.fpsak.tidsserie.LocalDateInterval;

public class RegelBeregnFeriepengerTest {

    private Arbeidsforhold arbeidsforhold1 = Arbeidsforhold.nyttArbeidsforholdHosVirksomhet("123456789");
    private Arbeidsforhold arbeidsforhold2 = Arbeidsforhold.nyttArbeidsforholdHosVirksomhet("987654321");

    // Eksempler tatt fra
    // https://confluence.adeo.no/display/MODNAV/27c+Beregn+feriepenger+PK-51965+OMR-49

    // Eksempel 1 Mor
    @Test
    public void skalBeregneFeriepengerForMor() {
        var periode1 = byggBRPeriode(LocalDate.of(2018, 1, 6), LocalDate.of(2018, 3, 9));
        var periode2 = byggBRPeriode(LocalDate.of(2018, 3, 10), LocalDate.of(2018, 3, 16));
        byggAndelerForPeriode(periode1, 350, 600, arbeidsforhold1);
        byggAndelerForPeriode(periode1, 100, 500, arbeidsforhold2);
        byggAndelerForPeriode(periode2, 150, 400, arbeidsforhold1);

        var periode1annenPart = byggBRPeriode(LocalDate.of(2018, 3, 10), LocalDate.of(2018, 3, 16));
        var periode2annenPart = byggBRPeriode(LocalDate.of(2018, 3, 17), LocalDate.of(2018, 3, 31));
        byggAndelerForPeriode(periode1annenPart, 200, 0, arbeidsforhold1);
        byggAndelerForPeriode(periode2annenPart, 300, 0, arbeidsforhold1);

        var annenPartsBeregningsresultatPerioder = List.of(periode1annenPart, periode2annenPart);
        var regelModell = BeregningsresultatFeriepengerRegelModell.builder()
                .medBeregningsresultatPerioder(List.of(periode1, periode2))
                .medAnnenPartsBeregningsresultatPerioder(annenPartsBeregningsresultatPerioder)
                .medInntektskategorier(Collections.singleton(Inntektskategori.ARBEIDSTAKER))
                .medAnnenPartsInntektskategorier(Collections.singleton(Inntektskategori.ARBEIDSTAKER))
                .medDekningsgrad(Dekningsgrad.DEKNINGSGRAD_100)
                .medErForelder1(true)
                .medAntallDagerFeriepenger(60)
                .build();

        var regel = new RegelBeregnFeriepenger();
        var evaluation = regel.evaluer(regelModell);
        var sporing = EvaluationSerializer.asJson(evaluation);

        assertThat(sporing).isNotNull();
        assertThat(regelModell.getFeriepengerPeriode().getFomDato()).isEqualTo(LocalDate.of(2018, 1, 6));
        assertThat(regelModell.getFeriepengerPeriode().getTomDato()).isEqualTo(LocalDate.of(2018, 3, 23));

        regelModell.getBeregningsresultatPerioder().stream().flatMap(p -> p.getBeregningsresultatAndelList().stream())
                .forEach(andel -> assertThat(andel.getBeregningsresultatFeriepengerPrÅrListe()).hasSize(1));
        var andelBruker1 = periode1.getBeregningsresultatAndelList().get(0);
        var andelArbeidsgiver1 = periode1.getBeregningsresultatAndelList().get(1);
        var andelBruker2 = periode1.getBeregningsresultatAndelList().get(2);
        var andelArbeidsgiver2 = periode1.getBeregningsresultatAndelList().get(3);
        var andelBruker3 = periode2.getBeregningsresultatAndelList().get(0);
        var andelArbeidsgiver3 = periode2.getBeregningsresultatAndelList().get(1);

        assertThat(andelBruker1.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(1606.5));
        assertThat(andelBruker2.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(459));
        assertThat(andelArbeidsgiver1.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp())
                .isEqualByComparingTo(BigDecimal.valueOf(2754));
        assertThat(andelArbeidsgiver2.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp())
                .isEqualByComparingTo(BigDecimal.valueOf(2295));
        assertThat(andelBruker3.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(76.5));
        assertThat(andelArbeidsgiver3.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(204));
    }

    // Eksempel 1X Mor med avslag i første periode
    @Test
    public void skalBeregneFeriepengerForMorMedAvslagIFørstePeriode() {
        var periode0 = byggBRPeriode(LocalDate.of(2018, 1, 5), LocalDate.of(2018, 1, 5));
        var periode1 = byggBRPeriode(LocalDate.of(2018, 1, 6), LocalDate.of(2018, 3, 9));
        var periode2 = byggBRPeriode(LocalDate.of(2018, 3, 10), LocalDate.of(2018, 3, 16));
        byggAndelerForPeriode(periode0, 0, 0, arbeidsforhold1);
        byggAndelerForPeriode(periode1, 350, 600, arbeidsforhold1);
        byggAndelerForPeriode(periode1, 100, 500, arbeidsforhold2);
        byggAndelerForPeriode(periode2, 150, 400, arbeidsforhold1);

        var periode1annenPart = byggBRPeriode(LocalDate.of(2018, 3, 10), LocalDate.of(2018, 3, 16));
        var periode2annenPart = byggBRPeriode(LocalDate.of(2018, 3, 17), LocalDate.of(2018, 3, 31));
        byggAndelerForPeriode(periode1annenPart, 200, 0, arbeidsforhold1);
        byggAndelerForPeriode(periode2annenPart, 300, 0, arbeidsforhold1);

        var annenPartsBeregningsresultatPerioder = List.of(periode1annenPart, periode2annenPart);
        var regelModell = BeregningsresultatFeriepengerRegelModell.builder()
                .medBeregningsresultatPerioder(List.of(periode0, periode1, periode2))
                .medAnnenPartsBeregningsresultatPerioder(annenPartsBeregningsresultatPerioder)
                .medInntektskategorier(Collections.singleton(Inntektskategori.ARBEIDSTAKER))
                .medAnnenPartsInntektskategorier(Collections.singleton(Inntektskategori.ARBEIDSTAKER))
                .medDekningsgrad(Dekningsgrad.DEKNINGSGRAD_100)
                .medErForelder1(true)
                .medAntallDagerFeriepenger(60)
                .build();

        var regel = new RegelBeregnFeriepenger();
        var evaluation = regel.evaluer(regelModell);
        var sporing = EvaluationSerializer.asJson(evaluation);

        assertThat(sporing).isNotNull();
        assertThat(regelModell.getFeriepengerPeriode().getFomDato()).isEqualTo(LocalDate.of(2018, 1, 6));
        assertThat(regelModell.getFeriepengerPeriode().getTomDato()).isEqualTo(LocalDate.of(2018, 3, 23));

        regelModell.getBeregningsresultatPerioder().stream().flatMap(p -> p.getBeregningsresultatAndelList().stream())
                .forEach(andel -> {
                    if (andel.getDagsats() > 0) {
                        assertThat(andel.getBeregningsresultatFeriepengerPrÅrListe()).hasSize(1);
                    } else {
                        assertThat(andel.getBeregningsresultatFeriepengerPrÅrListe()).isEmpty();
                    }
                });
        var andelBruker1 = periode1.getBeregningsresultatAndelList().get(0);
        var andelArbeidsgiver1 = periode1.getBeregningsresultatAndelList().get(1);
        var andelBruker2 = periode1.getBeregningsresultatAndelList().get(2);
        var andelArbeidsgiver2 = periode1.getBeregningsresultatAndelList().get(3);
        var andelBruker3 = periode2.getBeregningsresultatAndelList().get(0);
        var andelArbeidsgiver3 = periode2.getBeregningsresultatAndelList().get(1);

        assertThat(andelBruker1.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(1606.5));
        assertThat(andelBruker2.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(459));
        assertThat(andelArbeidsgiver1.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp())
                .isEqualByComparingTo(BigDecimal.valueOf(2754));
        assertThat(andelArbeidsgiver2.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp())
                .isEqualByComparingTo(BigDecimal.valueOf(2295));
        assertThat(andelBruker3.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(76.5));
        assertThat(andelArbeidsgiver3.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(204));
    }

    // Eksempel 1 Far
    @Test
    public void skalBeregneFeriepengerForFar() {
        // Arrange
        var periode1annenPart = byggBRPeriode(LocalDate.of(2018, 1, 6), LocalDate.of(2018, 3, 9));
        var periode2annenPart = byggBRPeriode(LocalDate.of(2018, 3, 10), LocalDate.of(2018, 3, 16));
        byggAndelerForPeriode(periode1annenPart, 0, 1, arbeidsforhold1);
        byggAndelerForPeriode(periode2annenPart, 0, 1, arbeidsforhold1);
        var annenPartsBeregningsresultatPerioder = List.of(periode1annenPart, periode2annenPart);

        var periode1 = byggBRPeriode(LocalDate.of(2018, 3, 10), LocalDate.of(2018, 3, 16));
        var periode2 = byggBRPeriode(LocalDate.of(2018, 3, 17), LocalDate.of(2018, 3, 31));
        byggAndelerForPeriode(periode1, 150, 400, arbeidsforhold1);
        byggAndelerForPeriode(periode2, 460, 1000, arbeidsforhold1);

        var regelModell = BeregningsresultatFeriepengerRegelModell.builder()
                .medBeregningsresultatPerioder(List.of(periode1, periode2))
                .medAnnenPartsBeregningsresultatPerioder(annenPartsBeregningsresultatPerioder)
                .medInntektskategorier(Collections.singleton(Inntektskategori.ARBEIDSTAKER))
                .medAnnenPartsInntektskategorier(Collections.singleton(Inntektskategori.ARBEIDSTAKER))
                .medDekningsgrad(Dekningsgrad.DEKNINGSGRAD_100)
                .medErForelder1(false)
                .medAntallDagerFeriepenger(60)
                .build();
        // Act
        var regel = new RegelBeregnFeriepenger();
        var evaluation = regel.evaluer(regelModell);
        var sporing = EvaluationSerializer.asJson(evaluation);

        // Assert
        assertThat(sporing).isNotNull();
        assertThat(regelModell.getFeriepengerPeriode().getFomDato()).isEqualTo(LocalDate.of(2018, 1, 6));
        assertThat(regelModell.getFeriepengerPeriode().getTomDato()).isEqualTo(LocalDate.of(2018, 3, 23));

        regelModell.getBeregningsresultatPerioder().stream().flatMap(p -> p.getBeregningsresultatAndelList().stream())
                .forEach(andel -> assertThat(andel.getBeregningsresultatFeriepengerPrÅrListe()).hasSize(1));
        var andelBruker1 = periode1.getBeregningsresultatAndelList().get(0);
        var andelArbeidsgiver1 = periode1.getBeregningsresultatAndelList().get(1);
        var andelBruker2 = periode2.getBeregningsresultatAndelList().get(0);
        var andelArbeidsgiver2 = periode2.getBeregningsresultatAndelList().get(1);

        assertThat(andelBruker1.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(76.5));
        assertThat(andelBruker2.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(234.6));
        assertThat(andelArbeidsgiver1.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(204));
        assertThat(andelArbeidsgiver2.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(510));
    }

    // Eksempel 2 Mor
    @Test
    public void skalBeregneFeriepengerForMorEksempel2() {
        var periode1 = byggBRPeriode(LocalDate.of(2018, 1, 17), LocalDate.of(2018, 3, 20));
        var periode2 = byggBRPeriode(LocalDate.of(2018, 3, 21), LocalDate.of(2018, 3, 28));
        var periode3 = byggBRPeriode(LocalDate.of(2018, 3, 29), LocalDate.of(2018, 4, 8));
        byggAndelerForPeriode(periode1, 350, 600, arbeidsforhold1);
        byggAndelerForPeriode(periode1, 100, 500, arbeidsforhold2);
        byggAndelerForPeriode(periode2, 0, 0, arbeidsforhold1);
        byggAndelerForPeriode(periode3, 350, 600, arbeidsforhold1);

        var periode1annenPart = byggBRPeriode(LocalDate.of(2018, 3, 21), LocalDate.of(2018, 4, 15));
        byggAndelerForPeriode(periode1annenPart, 500, 0, arbeidsforhold1);
        var annenPartsBeregningsresultatPerioder = List.of(periode1annenPart);

        var regelModell = BeregningsresultatFeriepengerRegelModell.builder()
                .medBeregningsresultatPerioder(List.of(periode1, periode2, periode3))
                .medAnnenPartsBeregningsresultatPerioder(annenPartsBeregningsresultatPerioder)
                .medInntektskategorier(Collections.singleton(Inntektskategori.ARBEIDSTAKER))
                .medAnnenPartsInntektskategorier(Collections.singleton(Inntektskategori.ARBEIDSTAKER))
                .medDekningsgrad(Dekningsgrad.DEKNINGSGRAD_100)
                .medErForelder1(true)
                .medAntallDagerFeriepenger(60)
                .build();

        var regel = new RegelBeregnFeriepenger();
        var evaluation = regel.evaluer(regelModell);
        var sporing = EvaluationSerializer.asJson(evaluation);

        assertThat(sporing).isNotNull();
        assertThat(regelModell.getFeriepengerPeriode().getFomDato()).isEqualTo(LocalDate.of(2018, 1, 17));
        assertThat(regelModell.getFeriepengerPeriode().getTomDato()).isEqualTo(LocalDate.of(2018, 4, 4));

        assertThat(regelModell.getBeregningsresultatPerioder().stream().flatMap(p -> p.getBeregningsresultatAndelList().stream())
                .flatMap(a -> a.getBeregningsresultatFeriepengerPrÅrListe().stream()).collect(Collectors.toList())).hasSize(6);
        periode1.getBeregningsresultatAndelList().forEach(andel -> assertThat(andel.getBeregningsresultatFeriepengerPrÅrListe()).hasSize(1));
        periode2.getBeregningsresultatAndelList().forEach(andel -> assertThat(andel.getBeregningsresultatFeriepengerPrÅrListe()).isEmpty());
        periode3.getBeregningsresultatAndelList().forEach(andel -> assertThat(andel.getBeregningsresultatFeriepengerPrÅrListe()).hasSize(1));

        var andelBruker1 = periode1.getBeregningsresultatAndelList().get(0);
        var andelArbeidsgiver1 = periode1.getBeregningsresultatAndelList().get(1);
        var andelBruker2 = periode1.getBeregningsresultatAndelList().get(2);
        var andelArbeidsgiver2 = periode1.getBeregningsresultatAndelList().get(3);
        var andelBruker4 = periode3.getBeregningsresultatAndelList().get(0);
        var andelArbeidsgiver4 = periode3.getBeregningsresultatAndelList().get(1);

        assertThat(andelBruker1.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(1606.5));
        assertThat(andelBruker2.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(459));
        assertThat(andelArbeidsgiver1.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp())
                .isEqualByComparingTo(BigDecimal.valueOf(2754));
        assertThat(andelArbeidsgiver2.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp())
                .isEqualByComparingTo(BigDecimal.valueOf(2295));
        assertThat(andelBruker4.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(178.5));
        assertThat(andelArbeidsgiver4.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(306));
    }

    // Eksempel 2 Far
    @Test
    public void skalBeregneFeriepengerForFarEksempel2() {
        var periode1 = byggBRPeriode(LocalDate.of(2018, 3, 21), LocalDate.of(2018, 4, 15));
        byggAndelerForPeriode(periode1, 150, 400, arbeidsforhold1);

        var periode1annenPart = byggBRPeriode(LocalDate.of(2018, 1, 17), LocalDate.of(2018, 3, 20));
        var periode2annenPart = byggBRPeriode(LocalDate.of(2018, 3, 21), LocalDate.of(2018, 3, 28));
        var periode3annenPart = byggBRPeriode(LocalDate.of(2018, 3, 29), LocalDate.of(2018, 4, 8));
        byggAndelerForPeriode(periode1annenPart, 500, 0, arbeidsforhold1);
        byggAndelerForPeriode(periode2annenPart, 0, 0, arbeidsforhold1);
        byggAndelerForPeriode(periode3annenPart, 500, 0, arbeidsforhold1);
        var annenPartsBeregningsresultatPerioder = List.of(periode1annenPart, periode2annenPart, periode3annenPart);

        var regelModell = BeregningsresultatFeriepengerRegelModell.builder()
                .medBeregningsresultatPerioder(List.of(periode1))
                .medAnnenPartsBeregningsresultatPerioder(annenPartsBeregningsresultatPerioder)
                .medInntektskategorier(Collections.singleton(Inntektskategori.ARBEIDSTAKER))
                .medAnnenPartsInntektskategorier(Collections.singleton(Inntektskategori.ARBEIDSTAKER))
                .medDekningsgrad(Dekningsgrad.DEKNINGSGRAD_100)
                .medErForelder1(false)
                .medAntallDagerFeriepenger(60)
                .build();

        var regel = new RegelBeregnFeriepenger();
        var evaluation = regel.evaluer(regelModell);
        var sporing = EvaluationSerializer.asJson(evaluation);

        assertThat(sporing).isNotNull();
        assertThat(regelModell.getFeriepengerPeriode().getFomDato()).isEqualTo(LocalDate.of(2018, 1, 17));
        assertThat(regelModell.getFeriepengerPeriode().getTomDato()).isEqualTo(LocalDate.of(2018, 4, 3));

        assertThat(regelModell.getBeregningsresultatPerioder().stream().flatMap(p -> p.getBeregningsresultatAndelList().stream())
                .flatMap(a -> a.getBeregningsresultatFeriepengerPrÅrListe().stream()).collect(Collectors.toList())).hasSize(2);
        periode1.getBeregningsresultatAndelList().forEach(andel -> assertThat(andel.getBeregningsresultatFeriepengerPrÅrListe()).hasSize(1));

        var andelBruker1 = periode1.getBeregningsresultatAndelList().get(0);
        var andelArbeidsgiver1 = periode1.getBeregningsresultatAndelList().get(1);

        assertThat(andelBruker1.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(153));
        assertThat(andelArbeidsgiver1.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(408));
    }

    @Test
    public void skalBeregneFeriepengerOverFlereÅr() {
        var periode1 = byggBRPeriode(LocalDate.of(2018, 11, 1), LocalDate.of(2019, 1, 5)); // 47 ukedager
        var periode2 = byggBRPeriode(LocalDate.of(2019, 1, 6), LocalDate.of(2019, 2, 5)); // 22 ukedager
        var periode3 = byggBRPeriode(LocalDate.of(2019, 2, 6), LocalDate.of(2019, 4, 16)); // 50 ukedager
        byggAndelerForPeriode(periode1, 1000, 0, arbeidsforhold1);
        byggAndelerForPeriode(periode2, 0, 0, arbeidsforhold1);
        byggAndelerForPeriode(periode3, 500, 500, arbeidsforhold1);

        var periode1annenPart = byggBRPeriode(LocalDate.of(2019, 1, 25), LocalDate.of(2019, 2, 15));
        byggAndelerForPeriode(periode1annenPart, 500, 0, arbeidsforhold1);
        var annenPartsBeregningsresultatPerioder = List.of(periode1annenPart);

        var regelModell = BeregningsresultatFeriepengerRegelModell.builder()
                .medBeregningsresultatPerioder(List.of(periode1, periode2, periode3))
                .medAnnenPartsBeregningsresultatPerioder(annenPartsBeregningsresultatPerioder)
                .medInntektskategorier(Collections.singleton(Inntektskategori.ARBEIDSTAKER))
                .medAnnenPartsInntektskategorier(Collections.singleton(Inntektskategori.SJØMANN))
                .medDekningsgrad(Dekningsgrad.DEKNINGSGRAD_80)
                .medErForelder1(true)
                .medAntallDagerFeriepenger(60)
                .build();

        var regel = new RegelBeregnFeriepenger();
        var evaluation = regel.evaluer(regelModell);
        var sporing = EvaluationSerializer.asJson(evaluation);

        assertThat(sporing).isNotNull();
        assertThat(regelModell.getFeriepengerPeriode().getFomDato()).isEqualTo(LocalDate.of(2018, 11, 1));
        assertThat(regelModell.getFeriepengerPeriode().getTomDato()).isEqualTo(LocalDate.of(2019, 2, 21));

        regelModell.getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList()
                .forEach(andel -> assertThat(andel.getBeregningsresultatFeriepengerPrÅrListe()).hasSize(2));
        regelModell.getBeregningsresultatPerioder().get(1).getBeregningsresultatAndelList()
                .forEach(andel -> assertThat(andel.getBeregningsresultatFeriepengerPrÅrListe()).isEmpty());
        regelModell.getBeregningsresultatPerioder().get(2).getBeregningsresultatAndelList()
                .forEach(andel -> assertThat(andel.getBeregningsresultatFeriepengerPrÅrListe()).hasSize(1));
        var andelBruker1 = periode1.getBeregningsresultatAndelList().get(0);
        var andelBruker2 = periode3.getBeregningsresultatAndelList().get(0);
        var andelArbeidsgiver = periode3.getBeregningsresultatAndelList().get(1);

        assertThat(andelBruker1.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(4386));
        assertThat(andelBruker1.getBeregningsresultatFeriepengerPrÅrListe().get(1).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(408));

        assertThat(andelBruker2.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(612));
        assertThat(andelArbeidsgiver.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(612));
    }

    @Test
    public void skalBeregneFeriepengerUtenAnnenpart() {
        var periode1 = byggBRPeriode(LocalDate.of(2018, 11, 1), LocalDate.of(2018, 12, 31)); // 43 ukedager
        byggAndelerForPeriode(periode1, 1000, 0, arbeidsforhold1);

        var regelModell = BeregningsresultatFeriepengerRegelModell.builder()
                .medBeregningsresultatPerioder(List.of(periode1))
                .medAnnenPartsBeregningsresultatPerioder(Collections.emptyList())
                .medInntektskategorier(Collections.singleton(Inntektskategori.ARBEIDSTAKER))
                .medAnnenPartsInntektskategorier(Collections.emptySet())
                .medDekningsgrad(Dekningsgrad.DEKNINGSGRAD_100)
                .medErForelder1(true)
                .medAntallDagerFeriepenger(60)
                .build();

        var regel = new RegelBeregnFeriepenger();
        var evaluation = regel.evaluer(regelModell);
        var sporing = EvaluationSerializer.asJson(evaluation);

        assertThat(sporing).isNotNull();
        assertThat(regelModell.getFeriepengerPeriode().getFomDato()).isEqualTo(LocalDate.of(2018, 11, 1));
        assertThat(regelModell.getFeriepengerPeriode().getTomDato()).isEqualTo(LocalDate.of(2018, 12, 31));

        regelModell.getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList()
                .forEach(andel -> assertThat(andel.getBeregningsresultatFeriepengerPrÅrListe()).hasSize(1));
        var andelBruker1 = periode1.getBeregningsresultatAndelList().get(0);

        assertThat(andelBruker1.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(4386));
    }

    @Test
    public void skalBeregneFeriepengerPeriodeForSvangerskapspenger() {
        var periode1 = byggBRPeriode(LocalDate.of(2018, 1, 6), LocalDate.of(2018, 3, 9));
        var periode2 = byggBRPeriode(LocalDate.of(2018, 3, 10), LocalDate.of(2018, 5, 1));
        byggAndelerForPeriode(periode1, 700, 500, arbeidsforhold1);
        byggAndelerForPeriode(periode2, 300, 200, arbeidsforhold1);

        var regelModell = BeregningsresultatFeriepengerRegelModell.builder()
                .medBeregningsresultatPerioder(List.of(periode1, periode2))
                .medAnnenPartsBeregningsresultatPerioder(List.of())
                .medInntektskategorier(Set.of(Inntektskategori.ARBEIDSTAKER))
                .medAnnenPartsInntektskategorier(Set.of())
                .medDekningsgrad(Dekningsgrad.DEKNINGSGRAD_100)
                .medErForelder1(true)
                .medAntallDagerFeriepenger(64) // SVP-spesifikt
                .build();

        var regel = new RegelBeregnFeriepenger();
        var evaluation = regel.evaluer(regelModell);
        var sporing = EvaluationSerializer.asJson(evaluation);

        assertThat(sporing).isNotNull();
        assertThat(regelModell.getFeriepengerPeriode().getFomDato()).isEqualTo(LocalDate.of(2018, 1, 6));
        assertThat(regelModell.getFeriepengerPeriode().getTomDato()).isEqualTo(LocalDate.of(2018, 4, 5));

        regelModell.getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList()
            .forEach(andel -> assertThat(andel.getBeregningsresultatFeriepengerPrÅrListe()).hasSize(1));

        assertThat(periode1.getBeregningsresultatAndelList().get(0).getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(3213));
    }

    @Test
    public void skalIkkeBeregneFeriepengerPeriodeForSvangerskapspengerSN() {
        var periode1 = byggBRPeriode(LocalDate.of(2018, 1, 6), LocalDate.of(2018, 3, 9));
        var periode2 = byggBRPeriode(LocalDate.of(2018, 3, 10), LocalDate.of(2018, 5, 1));
        byggSNAndelForPeriode(periode1, 700);
        byggSNAndelForPeriode(periode2, 300);

        var regelModell = BeregningsresultatFeriepengerRegelModell.builder()
            .medBeregningsresultatPerioder(List.of(periode1, periode2))
            .medAnnenPartsBeregningsresultatPerioder(List.of())
            .medInntektskategorier(Set.of(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE))
            .medAnnenPartsInntektskategorier(Set.of())
            .medDekningsgrad(Dekningsgrad.DEKNINGSGRAD_100)
            .medErForelder1(true)
            .medAntallDagerFeriepenger(64) // SVP-spesifikt
            .build();

        var regel = new RegelBeregnFeriepenger();
        var evaluation = regel.evaluer(regelModell);
        var sporing = EvaluationSerializer.asJson(evaluation);

        assertThat(sporing).isNotNull();
        assertThat(regelModell.getFeriepengerPeriode()).isNull();

        regelModell.getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList()
            .forEach(andel -> assertThat(andel.getBeregningsresultatFeriepengerPrÅrListe()).isEmpty());
    }

    // Skal beregne feriepenger fom tilkommet AT-andel
    @Test
    public void skalBeregneFeriepengerPeriodeForSvangerskapspengerTilkommetAT() {
        var periode1 = byggBRPeriode(LocalDate.of(2018, 1, 6), LocalDate.of(2018, 3, 9));
        var periode2 = byggBRPeriode(LocalDate.of(2018, 3, 10), LocalDate.of(2018, 7, 1));
        byggSNAndelForPeriode(periode1, 700);
        byggSNAndelForPeriode(periode2, 300);
        byggAndelerForPeriode(periode2, 625, 250, arbeidsforhold1);

        var regelModell = BeregningsresultatFeriepengerRegelModell.builder()
            .medBeregningsresultatPerioder(List.of(periode1, periode2))
            .medAnnenPartsBeregningsresultatPerioder(List.of())
            .medInntektskategorier(Set.of(Inntektskategori.ARBEIDSTAKER, Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE))
            .medAnnenPartsInntektskategorier(Set.of())
            .medDekningsgrad(Dekningsgrad.DEKNINGSGRAD_100)
            .medErForelder1(true)
            .medAntallDagerFeriepenger(64) // SVP-spesifikt
            .build();

        var regel = new RegelBeregnFeriepenger();
        var evaluation = regel.evaluer(regelModell);
        var sporing = EvaluationSerializer.asJson(evaluation);

        assertThat(sporing).isNotNull();
        assertThat(regelModell.getFeriepengerPeriode().getFomDato()).isEqualTo(LocalDate.of(2018, 3, 10));
        assertThat(regelModell.getFeriepengerPeriode().getTomDato()).isEqualTo(LocalDate.of(2018, 6, 7));

        regelModell.getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList()
            .forEach(andel -> assertThat(andel.getBeregningsresultatFeriepengerPrÅrListe()).isEmpty());
        regelModell.getBeregningsresultatPerioder().get(1).getBeregningsresultatAndelList()
            .stream().filter(p -> Inntektskategori.ARBEIDSTAKER.equals(p.getInntektskategori()))
            .forEach(andel -> assertThat(andel.getBeregningsresultatFeriepengerPrÅrListe()).hasSize(1));

        assertThat(periode1.getBeregningsresultatAndelList().get(0).getInntektskategori()).isEqualTo(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE);
        assertThat(periode1.getBeregningsresultatAndelList().get(0).getBeregningsresultatFeriepengerPrÅrListe()).isEmpty();

        assertThat(periode2.getBeregningsresultatAndelList().stream().filter(a -> Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE.equals(a.getInntektskategori())).findFirst())
            .hasValueSatisfying(a -> assertThat(a.getBeregningsresultatFeriepengerPrÅrListe()).isEmpty());
        assertThat(periode2.getBeregningsresultatAndelList().stream().filter(a -> Inntektskategori.ARBEIDSTAKER.equals(a.getInntektskategori()) && a.erBrukerMottaker()).findFirst())
            .hasValueSatisfying(a -> assertThat(a.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(4080)));
        assertThat(periode2.getBeregningsresultatAndelList().stream().filter(a -> Inntektskategori.ARBEIDSTAKER.equals(a.getInntektskategori()) && !a.erBrukerMottaker()).findFirst())
            .hasValueSatisfying(a -> assertThat(a.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(1632)));

    }

    private BeregningsresultatPeriode byggBRPeriode(LocalDate fom, LocalDate tom) {
        return BeregningsresultatPeriode.builder()
                .medPeriode(new LocalDateInterval(fom, tom))
                .build();
    }

    private void byggAndelerForPeriode(BeregningsresultatPeriode periode, int dagsats, int refusjon, Arbeidsforhold arbeidsforhold1) {
        BeregningsresultatAndel.builder()
                .medDagsats((long) dagsats)
                .medDagsatsFraBg((long) dagsats)
                .medAktivitetStatus(AktivitetStatus.ATFL)
                .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
                .medBrukerErMottaker(true)
                .medArbeidsforhold(arbeidsforhold1)
                .build(periode);
        if (refusjon > 0) {
            BeregningsresultatAndel.builder()
                    .medDagsats((long) refusjon)
                    .medDagsatsFraBg((long) refusjon)
                    .medAktivitetStatus(AktivitetStatus.ATFL)
                    .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
                    .medBrukerErMottaker(false)
                    .medArbeidsforhold(arbeidsforhold1)
                    .build(periode);
        }
    }

    private void byggSNAndelForPeriode(BeregningsresultatPeriode periode, int dagsats) {
        BeregningsresultatAndel.builder()
            .medDagsats((long) dagsats)
            .medDagsatsFraBg((long) dagsats)
            .medAktivitetStatus(AktivitetStatus.SN)
            .medInntektskategori(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE)
            .medBrukerErMottaker(true)
            .build(periode);
    }

}

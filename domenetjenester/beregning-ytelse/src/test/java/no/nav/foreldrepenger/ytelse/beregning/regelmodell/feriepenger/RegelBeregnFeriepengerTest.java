package no.nav.foreldrepenger.ytelse.beregning.regelmodell.feriepenger;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatAndel;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatFeriepengerGrunnlag;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatRegler;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Dekningsgrad;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori;

class RegelBeregnFeriepengerTest {

    private Arbeidsforhold arbeidsforhold1 = Arbeidsforhold.nyttArbeidsforholdHosVirksomhet("123456789");
    private Arbeidsforhold arbeidsforhold2 = Arbeidsforhold.nyttArbeidsforholdHosVirksomhet("987654321");

    // Eksempler tatt fra
    // https://confluence.adeo.no/display/MODNAV/27c+Beregn+feriepenger+PK-51965+OMR-49

    // Eksempel 1 Mor
    @Test
    void skalBeregneFeriepengerForMor() {
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
        var regelModell = BeregningsresultatFeriepengerGrunnlag.builder()
                .medBeregningsresultatPerioder(List.of(periode1, periode2))
                .medAnnenPartsBeregningsresultatPerioder(annenPartsBeregningsresultatPerioder)
                .medInntektskategorier(Collections.singleton(Inntektskategori.ARBEIDSTAKER))
                .medArbeidstakerVedSkjæringstidspunkt(true)
                .medAnnenPartsInntektskategorier(Collections.singleton(Inntektskategori.ARBEIDSTAKER))
                .medDekningsgrad(Dekningsgrad.DEKNINGSGRAD_100)
                .medErForelder1(true)
                .medAntallDagerFeriepenger(60)
                .build();

        // Act
        var resultat = BeregningsresultatRegler.fastsettFeriepenger(regelModell);

        assertThat(resultat.regelSporing()).isNotNull();
        assertThat(resultat.resultat().feriepengerPeriode().getFomDato()).isEqualTo(LocalDate.of(2018, 1, 6));
        assertThat(resultat.resultat().feriepengerPeriode().getTomDato()).isEqualTo(LocalDate.of(2018, 3, 23));

        assertThat(resultat.resultat().beregningsresultatFeriepengerPrÅrListe()).hasSize(4);
        var tilBruker = resultat.resultat().beregningsresultatFeriepengerPrÅrListe().stream().filter(f -> f.erBrukerMottaker()).toList();
        var tilArbeidsgiver = resultat.resultat().beregningsresultatFeriepengerPrÅrListe().stream().filter(f -> !f.erBrukerMottaker()).toList();

        assertThat(tilBruker).satisfiesOnlyOnce(v -> assertThat(v.getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(1606.5 + 76.5)));
        assertThat(tilBruker).satisfiesOnlyOnce(v -> assertThat(v.getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(459)));
        assertThat(tilArbeidsgiver).satisfiesOnlyOnce(v -> assertThat(v.getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(2754 + 204)));
        assertThat(tilArbeidsgiver).satisfiesOnlyOnce(v -> assertThat(v.getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(2295)));
    }

    // Eksempel 1X Mor med avslag i første periode
    @Test
    void skalBeregneFeriepengerForMorMedAvslagIFørstePeriode() {
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
        var regelModell = BeregningsresultatFeriepengerGrunnlag.builder()
            .medBeregningsresultatPerioder(List.of(periode0, periode1, periode2))
            .medAnnenPartsBeregningsresultatPerioder(annenPartsBeregningsresultatPerioder)
            .medInntektskategorier(Collections.singleton(Inntektskategori.ARBEIDSTAKER))
            .medArbeidstakerVedSkjæringstidspunkt(true)
            .medAnnenPartsInntektskategorier(Collections.singleton(Inntektskategori.ARBEIDSTAKER))
            .medDekningsgrad(Dekningsgrad.DEKNINGSGRAD_100)
            .medErForelder1(true)
            .medAntallDagerFeriepenger(60)
            .build();

        // Act
        var resultat = BeregningsresultatRegler.fastsettFeriepenger(regelModell);

        assertThat(resultat.regelSporing()).isNotNull();
        assertThat(resultat.resultat().feriepengerPeriode().getFomDato()).isEqualTo(LocalDate.of(2018, 1, 6));
        assertThat(resultat.resultat().feriepengerPeriode().getTomDato()).isEqualTo(LocalDate.of(2018, 3, 23));

        assertThat(resultat.resultat().beregningsresultatFeriepengerPrÅrListe()).hasSize(4);
        var tilBruker = resultat.resultat().beregningsresultatFeriepengerPrÅrListe().stream().filter(f -> f.erBrukerMottaker()).toList();
        var tilArbeidsgiver = resultat.resultat().beregningsresultatFeriepengerPrÅrListe().stream().filter(f -> !f.erBrukerMottaker()).toList();

        assertThat(tilBruker).satisfiesOnlyOnce(v -> assertThat(v.getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(1606.5 + 76.5)));
        assertThat(tilBruker).satisfiesOnlyOnce(v -> assertThat(v.getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(459)));
        assertThat(tilArbeidsgiver).satisfiesOnlyOnce(v -> assertThat(v.getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(2754 + 204)));
        assertThat(tilArbeidsgiver).satisfiesOnlyOnce(v -> assertThat(v.getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(2295)));
    }

    // Eksempel 1 Far
    @Test
    void skalBeregneFeriepengerForFar() {
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

        var regelModell = BeregningsresultatFeriepengerGrunnlag.builder()
            .medBeregningsresultatPerioder(List.of(periode1, periode2))
            .medAnnenPartsBeregningsresultatPerioder(annenPartsBeregningsresultatPerioder)
            .medInntektskategorier(Collections.singleton(Inntektskategori.ARBEIDSTAKER))
            .medArbeidstakerVedSkjæringstidspunkt(true)
            .medAnnenPartsInntektskategorier(Collections.singleton(Inntektskategori.ARBEIDSTAKER))
            .medDekningsgrad(Dekningsgrad.DEKNINGSGRAD_100)
            .medErForelder1(false)
            .medAntallDagerFeriepenger(60)
            .build();

        // Act
        var resultat = BeregningsresultatRegler.fastsettFeriepenger(regelModell);

        assertThat(resultat.regelSporing()).isNotNull();
        assertThat(resultat.resultat().feriepengerPeriode().getFomDato()).isEqualTo(LocalDate.of(2018, 1, 6));
        assertThat(resultat.resultat().feriepengerPeriode().getTomDato()).isEqualTo(LocalDate.of(2018, 3, 23));

        assertThat(resultat.resultat().beregningsresultatFeriepengerPrÅrListe()).hasSize(2);
        var tilBruker = resultat.resultat().beregningsresultatFeriepengerPrÅrListe().stream().filter(f -> f.erBrukerMottaker()).toList();
        var tilArbeidsgiver = resultat.resultat().beregningsresultatFeriepengerPrÅrListe().stream().filter(f -> !f.erBrukerMottaker()).toList();

        assertThat(tilBruker).satisfiesOnlyOnce(v -> assertThat(v.getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(234.5 + 76.5)));
        assertThat(tilArbeidsgiver).satisfiesOnlyOnce(v -> assertThat(v.getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(510 + 204)));
    }


    // Eksempel 2 Mor
    @Test
    void skalBeregneFeriepengerForMorEksempel2() {
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

        var regelModell = BeregningsresultatFeriepengerGrunnlag.builder()
            .medBeregningsresultatPerioder(List.of(periode1, periode2, periode3))
            .medAnnenPartsBeregningsresultatPerioder(annenPartsBeregningsresultatPerioder)
            .medInntektskategorier(Collections.singleton(Inntektskategori.ARBEIDSTAKER))
            .medArbeidstakerVedSkjæringstidspunkt(true)
            .medAnnenPartsInntektskategorier(Collections.singleton(Inntektskategori.ARBEIDSTAKER))
            .medDekningsgrad(Dekningsgrad.DEKNINGSGRAD_100)
            .medErForelder1(true)
            .medAntallDagerFeriepenger(60)
            .build();

        // Act
        var resultat = BeregningsresultatRegler.fastsettFeriepenger(regelModell);

        assertThat(resultat.regelSporing()).isNotNull();
        assertThat(resultat.resultat().feriepengerPeriode().getFomDato()).isEqualTo(LocalDate.of(2018, 1, 17));
        assertThat(resultat.resultat().feriepengerPeriode().getTomDato()).isEqualTo(LocalDate.of(2018, 4, 4));

        assertThat(resultat.resultat().beregningsresultatFeriepengerPrÅrListe()).hasSize(4);
        var tilBruker = resultat.resultat().beregningsresultatFeriepengerPrÅrListe().stream().filter(f -> f.erBrukerMottaker()).toList();
        var tilArbeidsgiver = resultat.resultat().beregningsresultatFeriepengerPrÅrListe().stream().filter(f -> !f.erBrukerMottaker()).toList();

        assertThat(tilBruker).satisfiesOnlyOnce(v -> assertThat(v.getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(1606.5 + 178.5)));
        assertThat(tilBruker).satisfiesOnlyOnce(v -> assertThat(v.getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(459)));
        assertThat(tilArbeidsgiver).satisfiesOnlyOnce(v -> assertThat(v.getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(2754 + 306)));
        assertThat(tilArbeidsgiver).satisfiesOnlyOnce(v -> assertThat(v.getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(2295)));
    }

    // Eksempel 2 Far
    @Test
    void skalBeregneFeriepengerForFarEksempel2() {
        var periode1 = byggBRPeriode(LocalDate.of(2018, 3, 21), LocalDate.of(2018, 4, 15));
        byggAndelerForPeriode(periode1, 150, 400, arbeidsforhold1);

        var periode1annenPart = byggBRPeriode(LocalDate.of(2018, 1, 17), LocalDate.of(2018, 3, 20));
        var periode2annenPart = byggBRPeriode(LocalDate.of(2018, 3, 21), LocalDate.of(2018, 3, 28));
        var periode3annenPart = byggBRPeriode(LocalDate.of(2018, 3, 29), LocalDate.of(2018, 4, 8));
        byggAndelerForPeriode(periode1annenPart, 500, 0, arbeidsforhold1);
        byggAndelerForPeriode(periode2annenPart, 0, 0, arbeidsforhold1);
        byggAndelerForPeriode(periode3annenPart, 500, 0, arbeidsforhold1);
        var annenPartsBeregningsresultatPerioder = List.of(periode1annenPart, periode2annenPart, periode3annenPart);

        var regelModell = BeregningsresultatFeriepengerGrunnlag.builder()
            .medBeregningsresultatPerioder(List.of(periode1))
            .medAnnenPartsBeregningsresultatPerioder(annenPartsBeregningsresultatPerioder)
            .medInntektskategorier(Collections.singleton(Inntektskategori.ARBEIDSTAKER))
            .medArbeidstakerVedSkjæringstidspunkt(true)
            .medAnnenPartsInntektskategorier(Collections.singleton(Inntektskategori.ARBEIDSTAKER))
            .medDekningsgrad(Dekningsgrad.DEKNINGSGRAD_100)
            .medErForelder1(false)
            .medAntallDagerFeriepenger(60)
            .build();

        // Act
        var resultat = BeregningsresultatRegler.fastsettFeriepenger(regelModell);

        assertThat(resultat.regelSporing()).isNotNull();
        assertThat(resultat.resultat().feriepengerPeriode().getFomDato()).isEqualTo(LocalDate.of(2018, 1, 17));
        assertThat(resultat.resultat().feriepengerPeriode().getTomDato()).isEqualTo(LocalDate.of(2018, 4, 3));

        assertThat(resultat.resultat().beregningsresultatFeriepengerPrÅrListe()).hasSize(2);
        var tilBruker = resultat.resultat().beregningsresultatFeriepengerPrÅrListe().stream().filter(f -> f.erBrukerMottaker()).toList();
        var tilArbeidsgiver = resultat.resultat().beregningsresultatFeriepengerPrÅrListe().stream().filter(f -> !f.erBrukerMottaker()).toList();

        assertThat(tilBruker).satisfiesOnlyOnce(v -> assertThat(v.getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(153)));
        assertThat(tilArbeidsgiver).satisfiesOnlyOnce(v -> assertThat(v.getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(408)));
    }

    @Test
    void skalBeregneFeriepengerOverFlereÅr() {
        var periode1 = byggBRPeriode(LocalDate.of(2018, 11, 1), LocalDate.of(2019, 1, 5)); // 47 ukedager
        var periode2 = byggBRPeriode(LocalDate.of(2019, 1, 6), LocalDate.of(2019, 2, 5)); // 22 ukedager
        var periode3 = byggBRPeriode(LocalDate.of(2019, 2, 6), LocalDate.of(2019, 4, 16)); // 50 ukedager
        byggAndelerForPeriode(periode1, 1000, 0, arbeidsforhold1);
        byggAndelerForPeriode(periode2, 0, 0, arbeidsforhold1);
        byggAndelerForPeriode(periode3, 500, 500, arbeidsforhold1);

        var periode1annenPart = byggBRPeriode(LocalDate.of(2019, 1, 25), LocalDate.of(2019, 2, 15));
        byggAndelerForPeriode(periode1annenPart, 500, 0, arbeidsforhold1);
        var annenPartsBeregningsresultatPerioder = List.of(periode1annenPart);

        var regelModell = BeregningsresultatFeriepengerGrunnlag.builder()
            .medBeregningsresultatPerioder(List.of(periode1, periode2, periode3))
            .medAnnenPartsBeregningsresultatPerioder(annenPartsBeregningsresultatPerioder)
            .medInntektskategorier(Collections.singleton(Inntektskategori.ARBEIDSTAKER))
            .medArbeidstakerVedSkjæringstidspunkt(true)
            .medAnnenPartsInntektskategorier(Collections.singleton(Inntektskategori.SJØMANN))
            .medDekningsgrad(Dekningsgrad.DEKNINGSGRAD_80)
            .medErForelder1(true)
            .medAntallDagerFeriepenger(60)
            .build();

        // Act
        var resultat = BeregningsresultatRegler.fastsettFeriepenger(regelModell);

        assertThat(resultat.regelSporing()).isNotNull();
        assertThat(resultat.resultat().feriepengerPeriode().getFomDato()).isEqualTo(LocalDate.of(2018, 11, 1));
        assertThat(resultat.resultat().feriepengerPeriode().getTomDato()).isEqualTo(LocalDate.of(2019, 2, 21));

        assertThat(resultat.resultat().beregningsresultatFeriepengerPrÅrListe()).hasSize(3);
        var tilBruker = resultat.resultat().beregningsresultatFeriepengerPrÅrListe().stream().filter(f -> f.erBrukerMottaker()).toList();
        var tilArbeidsgiver = resultat.resultat().beregningsresultatFeriepengerPrÅrListe().stream().filter(f -> !f.erBrukerMottaker()).toList();

        assertThat(tilBruker).satisfiesOnlyOnce(v -> assertThat(v.getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(4386))); // 43d a 1000
        assertThat(tilBruker).satisfiesOnlyOnce(v -> assertThat(v.getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(1020))); // 4d a 1000 + 12d a 500
        assertThat(tilArbeidsgiver).satisfiesOnlyOnce(v -> assertThat(v.getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(612))); // 12d a 500
    }

    @Test
    void skalBeregneFeriepengerUtenAnnenpart() {
        var periode1 = byggBRPeriode(LocalDate.of(2018, 11, 1), LocalDate.of(2018, 12, 31)); // 43 ukedager
        byggAndelerForPeriode(periode1, 1000, 0, arbeidsforhold1);

        var regelModell = BeregningsresultatFeriepengerGrunnlag.builder()
            .medBeregningsresultatPerioder(List.of(periode1))
            .medAnnenPartsBeregningsresultatPerioder(Collections.emptyList())
            .medInntektskategorier(Collections.singleton(Inntektskategori.ARBEIDSTAKER))
            .medArbeidstakerVedSkjæringstidspunkt(true)
            .medAnnenPartsInntektskategorier(Collections.emptySet())
            .medDekningsgrad(Dekningsgrad.DEKNINGSGRAD_100)
            .medErForelder1(true)
            .medAntallDagerFeriepenger(60)
            .build();

        // Act
        var resultat = BeregningsresultatRegler.fastsettFeriepenger(regelModell);

        assertThat(resultat.regelSporing()).isNotNull();
        assertThat(resultat.resultat().feriepengerPeriode().getFomDato()).isEqualTo(LocalDate.of(2018, 11, 1));
        assertThat(resultat.resultat().feriepengerPeriode().getTomDato()).isEqualTo(LocalDate.of(2018, 12, 31));

        assertThat(resultat.resultat().beregningsresultatFeriepengerPrÅrListe()).hasSize(1);
        var tilBruker = resultat.resultat().beregningsresultatFeriepengerPrÅrListe().stream().filter(f -> f.erBrukerMottaker()).toList();
        var tilArbeidsgiver = resultat.resultat().beregningsresultatFeriepengerPrÅrListe().stream().filter(f -> !f.erBrukerMottaker()).toList();

        assertThat(tilBruker).satisfiesOnlyOnce(v -> assertThat(v.getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(4386)));
        assertThat(tilArbeidsgiver).isEmpty();
    }

    @Test
    void skalBeregneFeriepengerPeriodeForSvangerskapspenger() {
        var periode1 = byggBRPeriode(LocalDate.of(2018, 1, 6), LocalDate.of(2018, 3, 9));
        var periode2 = byggBRPeriode(LocalDate.of(2018, 3, 10), LocalDate.of(2018, 5, 1));
        byggAndelerForPeriode(periode1, 700, 500, arbeidsforhold1);
        byggAndelerForPeriode(periode2, 300, 200, arbeidsforhold1);

        var regelModell = BeregningsresultatFeriepengerGrunnlag.builder()
            .medBeregningsresultatPerioder(List.of(periode1, periode2))
            .medAnnenPartsBeregningsresultatPerioder(List.of())
            .medInntektskategorier(Set.of(Inntektskategori.ARBEIDSTAKER))
            .medArbeidstakerVedSkjæringstidspunkt(true)
            .medAnnenPartsInntektskategorier(Set.of())
            .medDekningsgrad(Dekningsgrad.DEKNINGSGRAD_100)
            .medErForelder1(true)
            .medAntallDagerFeriepenger(64) // SVP-spesifikt
            .build();

        // Act
        var resultat = BeregningsresultatRegler.fastsettFeriepenger(regelModell);

        assertThat(resultat.regelSporing()).isNotNull();
        assertThat(resultat.resultat().feriepengerPeriode().getFomDato()).isEqualTo(LocalDate.of(2018, 1, 6));
        assertThat(resultat.resultat().feriepengerPeriode().getTomDato()).isEqualTo(LocalDate.of(2018, 4, 5));

        assertThat(resultat.resultat().beregningsresultatFeriepengerPrÅrListe()).hasSize(2);
        var tilBruker = resultat.resultat().beregningsresultatFeriepengerPrÅrListe().stream().filter(f -> f.erBrukerMottaker()).toList();
        var tilArbeidsgiver = resultat.resultat().beregningsresultatFeriepengerPrÅrListe().stream().filter(f -> !f.erBrukerMottaker()).toList();

        assertThat(tilBruker).satisfiesOnlyOnce(v -> assertThat(v.getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(3794))); // 45d a 700, 19d a 300
        assertThat(tilArbeidsgiver).satisfiesOnlyOnce(v -> assertThat(v.getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(2683))); // 45d a 500, 19d a 200
    }

    @Test
    void skalIkkeBeregneFeriepengerPeriodeForSvangerskapspengerSN() {
        var periode1 = byggBRPeriode(LocalDate.of(2018, 1, 6), LocalDate.of(2018, 3, 9));
        var periode2 = byggBRPeriode(LocalDate.of(2018, 3, 10), LocalDate.of(2018, 5, 1));
        byggSNAndelForPeriode(periode1, 700);
        byggSNAndelForPeriode(periode2, 300);

        var regelModell = BeregningsresultatFeriepengerGrunnlag.builder()
            .medBeregningsresultatPerioder(List.of(periode1, periode2))
            .medAnnenPartsBeregningsresultatPerioder(List.of())
            .medInntektskategorier(Set.of(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE))
            .medArbeidstakerVedSkjæringstidspunkt(false)
            .medAnnenPartsInntektskategorier(Set.of())
            .medDekningsgrad(Dekningsgrad.DEKNINGSGRAD_100)
            .medErForelder1(true)
            .medAntallDagerFeriepenger(64) // SVP-spesifikt
            .build();

        // Act
        var resultat = BeregningsresultatRegler.fastsettFeriepenger(regelModell);

        assertThat(resultat.regelSporing()).isNotNull();
        assertThat(resultat.resultat().feriepengerPeriode()).isNull();

        assertThat(resultat.resultat().beregningsresultatFeriepengerPrÅrListe()).isEmpty();
    }

    @Test
    void skalIkkeBeregneFeriepengerPeriodeForSvangerskapspengerSNTilkommetAT() {
        var periode1 = byggBRPeriode(LocalDate.of(2018, 1, 6), LocalDate.of(2018, 3, 9));
        var periode2 = byggBRPeriode(LocalDate.of(2018, 3, 10), LocalDate.of(2018, 7, 1));
        byggSNAndelForPeriode(periode1, 700);
        byggSNAndelForPeriode(periode2, 300);
        byggAndelerForPeriode(periode2, 625, 250, arbeidsforhold1);

        var regelModell = BeregningsresultatFeriepengerGrunnlag.builder()
            .medBeregningsresultatPerioder(List.of(periode1, periode2))
            .medAnnenPartsBeregningsresultatPerioder(List.of())
            .medInntektskategorier(Set.of(Inntektskategori.ARBEIDSTAKER, Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE))
            .medArbeidstakerVedSkjæringstidspunkt(false)
            .medAnnenPartsInntektskategorier(Set.of())
            .medDekningsgrad(Dekningsgrad.DEKNINGSGRAD_100)
            .medErForelder1(true)
            .medAntallDagerFeriepenger(64) // SVP-spesifikt
            .build();

        // Act
        var resultat = BeregningsresultatRegler.fastsettFeriepenger(regelModell);

        assertThat(resultat.regelSporing()).isNotNull();
        assertThat(resultat.resultat().feriepengerPeriode()).isNull();

        assertThat(resultat.resultat().beregningsresultatFeriepengerPrÅrListe()).isEmpty();
    }

    // Skal beregne feriepenger fom tilkommet AT-andel
    @Test
    void skalBeregneFeriepengerPeriodeForSvangerskapspengerATSNmedSNførAT() {
        var periode1 = byggBRPeriode(LocalDate.of(2018, 1, 6), LocalDate.of(2018, 3, 9));
        var periode2 = byggBRPeriode(LocalDate.of(2018, 3, 10), LocalDate.of(2018, 7, 1));
        byggSNAndelForPeriode(periode1, 700);
        byggSNAndelForPeriode(periode2, 300);
        byggAndelerForPeriode(periode2, 625, 250, arbeidsforhold1);

        var regelModell = BeregningsresultatFeriepengerGrunnlag.builder()
            .medBeregningsresultatPerioder(List.of(periode1, periode2))
            .medAnnenPartsBeregningsresultatPerioder(List.of())
            .medInntektskategorier(Set.of(Inntektskategori.ARBEIDSTAKER, Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE))
            .medArbeidstakerVedSkjæringstidspunkt(true)
            .medAnnenPartsInntektskategorier(Set.of())
            .medDekningsgrad(Dekningsgrad.DEKNINGSGRAD_100)
            .medErForelder1(true)
            .medAntallDagerFeriepenger(64) // SVP-spesifikt
            .build();

        // Act
        var resultat = BeregningsresultatRegler.fastsettFeriepenger(regelModell);

        assertThat(resultat.regelSporing()).isNotNull();
        assertThat(resultat.resultat().feriepengerPeriode().getFomDato()).isEqualTo(LocalDate.of(2018, 3, 10));
        assertThat(resultat.resultat().feriepengerPeriode().getTomDato()).isEqualTo(LocalDate.of(2018, 6, 7));

        assertThat(resultat.resultat().beregningsresultatFeriepengerPrÅrListe()).hasSize(2);
        var tilBruker = resultat.resultat().beregningsresultatFeriepengerPrÅrListe().stream().filter(f -> f.erBrukerMottaker()).toList();
        var tilArbeidsgiver = resultat.resultat().beregningsresultatFeriepengerPrÅrListe().stream().filter(f -> !f.erBrukerMottaker()).toList();

        assertThat(tilBruker).satisfiesOnlyOnce(v -> assertThat(v.getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(4080)));
        assertThat(tilArbeidsgiver).satisfiesOnlyOnce(v -> assertThat(v.getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(1632)));
    }


    private BeregningsresultatPeriode byggBRPeriode(LocalDate fom, LocalDate tom) {
        return new BeregningsresultatPeriode(fom, tom);
    }

    private void byggAndelerForPeriode(BeregningsresultatPeriode periode, int dagsats, int refusjon, Arbeidsforhold arbeidsforhold1) {
        periode.addBeregningsresultatAndel(BeregningsresultatAndel.builder()
                .medDagsats((long) dagsats)
                .medDagsatsFraBg((long) dagsats)
                .medAktivitetStatus(AktivitetStatus.ATFL)
                .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
                .medBrukerErMottaker(true)
                .medArbeidsforhold(arbeidsforhold1)
                .build());
        if (refusjon > 0) {
            periode.addBeregningsresultatAndel(BeregningsresultatAndel.builder()
                    .medDagsats((long) refusjon)
                    .medDagsatsFraBg((long) refusjon)
                    .medAktivitetStatus(AktivitetStatus.ATFL)
                    .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
                    .medBrukerErMottaker(false)
                    .medArbeidsforhold(arbeidsforhold1)
                    .build());
        }
    }

    private void byggSNAndelForPeriode(BeregningsresultatPeriode periode, int dagsats) {
        periode.addBeregningsresultatAndel(BeregningsresultatAndel.builder()
            .medDagsats((long) dagsats)
            .medDagsatsFraBg((long) dagsats)
            .medAktivitetStatus(AktivitetStatus.SN)
            .medInntektskategori(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE)
            .medBrukerErMottaker(true)
            .build());
    }

}

package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.domene.modell.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.modell.Inntektskategori;
import no.nav.foreldrepenger.domene.modell.PeriodeÅrsak;
import no.nav.foreldrepenger.domene.modell.Sammenligningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.SammenligningsgrunnlagPrStatus;
import no.nav.foreldrepenger.domene.modell.SammenligningsgrunnlagType;

public class BeregningsgrunnlagDiffSjekkerTest {

    public static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    private static final Arbeidsgiver ARBEIDSGIVER1 = Arbeidsgiver.fra(new Virksomhet.Builder().medOrgnr("238201321").build());
    private static final Arbeidsgiver ARBEIDSGIVER2 = Arbeidsgiver.fra(new Virksomhet.Builder().medOrgnr("490830958").build());


    @Test
    public void skalReturnereTrueOmUlikeGrunnbeløp() {
        // Arrange
        BeregningsgrunnlagEntitet aktivt = BeregningsgrunnlagEntitet.ny()
            .medGrunnbeløp(BigDecimal.TEN)
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.FRILANSER))
            .medSkjæringstidspunkt(LocalDate.now())
            .build();

        BeregningsgrunnlagEntitet forrige = BeregningsgrunnlagEntitet.ny()
            .medGrunnbeløp(BigDecimal.ONE)
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.FRILANSER))
            .medSkjæringstidspunkt(LocalDate.now())
            .build();

        // Act

        boolean harDiff = BeregningsgrunnlagDiffSjekker.harSignifikantDiffIBeregningsgrunnlag(aktivt, forrige);

        // Assert
        assertThat(harDiff).isTrue();
    }

    @Test
    public void skalReturnereTrueOmUlikeAktivitetstatuser() {
        // Arrange
        BeregningsgrunnlagEntitet aktivt = BeregningsgrunnlagEntitet.ny()
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.FRILANSER))
            .medSkjæringstidspunkt(LocalDate.now())
            .build();

        BeregningsgrunnlagEntitet forrige = BeregningsgrunnlagEntitet.ny()
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE))
            .medSkjæringstidspunkt(LocalDate.now())
            .build();

        // Act

        boolean harDiff = BeregningsgrunnlagDiffSjekker.harSignifikantDiffIBeregningsgrunnlag(aktivt, forrige);

        // Assert
        assertThat(harDiff).isTrue();
    }

    @Test
    public void skalReturnereTrueOmUliktAntallPerioder() {
        // Arrange
        BeregningsgrunnlagEntitet aktivt = BeregningsgrunnlagEntitet.ny()
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER))
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .build();

        BeregningsgrunnlagPeriode.ny()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null)
            .build(aktivt);

        BeregningsgrunnlagEntitet forrige = BeregningsgrunnlagEntitet.ny()
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER))
            .medSkjæringstidspunkt(LocalDate.now())
            .build();

        BeregningsgrunnlagPeriode.ny()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(1))
            .build(forrige);

        BeregningsgrunnlagPeriode.ny()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT.plusMonths(1).plusDays(1), null)
            .leggTilPeriodeÅrsak(PeriodeÅrsak.NATURALYTELSE_BORTFALT)
            .build(forrige);

        // Act
        boolean harDiff = BeregningsgrunnlagDiffSjekker.harSignifikantDiffIBeregningsgrunnlag(aktivt, forrige);

        // Assert
        assertThat(harDiff).isTrue();
    }

    @Test
    public void skalReturnereTrueOmUliktAntallAndeler() {
        // Arrange
        BeregningsgrunnlagEntitet aktivt = BeregningsgrunnlagEntitet.ny()
            .medGrunnbeløp(BigDecimal.TEN)
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE))
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .build();

        BeregningsgrunnlagPeriode aktivPeriode = BeregningsgrunnlagPeriode.ny()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null)
            .build(aktivt);

        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medInntektskategori(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE)
            .medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
            .build(aktivPeriode);

        BeregningsgrunnlagEntitet forrige = BeregningsgrunnlagEntitet.ny()
            .medGrunnbeløp(BigDecimal.TEN)
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE))
            .medSkjæringstidspunkt(LocalDate.now())
            .build();

        BeregningsgrunnlagPeriode forrigePeriode = BeregningsgrunnlagPeriode.ny()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null)
            .build(forrige);

        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medInntektskategori(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE)
            .medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
            .build(forrigePeriode);

        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medInntektskategori(Inntektskategori.FRILANSER)
            .medAktivitetStatus(AktivitetStatus.FRILANSER)
            .build(forrigePeriode);

        // Act
        boolean harDiff = BeregningsgrunnlagDiffSjekker.harSignifikantDiffIBeregningsgrunnlag(aktivt, forrige);

        // Assert
        assertThat(harDiff).isTrue();
    }

    @Test
    public void skalReturnereTrueOmUlikAktivitetstatusPåAndelMedSammeAndelsnr() {
        // Arrange
        BeregningsgrunnlagEntitet aktivt = BeregningsgrunnlagEntitet.ny()
            .medGrunnbeløp(BigDecimal.TEN)
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE))
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .build();

        BeregningsgrunnlagPeriode aktivPeriode = BeregningsgrunnlagPeriode.ny()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null)
            .build(aktivt);

        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medInntektskategori(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE)
            .medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
            .medAndelsnr(1L)
            .build(aktivPeriode);

        BeregningsgrunnlagEntitet forrige = BeregningsgrunnlagEntitet.ny()
            .medGrunnbeløp(BigDecimal.TEN)
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE))
            .medSkjæringstidspunkt(LocalDate.now())
            .build();

        BeregningsgrunnlagPeriode forrigePeriode = BeregningsgrunnlagPeriode.ny()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null)
            .build(forrige);

        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medInntektskategori(Inntektskategori.FRILANSER)
            .medAktivitetStatus(AktivitetStatus.FRILANSER)
            .medAndelsnr(1L)
            .build(forrigePeriode);

        // Act
        boolean harDiff = BeregningsgrunnlagDiffSjekker.harSignifikantDiffIBeregningsgrunnlag(aktivt, forrige);

        // Assert
        assertThat(harDiff).isTrue();
    }


    @Test
    public void skalReturnereTrueOmUlikArbeidsgiverPåAndelMedSammeAndelsnr() {
        // Arrange
        BeregningsgrunnlagEntitet aktivt = BeregningsgrunnlagEntitet.ny()
            .medGrunnbeløp(BigDecimal.TEN)
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER))
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .build();

        BeregningsgrunnlagPeriode aktivPeriode = BeregningsgrunnlagPeriode.ny()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null)
            .build(aktivt);

        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(ARBEIDSGIVER2))
            .medAndelsnr(1L)
            .build(aktivPeriode);

        BeregningsgrunnlagEntitet forrige = BeregningsgrunnlagEntitet.ny()
            .medGrunnbeløp(BigDecimal.TEN)
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER))
            .medSkjæringstidspunkt(LocalDate.now())
            .build();

        BeregningsgrunnlagPeriode forrigePeriode = BeregningsgrunnlagPeriode.ny()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null)
            .build(forrige);

        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(ARBEIDSGIVER1))
            .medAndelsnr(1L)
            .build(forrigePeriode);

        // Act
        boolean harDiff = BeregningsgrunnlagDiffSjekker.harSignifikantDiffIBeregningsgrunnlag(aktivt, forrige);

        // Assert
        assertThat(harDiff).isTrue();
    }

    @Test
    public void skalReturnereFalseArbeidstakerIngenDiff() {
        // Arrange
        BeregningsgrunnlagEntitet aktivt = BeregningsgrunnlagEntitet.ny()
            .medGrunnbeløp(BigDecimal.TEN)
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER))
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .build();

        BeregningsgrunnlagPeriode aktivPeriode = BeregningsgrunnlagPeriode.ny()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null)
            .build(aktivt);

        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(ARBEIDSGIVER1).medArbeidsgiver(ARBEIDSGIVER1))
            .medAndelsnr(1L)
            .build(aktivPeriode);

        BeregningsgrunnlagEntitet forrige = BeregningsgrunnlagEntitet.ny()
            .medGrunnbeløp(BigDecimal.TEN)
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER))
            .medSkjæringstidspunkt(LocalDate.now())
            .build();

        BeregningsgrunnlagPeriode forrigePeriode = BeregningsgrunnlagPeriode.ny()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null)
            .build(forrige);

        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(ARBEIDSGIVER1).medArbeidsgiver(ARBEIDSGIVER1))
            .medAndelsnr(1L)
            .build(forrigePeriode);

        // Act
        boolean harDiff = BeregningsgrunnlagDiffSjekker.harSignifikantDiffIBeregningsgrunnlag(aktivt, forrige);

        // Assert
        assertThat(harDiff).isFalse();
    }

    @Test
    public void skalIkkeGiForskjellPåAndelerHvisBeggeManglerArbeidsforhold() {
        // Arrange
        BeregningsgrunnlagEntitet aktivt = BeregningsgrunnlagEntitet.ny()
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER))
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .build();

        BeregningsgrunnlagPeriode aktivPeriode = BeregningsgrunnlagPeriode.ny()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null)
            .build(aktivt);

        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medArbforholdType(OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE)
            .medAndelsnr(1L)
            .build(aktivPeriode);

        BeregningsgrunnlagEntitet forrige = BeregningsgrunnlagEntitet.ny()
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER))
            .medSkjæringstidspunkt(LocalDate.now())
            .build();

        BeregningsgrunnlagPeriode forrigePeriode = BeregningsgrunnlagPeriode.ny()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(1))
            .build(forrige);

        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medArbforholdType(OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE)
            .medAndelsnr(1L)
            .build(forrigePeriode);

        // Act
        boolean harDiff = BeregningsgrunnlagDiffSjekker.harSignifikantDiffIBeregningsgrunnlag(aktivt, forrige);

        // Assert
        assertThat(harDiff).isFalse();
    }

    @Test
    public void skalGiForskjellPåSammenligningsgrunnlagVedForskjelligAvvik() {
        // Arrange
        BeregningsgrunnlagEntitet aktivt = BeregningsgrunnlagEntitet.ny()
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER))
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .build();

        LocalDate sgTom = LocalDate.now().plusMonths(3);
        LocalDate sgFom = LocalDate.now();

        Sammenligningsgrunnlag.builder()
            .medAvvikPromille(BigDecimal.valueOf(250.0124))
            .medSammenligningsperiode(sgFom, sgTom)
            .medRapportertPrÅr(BigDecimal.valueOf(300_000))
            .build(aktivt);

        BeregningsgrunnlagEntitet forrige = BeregningsgrunnlagEntitet.ny()
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER))
            .medSkjæringstidspunkt(LocalDate.now())
            .build();

        Sammenligningsgrunnlag.builder()
            .medAvvikPromille(BigDecimal.valueOf(250.0123))
            .medSammenligningsperiode(sgFom, sgTom)
            .medRapportertPrÅr(BigDecimal.valueOf(300_000))
            .build(forrige);

        // Act
        boolean harDiff = BeregningsgrunnlagDiffSjekker.harSignifikantDiffIBeregningsgrunnlag(aktivt, forrige);

        // Assert
        assertThat(harDiff).isTrue();
    }

    @Test
    public void skalGiForskjellPåSammenligningsgrunnlagVedForskjelligInntektMenLiktAvvik() {
        // Arrange
        BeregningsgrunnlagEntitet aktivt = BeregningsgrunnlagEntitet.ny()
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER))
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .build();

        LocalDate sgTom = LocalDate.now().plusMonths(3);
        LocalDate sgFom = LocalDate.now();

        Sammenligningsgrunnlag.builder()
            .medAvvikPromille(BigDecimal.valueOf(250.654987))
            .medSammenligningsperiode(sgFom, sgTom)
            .medRapportertPrÅr(BigDecimal.valueOf(300_001))
            .build(aktivt);

        BeregningsgrunnlagEntitet forrige = BeregningsgrunnlagEntitet.ny()
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER))
            .medSkjæringstidspunkt(LocalDate.now())
            .build();

        Sammenligningsgrunnlag.builder()
            .medAvvikPromille(BigDecimal.valueOf(250.654987))
            .medSammenligningsperiode(sgFom, sgTom)
            .medRapportertPrÅr(BigDecimal.valueOf(300_000))
            .build(forrige);

        // Act
        boolean harDiff = BeregningsgrunnlagDiffSjekker.harSignifikantDiffIBeregningsgrunnlag(aktivt, forrige);

        // Assert
        assertThat(harDiff).isTrue();
    }

    @Test
    public void skalIkkeGiForskjellNårSammenligningsgrunnlagPrStatusListeErLik(){
        SammenligningsgrunnlagPrStatus.Builder sammenligningsgrunnlagPrStatusAt = new SammenligningsgrunnlagPrStatus.Builder();
        sammenligningsgrunnlagPrStatusAt.medRapportertPrÅr(BigDecimal.valueOf(100_000));
        sammenligningsgrunnlagPrStatusAt.medAvvikPromille(BigDecimal.ZERO);
        sammenligningsgrunnlagPrStatusAt.medSammenligningsperiode(LocalDate.now().minusYears(1), LocalDate.now());
        sammenligningsgrunnlagPrStatusAt.medSammenligningsgrunnlagType(no.nav.foreldrepenger.domene.modell.SammenligningsgrunnlagType.SAMMENLIGNING_AT);

        SammenligningsgrunnlagPrStatus.Builder sammenligningsgrunnlagPrStatusFl = new SammenligningsgrunnlagPrStatus.Builder();
        sammenligningsgrunnlagPrStatusFl.medRapportertPrÅr(BigDecimal.valueOf(200_000));
        sammenligningsgrunnlagPrStatusFl.medAvvikPromille(BigDecimal.valueOf(250));
        sammenligningsgrunnlagPrStatusFl.medSammenligningsperiode(LocalDate.now().minusYears(1), LocalDate.now());
        sammenligningsgrunnlagPrStatusFl.medSammenligningsgrunnlagType(no.nav.foreldrepenger.domene.modell.SammenligningsgrunnlagType.SAMMENLIGNING_FL);

        BeregningsgrunnlagEntitet beregningsgrunnlagEntitet = BeregningsgrunnlagEntitet.ny()
                .leggTilSammenligningsgrunnlag(sammenligningsgrunnlagPrStatusAt)
                .leggTilSammenligningsgrunnlag(sammenligningsgrunnlagPrStatusFl)
                .medSkjæringstidspunkt(LocalDate.now())
                .build();
        boolean resultat = BeregningsgrunnlagDiffSjekker.harSignifikantDiffIBeregningsgrunnlag(beregningsgrunnlagEntitet, beregningsgrunnlagEntitet);
        assertThat(resultat).isFalse();
    }

    @Test
    public void skalGiForskjellNårSammenligningsgrunnlagPrStatusListeIkkeInneholderSammeTyper(){
        SammenligningsgrunnlagPrStatus.Builder sammenligningsgrunnlagPrStatusAt = new SammenligningsgrunnlagPrStatus.Builder();
        sammenligningsgrunnlagPrStatusAt.medRapportertPrÅr(BigDecimal.valueOf(100_000));
        sammenligningsgrunnlagPrStatusAt.medAvvikPromille(BigDecimal.ZERO);
        sammenligningsgrunnlagPrStatusAt.medSammenligningsperiode(LocalDate.now().minusYears(1), LocalDate.now());
        sammenligningsgrunnlagPrStatusAt.medSammenligningsgrunnlagType(no.nav.foreldrepenger.domene.modell.SammenligningsgrunnlagType.SAMMENLIGNING_AT);

        SammenligningsgrunnlagPrStatus.Builder sammenligningsgrunnlagPrStatusFl = new SammenligningsgrunnlagPrStatus.Builder();
        sammenligningsgrunnlagPrStatusFl.medRapportertPrÅr(BigDecimal.valueOf(200_000));
        sammenligningsgrunnlagPrStatusFl.medAvvikPromille(BigDecimal.valueOf(250));
        sammenligningsgrunnlagPrStatusFl.medSammenligningsperiode(LocalDate.now().minusYears(1), LocalDate.now());
        sammenligningsgrunnlagPrStatusFl.medSammenligningsgrunnlagType(no.nav.foreldrepenger.domene.modell.SammenligningsgrunnlagType.SAMMENLIGNING_FL);

        BeregningsgrunnlagEntitet aktivt = BeregningsgrunnlagEntitet.ny()
                .leggTilSammenligningsgrunnlag(sammenligningsgrunnlagPrStatusAt)
                .medSkjæringstidspunkt(LocalDate.now())
                .build();
        BeregningsgrunnlagEntitet forrige = BeregningsgrunnlagEntitet.ny()
                .leggTilSammenligningsgrunnlag(sammenligningsgrunnlagPrStatusFl)
                .medSkjæringstidspunkt(LocalDate.now())
                .build();
        boolean resultat = BeregningsgrunnlagDiffSjekker.harSignifikantDiffIBeregningsgrunnlag(aktivt, forrige);
        assertThat(resultat).isTrue();
    }

    @Test
    public void skalGiForskjellNårSammenligningsgrunnlagPrStatusListeIkkeHarLikeVerdierForSammeType(){
        BigDecimal avvikPromilleAtAktivt = BigDecimal.ZERO;
        BigDecimal avvikPromilleAtForrige = BigDecimal.valueOf(10);
        SammenligningsgrunnlagPrStatus.Builder sammenligningsgrunnlagPrStatusAtAktivt = new SammenligningsgrunnlagPrStatus.Builder();
        sammenligningsgrunnlagPrStatusAtAktivt.medRapportertPrÅr(BigDecimal.valueOf(100_000));
        sammenligningsgrunnlagPrStatusAtAktivt.medAvvikPromille(avvikPromilleAtAktivt);
        sammenligningsgrunnlagPrStatusAtAktivt.medSammenligningsperiode(LocalDate.now().minusYears(1), LocalDate.now());
        sammenligningsgrunnlagPrStatusAtAktivt.medSammenligningsgrunnlagType(no.nav.foreldrepenger.domene.modell.SammenligningsgrunnlagType.SAMMENLIGNING_AT);

        SammenligningsgrunnlagPrStatus.Builder sammenligningsgrunnlagPrStatusFlAktivt = new SammenligningsgrunnlagPrStatus.Builder();
        sammenligningsgrunnlagPrStatusFlAktivt.medRapportertPrÅr(BigDecimal.valueOf(200_000));
        sammenligningsgrunnlagPrStatusFlAktivt.medAvvikPromille(BigDecimal.valueOf(250));
        sammenligningsgrunnlagPrStatusFlAktivt.medSammenligningsperiode(LocalDate.now().minusYears(1), LocalDate.now());
        sammenligningsgrunnlagPrStatusFlAktivt.medSammenligningsgrunnlagType(no.nav.foreldrepenger.domene.modell.SammenligningsgrunnlagType.SAMMENLIGNING_FL);

        SammenligningsgrunnlagPrStatus.Builder sammenligningsgrunnlagPrStatusFlForrige = new SammenligningsgrunnlagPrStatus.Builder();
        sammenligningsgrunnlagPrStatusFlForrige.medRapportertPrÅr(BigDecimal.valueOf(200_000));
        sammenligningsgrunnlagPrStatusFlForrige.medAvvikPromille(BigDecimal.valueOf(250));
        sammenligningsgrunnlagPrStatusFlForrige.medSammenligningsperiode(LocalDate.now().minusYears(1), LocalDate.now());
        sammenligningsgrunnlagPrStatusFlForrige.medSammenligningsgrunnlagType(no.nav.foreldrepenger.domene.modell.SammenligningsgrunnlagType.SAMMENLIGNING_FL);

        SammenligningsgrunnlagPrStatus.Builder sammenligningsgrunnlagPrStatusAtForrige = new SammenligningsgrunnlagPrStatus.Builder();
        sammenligningsgrunnlagPrStatusAtForrige.medRapportertPrÅr(BigDecimal.valueOf(100_000));
        sammenligningsgrunnlagPrStatusAtForrige.medAvvikPromille(avvikPromilleAtForrige);
        sammenligningsgrunnlagPrStatusAtForrige.medSammenligningsperiode(LocalDate.now().minusYears(1), LocalDate.now());
        sammenligningsgrunnlagPrStatusAtForrige.medSammenligningsgrunnlagType(no.nav.foreldrepenger.domene.modell.SammenligningsgrunnlagType.SAMMENLIGNING_AT);

        BeregningsgrunnlagEntitet aktivt = BeregningsgrunnlagEntitet.ny()
                .leggTilSammenligningsgrunnlag(sammenligningsgrunnlagPrStatusAtAktivt)
                .leggTilSammenligningsgrunnlag(sammenligningsgrunnlagPrStatusFlAktivt)
                .medSkjæringstidspunkt(LocalDate.now())
                .build();
        BeregningsgrunnlagEntitet forrige = BeregningsgrunnlagEntitet.ny()
                .leggTilSammenligningsgrunnlag(sammenligningsgrunnlagPrStatusAtForrige)
                .leggTilSammenligningsgrunnlag(sammenligningsgrunnlagPrStatusFlForrige)
                .medSkjæringstidspunkt(LocalDate.now())
                .build();
        boolean resultat = BeregningsgrunnlagDiffSjekker.harSignifikantDiffIBeregningsgrunnlag(aktivt, forrige);
        assertThat(resultat).isTrue();
    }

    @Test
    public void skalIkkeGiForskjellNårSammenligningsgrunnlagPrStatusListeIkkeErSattForBeggeBeregningsgrunnlag(){
        BeregningsgrunnlagEntitet beregningsgrunnlagEntitet = BeregningsgrunnlagEntitet.ny()
                .medSkjæringstidspunkt(LocalDate.now())
                .build();
        boolean resultat = BeregningsgrunnlagDiffSjekker.harSignifikantDiffIBeregningsgrunnlag(beregningsgrunnlagEntitet, beregningsgrunnlagEntitet);
        assertThat(resultat).isFalse();
    }

    @Test
    public void skalReturnereTrueNårSammenligningsgrunnlagPrStatusListeIkkeErSattForDetEneBeregningsgrunnlaget(){
        SammenligningsgrunnlagPrStatus.Builder sammenligningsgrunnlagPrStatusAt = new SammenligningsgrunnlagPrStatus.Builder();
        sammenligningsgrunnlagPrStatusAt.medRapportertPrÅr(BigDecimal.valueOf(100_000));
        sammenligningsgrunnlagPrStatusAt.medAvvikPromille(BigDecimal.ZERO);
        sammenligningsgrunnlagPrStatusAt.medSammenligningsperiode(LocalDate.now().minusYears(1), LocalDate.now());
        sammenligningsgrunnlagPrStatusAt.medSammenligningsgrunnlagType(SammenligningsgrunnlagType.SAMMENLIGNING_AT);

        BeregningsgrunnlagEntitet aktivt = BeregningsgrunnlagEntitet.ny()
                .leggTilSammenligningsgrunnlag(sammenligningsgrunnlagPrStatusAt)
                .medSkjæringstidspunkt(LocalDate.now())
                .build();
        BeregningsgrunnlagEntitet forrige = BeregningsgrunnlagEntitet.ny()
                .medSkjæringstidspunkt(LocalDate.now())
                .build();
        boolean resultat = BeregningsgrunnlagDiffSjekker.harSignifikantDiffIBeregningsgrunnlag(aktivt, forrige);
        assertThat(resultat).isTrue();
    }

}

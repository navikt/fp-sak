package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.VirksomhetEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.Inntektskategori;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.PeriodeÅrsak;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.Sammenligningsgrunnlag;

public class BeregningsgrunnlagDiffSjekkerTest {

    public static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    private static final Arbeidsgiver ARBEIDSGIVER1 = Arbeidsgiver.fra(new VirksomhetEntitet.Builder().medOrgnr("238201321").build());
    private static final Arbeidsgiver ARBEIDSGIVER2 = Arbeidsgiver.fra(new VirksomhetEntitet.Builder().medOrgnr("490830958").build());

    @Test
    public void skalReturnereTrueOmUlikeAktivitetstatuser() {
        // Arrange
        BeregningsgrunnlagEntitet aktivt = BeregningsgrunnlagEntitet.builder()
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.FRILANSER))
            .medSkjæringstidspunkt(LocalDate.now())
            .build();

        BeregningsgrunnlagEntitet forrige = BeregningsgrunnlagEntitet.builder()
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
        BeregningsgrunnlagEntitet aktivt = BeregningsgrunnlagEntitet.builder()
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER))
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .build();

        BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null)
            .build(aktivt);

        BeregningsgrunnlagEntitet forrige = BeregningsgrunnlagEntitet.builder()
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER))
            .medSkjæringstidspunkt(LocalDate.now())
            .build();

        BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(1))
            .build(forrige);

        BeregningsgrunnlagPeriode.builder()
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
        BeregningsgrunnlagEntitet aktivt = BeregningsgrunnlagEntitet.builder()
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE))
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .build();

        BeregningsgrunnlagPeriode aktivPeriode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null)
            .build(aktivt);

        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medInntektskategori(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE)
            .medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
            .build(aktivPeriode);

        BeregningsgrunnlagEntitet forrige = BeregningsgrunnlagEntitet.builder()
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE))
            .medSkjæringstidspunkt(LocalDate.now())
            .build();

        BeregningsgrunnlagPeriode forrigePeriode = BeregningsgrunnlagPeriode.builder()
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
        BeregningsgrunnlagEntitet aktivt = BeregningsgrunnlagEntitet.builder()
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE))
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .build();

        BeregningsgrunnlagPeriode aktivPeriode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null)
            .build(aktivt);

        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medInntektskategori(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE)
            .medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
            .medAndelsnr(1L)
            .build(aktivPeriode);

        BeregningsgrunnlagEntitet forrige = BeregningsgrunnlagEntitet.builder()
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE))
            .medSkjæringstidspunkt(LocalDate.now())
            .build();

        BeregningsgrunnlagPeriode forrigePeriode = BeregningsgrunnlagPeriode.builder()
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
        BeregningsgrunnlagEntitet aktivt = BeregningsgrunnlagEntitet.builder()
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER))
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .build();

        BeregningsgrunnlagPeriode aktivPeriode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null)
            .build(aktivt);

        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(ARBEIDSGIVER2))
            .medAndelsnr(1L)
            .build(aktivPeriode);

        BeregningsgrunnlagEntitet forrige = BeregningsgrunnlagEntitet.builder()
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER))
            .medSkjæringstidspunkt(LocalDate.now())
            .build();

        BeregningsgrunnlagPeriode forrigePeriode = BeregningsgrunnlagPeriode.builder()
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
        BeregningsgrunnlagEntitet aktivt = BeregningsgrunnlagEntitet.builder()
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER))
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .build();

        BeregningsgrunnlagPeriode aktivPeriode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null)
            .build(aktivt);

        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(ARBEIDSGIVER1).medArbeidsgiver(ARBEIDSGIVER1))
            .medAndelsnr(1L)
            .build(aktivPeriode);

        BeregningsgrunnlagEntitet forrige = BeregningsgrunnlagEntitet.builder()
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER))
            .medSkjæringstidspunkt(LocalDate.now())
            .build();

        BeregningsgrunnlagPeriode forrigePeriode = BeregningsgrunnlagPeriode.builder()
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
        BeregningsgrunnlagEntitet aktivt = BeregningsgrunnlagEntitet.builder()
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER))
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .build();

        BeregningsgrunnlagPeriode aktivPeriode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null)
            .build(aktivt);

        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medArbforholdType(OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE)
            .medAndelsnr(1L)
            .build(aktivPeriode);

        BeregningsgrunnlagEntitet forrige = BeregningsgrunnlagEntitet.builder()
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER))
            .medSkjæringstidspunkt(LocalDate.now())
            .build();

        BeregningsgrunnlagPeriode forrigePeriode = BeregningsgrunnlagPeriode.builder()
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
        BeregningsgrunnlagEntitet aktivt = BeregningsgrunnlagEntitet.builder()
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER))
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .build();

        LocalDate sgTom = LocalDate.now().plusMonths(3);
        LocalDate sgFom = LocalDate.now();

        Sammenligningsgrunnlag.builder()
            .medAvvikPromilleNy(BigDecimal.valueOf(250.0124))
            .medSammenligningsperiode(sgFom, sgTom)
            .medRapportertPrÅr(BigDecimal.valueOf(300_000))
            .build(aktivt);

        BeregningsgrunnlagEntitet forrige = BeregningsgrunnlagEntitet.builder()
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER))
            .medSkjæringstidspunkt(LocalDate.now())
            .build();

        Sammenligningsgrunnlag.builder()
            .medAvvikPromilleNy(BigDecimal.valueOf(250.0123))
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
        BeregningsgrunnlagEntitet aktivt = BeregningsgrunnlagEntitet.builder()
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER))
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .build();

        LocalDate sgTom = LocalDate.now().plusMonths(3);
        LocalDate sgFom = LocalDate.now();

        Sammenligningsgrunnlag.builder()
            .medAvvikPromilleNy(BigDecimal.valueOf(250.654987))
            .medSammenligningsperiode(sgFom, sgTom)
            .medRapportertPrÅr(BigDecimal.valueOf(300_001))
            .build(aktivt);

        BeregningsgrunnlagEntitet forrige = BeregningsgrunnlagEntitet.builder()
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER))
            .medSkjæringstidspunkt(LocalDate.now())
            .build();

        Sammenligningsgrunnlag.builder()
            .medAvvikPromilleNy(BigDecimal.valueOf(250.654987))
            .medSammenligningsperiode(sgFom, sgTom)
            .medRapportertPrÅr(BigDecimal.valueOf(300_000))
            .build(forrige);

        // Act
        boolean harDiff = BeregningsgrunnlagDiffSjekker.harSignifikantDiffIBeregningsgrunnlag(aktivt, forrige);

        // Assert
        assertThat(harDiff).isTrue();
    }

}

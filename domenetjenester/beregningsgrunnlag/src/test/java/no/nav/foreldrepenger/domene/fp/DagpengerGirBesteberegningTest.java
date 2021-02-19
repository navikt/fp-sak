package no.nav.foreldrepenger.domene.fp;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktiviteter;
import no.nav.foreldrepenger.domene.iay.modell.YtelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.Arbeidskategori;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DagpengerGirBesteberegningTest {
    private static final LocalDate STP = LocalDate.of(2020,6,1);

    @Test
    public void skal_gi_true_ved_sykepenger_på_dagpenger_på_stp() {
        // Arrange
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusDays(30), STP.plusDays(1));
        YtelseBuilder ytelseBuilder = lagYtelse(RelatertYtelseType.SYKEPENGER, periode, RelatertYtelseTilstand.LØPENDE);
        lagYtelseAnvist(ytelseBuilder, periode);
        lagYtelseGrunnlag(ytelseBuilder, Arbeidskategori.DAGPENGER);

        // Act
        no.nav.abakus.iaygrunnlag.Periode periode2 = new no.nav.abakus.iaygrunnlag.Periode(STP.minusDays(30), STP.plusDays(1));
        boolean resultat = DagpengerGirBesteberegning.harDagpengerPåEllerIntillSkjæringstidspunkt(
            OpptjeningAktiviteter.fra(OpptjeningAktivitetType.FRILANS, periode2),
            Collections.singletonList(ytelseBuilder.build()),
            STP);

        // Assert
        assertThat(resultat).isTrue();

    }

    @Test
    public void skal_gi_true_ved_sykepenger_på_dagpenger_dagen_før_stp() {
        // Arrange
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusDays(30), STP.minusDays(1));
        YtelseBuilder ytelseBuilder = lagYtelse(RelatertYtelseType.SYKEPENGER, periode, RelatertYtelseTilstand.LØPENDE);
        lagYtelseAnvist(ytelseBuilder, periode);
        lagYtelseGrunnlag(ytelseBuilder, Arbeidskategori.DAGPENGER);

        // Act
        no.nav.abakus.iaygrunnlag.Periode periode2 = new no.nav.abakus.iaygrunnlag.Periode(STP.minusDays(30), STP.plusDays(1));
        boolean resultat = DagpengerGirBesteberegning.harDagpengerPåEllerIntillSkjæringstidspunkt(
            OpptjeningAktiviteter.fra(OpptjeningAktivitetType.FRILANS, periode2),
            Collections.singletonList(ytelseBuilder.build()),
            STP);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void skal_gi_false_ved_sykepenger_på_dagpenger_2_dager_før_stp() {
        // Arrange
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusDays(30), STP.minusDays(2));
        YtelseBuilder ytelseBuilder = lagYtelse(RelatertYtelseType.SYKEPENGER, periode, RelatertYtelseTilstand.LØPENDE);
        lagYtelseAnvist(ytelseBuilder, periode);
        lagYtelseGrunnlag(ytelseBuilder, Arbeidskategori.DAGPENGER);

        // Act
        no.nav.abakus.iaygrunnlag.Periode periode2 = new no.nav.abakus.iaygrunnlag.Periode(STP.minusDays(30), STP.plusDays(1));
        boolean resultat = DagpengerGirBesteberegning.harDagpengerPåEllerIntillSkjæringstidspunkt(
            OpptjeningAktiviteter.fra(OpptjeningAktivitetType.FRILANS, periode2),
            Collections.singletonList(ytelseBuilder.build()),
            STP);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void skal_gi_true_ved_dagpenger_etter_stp() {
        // Act
        no.nav.abakus.iaygrunnlag.Periode periode = new no.nav.abakus.iaygrunnlag.Periode(STP.minusDays(30), STP.plusDays(1));
        boolean resultat = DagpengerGirBesteberegning.harDagpengerPåEllerIntillSkjæringstidspunkt(
            OpptjeningAktiviteter.fra(OpptjeningAktivitetType.DAGPENGER, periode),
            Collections.emptyList(),
            STP);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void skal_gi_true_ved_dagpenger_på_stp() {
        // Act
        no.nav.abakus.iaygrunnlag.Periode periode = new no.nav.abakus.iaygrunnlag.Periode(STP.minusDays(30), STP);
        boolean resultat = DagpengerGirBesteberegning.harDagpengerPåEllerIntillSkjæringstidspunkt(
            OpptjeningAktiviteter.fra(OpptjeningAktivitetType.DAGPENGER, periode),
            Collections.emptyList(),
            STP);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void skal_gi_true_ved_dagpenger_dagen_før_stp() {
        // Act
        no.nav.abakus.iaygrunnlag.Periode periode = new no.nav.abakus.iaygrunnlag.Periode(STP.minusDays(30), STP.minusDays(1));
        boolean resultat = DagpengerGirBesteberegning.harDagpengerPåEllerIntillSkjæringstidspunkt(
            OpptjeningAktiviteter.fra(OpptjeningAktivitetType.DAGPENGER, periode),
            Collections.emptyList(),
            STP);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void skal_gi_false_ved_dagpenger_2_dager_før_stp() {
        // Act
        no.nav.abakus.iaygrunnlag.Periode periode = new no.nav.abakus.iaygrunnlag.Periode(STP.minusDays(30), STP.minusDays(2));
        boolean resultat = DagpengerGirBesteberegning.harDagpengerPåEllerIntillSkjæringstidspunkt(
            OpptjeningAktiviteter.fra(OpptjeningAktivitetType.DAGPENGER, periode),
            Collections.emptyList(),
            STP);

        // Assert
        assertThat(resultat).isFalse();
    }

    private void lagYtelseAnvist(YtelseBuilder builder, DatoIntervallEntitet periode) {
        builder.medYtelseAnvist(builder.getAnvistBuilder().medAnvistPeriode(periode)
            .medBeløp(BigDecimal.ZERO)
            .medUtbetalingsgradProsent(BigDecimal.ZERO)
            .build());
    }

    private void lagYtelseGrunnlag(YtelseBuilder builder, Arbeidskategori arbeidskategori) {
        builder.medYtelseGrunnlag(builder.getGrunnlagBuilder()
            .medArbeidskategori(arbeidskategori)
            .medDekningsgradProsent(BigDecimal.ZERO)
            .medVedtaksDagsats(Beløp.ZERO)
            .medInntektsgrunnlagProsent(BigDecimal.ZERO)
            .build());
    }

    private YtelseBuilder lagYtelse(RelatertYtelseType type, DatoIntervallEntitet periode, RelatertYtelseTilstand status) {
        return YtelseBuilder.oppdatere(Optional.empty())
            .medPeriode(periode)
            .medYtelseType(type)
            .medStatus(status)
            .medKilde(Fagsystem.INFOTRYGD)
            .medSaksnummer(new Saksnummer("333"));
    }

}

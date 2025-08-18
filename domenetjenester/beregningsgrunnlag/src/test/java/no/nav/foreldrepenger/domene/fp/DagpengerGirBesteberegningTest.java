package no.nav.foreldrepenger.domene.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.abakus.iaygrunnlag.Periode;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.YtelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.Arbeidskategori;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktiviteter;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

class DagpengerGirBesteberegningTest {
    private static final LocalDate STP = LocalDate.of(2020,6,2);

    @Test
    void skal_gi_true_ved_sykepenger_på_dagpenger_på_stp() {
        // Arrange
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusDays(30), STP.plusDays(1));
        var ytelseBuilder = lagYtelse(RelatertYtelseType.SYKEPENGER, periode, RelatertYtelseTilstand.LØPENDE);
        lagYtelseAnvist(ytelseBuilder, periode);
        lagYtelseGrunnlag(ytelseBuilder, Arbeidskategori.DAGPENGER);

        // Act
        var periode2 = new Periode(STP.minusDays(30), STP.plusDays(1));
        var resultat = DagpengerGirBesteberegning.harDagpengerPåEllerIntillSkjæringstidspunkt(
            OpptjeningAktiviteter.fra(OpptjeningAktivitetType.FRILANS, periode2),
            Collections.singletonList(ytelseBuilder.build()),
            STP);

        // Assert
        assertThat(resultat).isTrue();

    }

    @Test
    void skal_gi_true_ved_sykepenger_på_dagpenger_dagen_før_stp() {
        // Arrange
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusDays(30), STP.minusDays(1));
        var ytelseBuilder = lagYtelse(RelatertYtelseType.SYKEPENGER, periode, RelatertYtelseTilstand.LØPENDE);
        lagYtelseAnvist(ytelseBuilder, periode);
        lagYtelseGrunnlag(ytelseBuilder, Arbeidskategori.DAGPENGER);

        // Act
        var periode2 = new Periode(STP.minusDays(30), STP.plusDays(1));
        var resultat = DagpengerGirBesteberegning.harDagpengerPåEllerIntillSkjæringstidspunkt(
            OpptjeningAktiviteter.fra(OpptjeningAktivitetType.FRILANS, periode2),
            Collections.singletonList(ytelseBuilder.build()),
            STP);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    void skal_gi_false_ved_sykepenger_på_dagpenger_2_dager_før_stp() {
        // Arrange
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusDays(30), STP.minusDays(2));
        var ytelseBuilder = lagYtelse(RelatertYtelseType.SYKEPENGER, periode, RelatertYtelseTilstand.LØPENDE);
        lagYtelseAnvist(ytelseBuilder, periode);
        lagYtelseGrunnlag(ytelseBuilder, Arbeidskategori.DAGPENGER);

        // Act
        var periode2 = new Periode(STP.minusDays(30), STP.plusDays(1));
        var resultat = DagpengerGirBesteberegning.harDagpengerPåEllerIntillSkjæringstidspunkt(
            OpptjeningAktiviteter.fra(OpptjeningAktivitetType.FRILANS, periode2),
            Collections.singletonList(ytelseBuilder.build()),
            STP);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_gi_true_ved_dagpenger_etter_stp() {
        // Act
        var periode = new Periode(STP.minusDays(30), STP.plusDays(1));
        var resultat = DagpengerGirBesteberegning.harDagpengerPåEllerIntillSkjæringstidspunkt(
            OpptjeningAktiviteter.fra(OpptjeningAktivitetType.DAGPENGER, periode),
            Collections.emptyList(),
            STP);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    void skal_gi_true_ved_dagpenger_som_starter_1_dag_før_stp() {
        // Act
        var periode = new Periode(STP.minusDays(1), STP.plusDays(20));
        var resultat = DagpengerGirBesteberegning.harDagpengerPåEllerIntillSkjæringstidspunkt(
            OpptjeningAktiviteter.fra(OpptjeningAktivitetType.DAGPENGER, periode),
            Collections.emptyList(),
            STP);

        // Assert
        assertThat(resultat).isTrue();
    }


    @Test
    void skal_gi_true_ved_dagpenger_på_stp() {
        // Act
        var periode = new Periode(STP.minusDays(30), STP);
        var resultat = DagpengerGirBesteberegning.harDagpengerPåEllerIntillSkjæringstidspunkt(
            OpptjeningAktiviteter.fra(OpptjeningAktivitetType.DAGPENGER, periode),
            Collections.emptyList(),
            STP);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    void skal_gi_true_ved_dagpenger_dagen_før_stp() {
        // Act
        var periode = new Periode(STP.minusDays(30), STP.minusDays(1));
        var resultat = DagpengerGirBesteberegning.harDagpengerPåEllerIntillSkjæringstidspunkt(
            OpptjeningAktiviteter.fra(OpptjeningAktivitetType.DAGPENGER, periode),
            Collections.emptyList(),
            STP);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    void skal_gi_false_ved_dagpenger_2_dager_før_stp() {
        // Act
        var periode = new Periode(STP.minusDays(30), STP.minusDays(2));
        var resultat = DagpengerGirBesteberegning.harDagpengerPåEllerIntillSkjæringstidspunkt(
            OpptjeningAktiviteter.fra(OpptjeningAktivitetType.DAGPENGER, periode),
            Collections.emptyList(),
            STP);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void når_stp_er_mandag_skal_vi_se_på_fredagen_før_for_sykepenger() {
        // Act
        var mandagSTP = STP.minusDays(1);
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(mandagSTP.minusDays(30), mandagSTP.minusDays(3));
        var ytelseBuilder = lagYtelse(RelatertYtelseType.SYKEPENGER, periode, RelatertYtelseTilstand.LØPENDE);
        lagYtelseAnvist(ytelseBuilder, periode);
        lagYtelseGrunnlag(ytelseBuilder, Arbeidskategori.DAGPENGER);

        // Act
        var periode2 = new Periode(STP.minusDays(30), STP.plusDays(1));
        var resultat = DagpengerGirBesteberegning.harDagpengerPåEllerIntillSkjæringstidspunkt(
            OpptjeningAktiviteter.fra(OpptjeningAktivitetType.FRILANS, periode2),
            Collections.singletonList(ytelseBuilder.build()),
            mandagSTP);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    void åpent_sykepengevedtak_skal_ikke_gi_besteberegning() {
        // Act
        var mandagSTP = STP.minusDays(1);
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(mandagSTP.minusDays(30), mandagSTP.minusDays(3));
        var ytelseBuilder = lagYtelse(RelatertYtelseType.SYKEPENGER, periode, RelatertYtelseTilstand.ÅPEN);
        lagYtelseAnvist(ytelseBuilder, periode);
        lagYtelseGrunnlag(ytelseBuilder, Arbeidskategori.DAGPENGER);

        // Act
        var periode2 = new Periode(STP.minusDays(30), STP.plusDays(1));
        var resultat = DagpengerGirBesteberegning.harDagpengerPåEllerIntillSkjæringstidspunkt(
            OpptjeningAktiviteter.fra(OpptjeningAktivitetType.FRILANS, periode2),
            Collections.singletonList(ytelseBuilder.build()),
            mandagSTP);

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
            .medSaksnummer(new Saksnummer("3339"));
    }

}

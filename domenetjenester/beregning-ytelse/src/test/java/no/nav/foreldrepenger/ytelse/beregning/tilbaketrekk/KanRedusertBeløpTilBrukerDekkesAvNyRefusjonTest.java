package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KanRedusertBeløpTilBrukerDekkesAvNyRefusjonTest {

    @Test
    void skal_gi_nei_hvis_inntekt_lik_dagsats_for_bruker_lik_refusjon_lik() {
        // Arrange
        var originalDagsatsBruker = 600;
        var revurderingRefusjon = 1500;
        var revurderingDagsatsBruker = 600;

        var endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        var resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingRefusjon);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_gi_nei_hvis_inntekt_lik_dagsats_for_bruker_lik_og_ingen_refusjon_i_original_og_revurdering() {
        // Arrange
        var originalDagsatsBruker = 2100;
        var revurderingRefusjon = 0;
        var revurderingDagsatsBruker = 2100;

        var endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        var resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingRefusjon);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_gi_nei_hvis_inntekt_lik_refusjon_lik_ingen_dagsats_til_bruker_i_original_og_revurdering() {
        // Arrange
        var originalDagsatsBruker = 0;
        var revurderingRefusjon = 2100;
        var revurderingDagsatsBruker = 0;

        var endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        var resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingRefusjon);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_gi_ja_hvis_inntekt_lik_refusjon_økt_og_dagsats_for_bruker_redusert() {
        // Arrange
        var originalDagsatsBruker = 1800;
        var revurderingRefusjon = 1100;
        var revurderingDagsatsBruker = 1000;

        var endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        var resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingRefusjon);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    void skal_gi_nei_hvis_inntekt_lik_refusjon_redusert_og_dagsats_for_bruker_økt() {
        // Arrange
        var originalDagsatsBruker = 600;
        var revurderingRefusjon = 900;
        var revurderingDagsatsBruker = 1200;

        var endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        var resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingRefusjon);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_gi_nei_hvis_inntekt_økt_refusjon_lik_og_dagsats_for_bruker_økt() {
        // Arrange
        var originalDagsatsBruker = 100;
        var revurderingRefusjon = 800;
        var revurderingDagsatsBruker = 1300;

        var endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        var resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingRefusjon);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_gi_nei_hvis_inntekt_økt_dagsats_for_bruker_økt_finnes_ingen_refusjon_i_original_og_revurdering() {
        // Arrange
        var originalDagsatsBruker = 900;
        var revurderingRefusjon = 0;
        var revurderingDagsatsBruker = 2100;

        var endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        var resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingRefusjon);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_gi_nei_hvis_inntekt_økt_refusjon_økt_og_ingen_endring_til_bruker() {
        // Arrange
        var originalDagsatsBruker = 100;
        var revurderingRefusjon = 2000;
        var revurderingDagsatsBruker = 100;

        var endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        var resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingRefusjon);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_gi_nei_hvis_inntekt_økt_dagsats_for_bruker_økt_ingen_refusjon_i_forrige_og_revurdering() {
        // Arrange
        var originalDagsatsBruker = 0;
        var revurderingRefusjon = 2100;
        var revurderingDagsatsBruker = 0;

        var endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        var resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingRefusjon);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_gi_nei_hvis_inntekt_økt_refusjon_økt_og_dagsats_for_bruker_økt() {
        // Arrange
        var originalDagsatsBruker = 100;
        var revurderingRefusjon = 1200;
        var revurderingDagsatsBruker = 900;

        var endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        var resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingRefusjon);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_gi_nei_hvis_inntekt_økt_refusjon_økt_fra_null_og_dagsats_for_bruker_økt() {
        // Arrange
        var originalDagsatsBruker = 900;
        var revurderingRefusjon = 1100;
        var revurderingDagsatsBruker = 1000;

        var endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        var resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingRefusjon);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_gi_nei_hvis_inntekt_økt_dagsats_for_bruker_økt_fra_null_og_refusjon_økt() {
        // Arrange
        var originalDagsatsBruker = 0;
        var revurderingRefusjon = 1100;
        var revurderingDagsatsBruker = 1000;

        var endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        var resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingRefusjon);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_gi_ja_hvis_inntekt_økt_refusjon_økt_og_dagsats_for_bruker_redusert() {
        // Arrange
        var originalDagsatsBruker = 800;
        var revurderingRefusjon = 1500;
        var revurderingDagsatsBruker = 600;

        var endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        var resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingRefusjon);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    void skal_gi_ja_hvis_inntekt_økt_refusjon_økt_fra_null_og_dagsats_for_bruker_redusert() {
        // Arrange
        var originalDagsatsBruker = 900;
        var revurderingRefusjon = 1500;
        var revurderingDagsatsBruker = 600;

        var endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        var resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingRefusjon);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    void skal_gi_ja_hvis_inntekt_økt_dagsats_for_bruker_opphørt_og_refusjon_økt() {
        // Arrange
        var originalDagsatsBruker = 800;
        var revurderingRefusjon = 2100;
        var revurderingDagsatsBruker = 0;

        var endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        var resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingRefusjon);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    void skal_gi_nei_hvis_inntekt_økt_refusjon_redusert_og_dagsats_for_bruker_økt() {
        // Arrange
        var originalDagsatsBruker = 100;
        var revurderingRefusjon = 700;
        var revurderingDagsatsBruker = 1400;

        var endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        var resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingRefusjon);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_gi_nei_hvis_inntekt_økt_refusjon_opphørt_og_dagsats_for_bruker_økt() {
        // Arrange
        var originalDagsatsBruker = 100;
        var revurderingRefusjon = 0;
        var revurderingDagsatsBruker = 2100;

        var endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        var resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingRefusjon);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_gi_nei_hvis_inntekt_økt_refusjon_redusert_og_dagsats_for_bruker_økt_fra_null() {
        // Arrange
        var originalDagsatsBruker = 0;
        var revurderingRefusjon = 700;
        var revurderingDagsatsBruker = 1400;

        var endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        var resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingRefusjon);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_gi_nei_hvis_inntekt_redusert_ingen_refusjon_i_original_og_revurdering_og_dagsats_for_bruker_redusert() {
        // Arrange
        var originalDagsatsBruker = 2100;
        var revurderingRefusjon = 0;
        var revurderingDagsatsBruker = 900;

        var endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        var resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingRefusjon);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_gi_nei_hvis_dagsats_for_bruker_opphører_og_refusjon_finnes_ikke_i_forrige_og_revurdering() {
        // Arrange
        var originalDagsatsBruker = 2100;
        var revurderingRefusjon = 0;
        var revurderingDagsatsBruker = 0;

        var endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        var resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingRefusjon);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_gi_nei_hvis_inntekt_redusert_refusjon_lik_og_dagsats_for_bruker_redusert() {
        // Arrange
        var originalDagsatsBruker = 900;
        var revurderingRefusjon = 1200;
        var revurderingDagsatsBruker = 200;

        var endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        var resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingRefusjon);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    void skal_gi_nei_hvis_inntekt_redusert_refusjon_lik_dagsats_for_bruker_redusert_mer_enn_ny_refusjon() {
        // Arrange
        var originalDagsatsBruker = 1500;
        var revurderingRefusjon = 600;
        var revurderingDagsatsBruker = 100;

        var endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        var resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingRefusjon);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_gi_nei_hvis_inntekt_redusert_refusjon_lik_og_dagsats_for_bruker_opphører() {
        // Arrange
        var originalDagsatsBruker = 1400;
        var revurderingRefusjon = 700;
        var revurderingDagsatsBruker = 0;

        var endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        var resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingRefusjon);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_gi_nei_hvis_inntekt_redusert_refusjon_redusert_og_dagsats_for_bruker_er_lik() {
        // Arrange
        var originalDagsatsBruker = 1000;
        var revurderingRefusjon = 400;
        var revurderingDagsatsBruker = 1000;

        var endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        var resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingRefusjon);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_gi_nei_hvis_inntekt_redusert_dagsats_for_bruker_lik_og_refusjon_opphører() {
        // Arrange
        var originalDagsatsBruker = 1400;
        var revurderingRefusjon = 0;
        var revurderingDagsatsBruker = 1400;

        var endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        var resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingRefusjon);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_gi_ja_hvis_inntekt_redusert_refusjon_redusert_og_dagsats_for_bruker_redusert() {
        // Arrange
        var originalDagsatsBruker = 1000;
        var revurderingRefusjon = 300;
        var revurderingDagsatsBruker = 900;

        var endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        var resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingRefusjon);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    void skal_gi_ja_hvis_inntekt_redusert_refusjon_redusert_og_dagsats_for_bruker_redusert_like_mye() {
        // Arrange
        var originalDagsatsBruker = 1400;
        var revurderingRefusjon = 600;
        var revurderingDagsatsBruker = 800;

        var endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        var resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingRefusjon);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    void skal_gi_nei_hvis_inntekt_redusert_refusjon_redusert_og_dagsats_for_bruker_redusert_mer_enn_refusjon() {
        // Arrange
        var originalDagsatsBruker = 1900;
        var revurderingRefusjon = 100;
        var revurderingDagsatsBruker = 1300;

        var endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        var resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingRefusjon);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_gi_ja_hvis_inntekt_redusert_refusjon_redusert_og_dagsats_for_bruker_opphørt() {
        // Arrange
        var originalDagsatsBruker = 1500;
        var revurderingRefusjon = 1400;
        var revurderingDagsatsBruker = 0;

        var endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        var resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingRefusjon);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_gi_nei_hvis_inntekt_redusert_refusjon_redusert_og_dagsats_for_bruker_økt() {
        // Arrange
        var originalDagsatsBruker = 500;
        var revurderingRefusjon = 800;
        var revurderingDagsatsBruker = 600;

        var endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        var resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingRefusjon);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_gi_nei_hvis_inntekt_redusert_refusjon_opphørt_og_dagsats_for_bruker_økt() {
        // Arrange
        var originalDagsatsBruker = 500;
        var revurderingRefusjon = 0;
        var revurderingDagsatsBruker = 1400;

        var endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        var resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingRefusjon);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_gi_nei_hvis_inntekt_redusert_refusjon_redusert_og_dagsats_for_bruker_økt_fra_null() {
        // Arrange
        var originalDagsatsBruker = 0;
        var revurderingRefusjon = 1200;
        var revurderingDagsatsBruker = 200;

        var endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        var resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingRefusjon);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_gi_ja_hvis_inntekt_redusert_refusjon_økt_og_dagsats_for_bruker_redusert() {
        // Arrange
        var originalDagsatsBruker = 1400;
        var revurderingRefusjon = 800;
        var revurderingDagsatsBruker = 600;

        var endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        var resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingRefusjon);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    void skal_gi_ja_hvis_inntekt_redusert_refusjon_økt_dagsats_for_bruker_redusert_mer_enn_ny_refusjon() {
        // Arrange
        var originalDagsatsBruker = 2000;
        var revurderingRefusjon = 200;
        var revurderingDagsatsBruker = 1200;

        var endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        var resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingRefusjon);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_gi_ja_hvis_inntekt_redusert_refusjon_økt_dagsats_for_bruker_opphørt() {
        // Arrange
        var originalDagsatsBruker = 800;
        var revurderingRefusjon = 1400;
        var revurderingDagsatsBruker = 0;

        var endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        var resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingRefusjon);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    void skal_gi_ja_hvis_inntekt_redusert_refusjon_økt_dagsats_for_bruker_redusert_til_null_hvor_endring_er_mer_enn_ny_refusjon() {
        // Arrange
        var originalDagsatsBruker = 2000;
        var revurderingRefusjon = 200;
        var revurderingDagsatsBruker = 0;

        var endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        var resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingRefusjon);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_gi_nei_hvis_inntekt_redusert_refusjon_økt_fra_null_og_dagsats_for_bruker_redusert_mer_enn_ny_refusjon() {
        // Arrange
        var originalDagsatsBruker = 2100;
        var revurderingRefusjon = 800;
        var revurderingDagsatsBruker = 600;

        var endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        var resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingRefusjon);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_gi_nei_hvis_inntekt_redusert_refusjon_økt_fra_null_dagsats_for_bruker_redusert_hvor_endring_er_mer_enn_ny_refusjon() {
        // Arrange
        var originalDagsatsBruker = 2100;
        var revurderingRefusjon = 200;
        var revurderingDagsatsBruker = 1800;

        var endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        var resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingRefusjon);

        // Assert
        assertThat(resultat).isFalse();
    }
}

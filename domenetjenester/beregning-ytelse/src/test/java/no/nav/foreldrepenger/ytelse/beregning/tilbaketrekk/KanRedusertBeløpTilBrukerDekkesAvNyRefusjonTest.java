package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk.KanRedusertBeløpTilBrukerDekkesAvNyRefusjon;

public class KanRedusertBeløpTilBrukerDekkesAvNyRefusjonTest {

    @Test
    public void skal_gi_nei_hvis_inntekt_lik_dagsats_for_bruker_lik_refusjon_lik() {
        // Arrange
        int originalDagsatsBruker = 600;
        int revurderingRefusjon = 1500;
        int revurderingDagsatsBruker = 600;

        int endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        boolean resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingRefusjon
        );

        // Assert
        assertThat(resultat).isFalse();
    }


    @Test
    public void skal_gi_nei_hvis_inntekt_lik_dagsats_for_bruker_lik_og_ingen_refusjon_i_original_og_revurdering() {
        // Arrange
        int originalDagsatsBruker = 2100;
        int revurderingRefusjon = 0;
        int revurderingDagsatsBruker = 2100;

        int endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        boolean resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingRefusjon
        );

        // Assert
        assertThat(resultat).isFalse();
    }


    @Test
    public void skal_gi_nei_hvis_inntekt_lik_refusjon_lik_ingen_dagsats_til_bruker_i_original_og_revurdering() {
        // Arrange
        int originalDagsatsBruker = 0;
        int revurderingRefusjon = 2100;
        int revurderingDagsatsBruker = 0;

        int endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        boolean resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingRefusjon
        );

        // Assert
        assertThat(resultat).isFalse();
    }


    @Test
    public void skal_gi_ja_hvis_inntekt_lik_refusjon_økt_og_dagsats_for_bruker_redusert() {
        // Arrange
        int originalDagsatsBruker = 1800;
        int revurderingRefusjon = 1100;
        int revurderingDagsatsBruker = 1000;

        int endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        boolean resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingRefusjon
        );

        // Assert
        assertThat(resultat).isTrue();
    }


    @Test
    public void skal_gi_nei_hvis_inntekt_lik_refusjon_redusert_og_dagsats_for_bruker_økt() {
        // Arrange
        int originalDagsatsBruker = 600;
        int revurderingRefusjon = 900;
        int revurderingDagsatsBruker = 1200;

        int endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        boolean resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingRefusjon
        );

        // Assert
        assertThat(resultat).isFalse();
    }


    @Test
    public void skal_gi_nei_hvis_inntekt_økt_refusjon_lik_og_dagsats_for_bruker_økt() {
        // Arrange
        int originalDagsatsBruker = 100;
        int revurderingRefusjon = 800;
        int revurderingDagsatsBruker = 1300;

        int endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        boolean resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingRefusjon
        );

        // Assert
        assertThat(resultat).isFalse();
    }


    @Test
    public void skal_gi_nei_hvis_inntekt_økt_dagsats_for_bruker_økt_finnes_ingen_refusjon_i_original_og_revurdering() {
        // Arrange
        int originalDagsatsBruker = 900;
        int revurderingRefusjon = 0;
        int revurderingDagsatsBruker = 2100;

        int endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        boolean resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingRefusjon
        );

        // Assert
        assertThat(resultat).isFalse();
    }


    @Test
    public void skal_gi_nei_hvis_inntekt_økt_refusjon_økt_og_ingen_endring_til_bruker() {
        // Arrange
        int originalDagsatsBruker = 100;
        int revurderingRefusjon = 2000;
        int revurderingDagsatsBruker = 100;

        int endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        boolean resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingRefusjon
        );

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void skal_gi_nei_hvis_inntekt_økt_dagsats_for_bruker_økt_ingen_refusjon_i_forrige_og_revurdering() {
        // Arrange
        int originalDagsatsBruker = 0;
        int revurderingRefusjon = 2100;
        int revurderingDagsatsBruker = 0;

        int endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        boolean resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingRefusjon
        );

        // Assert
        assertThat(resultat).isFalse();
    }


    @Test
    public void skal_gi_nei_hvis_inntekt_økt_refusjon_økt_og_dagsats_for_bruker_økt() {
        // Arrange
        int originalDagsatsBruker = 100;
        int revurderingRefusjon = 1200;
        int revurderingDagsatsBruker = 900;

        int endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        boolean resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingRefusjon
        );

        // Assert
        assertThat(resultat).isFalse();
    }


    @Test
    public void skal_gi_nei_hvis_inntekt_økt_refusjon_økt_fra_null_og_dagsats_for_bruker_økt() {
        // Arrange
        int originalDagsatsBruker = 900;
        int revurderingRefusjon = 1100;
        int revurderingDagsatsBruker = 1000;

        int endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        boolean resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingRefusjon
        );

        // Assert
        assertThat(resultat).isFalse();
    }


    @Test
    public void skal_gi_nei_hvis_inntekt_økt_dagsats_for_bruker_økt_fra_null_og_refusjon_økt() {
        // Arrange
        int originalDagsatsBruker = 0;
        int revurderingRefusjon = 1100;
        int revurderingDagsatsBruker = 1000;

        int endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        boolean resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingRefusjon
        );

        // Assert
        assertThat(resultat).isFalse();
    }


    @Test
    public void skal_gi_ja_hvis_inntekt_økt_refusjon_økt_og_dagsats_for_bruker_redusert() {
        // Arrange
        int originalDagsatsBruker = 800;
        int revurderingRefusjon = 1500;
        int revurderingDagsatsBruker = 600;

        int endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        boolean resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingRefusjon
        );

        // Assert
        assertThat(resultat).isTrue();
    }


    @Test
    public void skal_gi_ja_hvis_inntekt_økt_refusjon_økt_fra_null_og_dagsats_for_bruker_redusert() {
        // Arrange
        int originalDagsatsBruker = 900;
        int revurderingRefusjon = 1500;
        int revurderingDagsatsBruker = 600;

        int endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        boolean resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingRefusjon
        );

        // Assert
        assertThat(resultat).isTrue();
    }


    @Test
    public void skal_gi_ja_hvis_inntekt_økt_dagsats_for_bruker_opphørt_og_refusjon_økt() {
        // Arrange
        int originalDagsatsBruker = 800;
        int revurderingRefusjon = 2100;
        int revurderingDagsatsBruker = 0;

        int endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        boolean resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingRefusjon
        );

        // Assert
        assertThat(resultat).isTrue();
    }


    @Test
    public void skal_gi_nei_hvis_inntekt_økt_refusjon_redusert_og_dagsats_for_bruker_økt() {
        // Arrange
        int originalDagsatsBruker = 100;
        int revurderingRefusjon = 700;
        int revurderingDagsatsBruker = 1400;

        int endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        boolean resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingRefusjon
        );

        // Assert
        assertThat(resultat).isFalse();
    }


    @Test
    public void skal_gi_nei_hvis_inntekt_økt_refusjon_opphørt_og_dagsats_for_bruker_økt() {
        // Arrange
        int originalDagsatsBruker = 100;
        int revurderingRefusjon = 0;
        int revurderingDagsatsBruker = 2100;

        int endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        boolean resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingRefusjon
        );

        // Assert
        assertThat(resultat).isFalse();
    }


    @Test
    public void skal_gi_nei_hvis_inntekt_økt_refusjon_redusert_og_dagsats_for_bruker_økt_fra_null() {
        // Arrange
        int originalDagsatsBruker = 0;
        int revurderingRefusjon = 700;
        int revurderingDagsatsBruker = 1400;

        int endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        boolean resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingRefusjon
        );

        // Assert
        assertThat(resultat).isFalse();
    }


    @Test
    public void skal_gi_nei_hvis_inntekt_redusert_ingen_refusjon_i_original_og_revurdering_og_dagsats_for_bruker_redusert() {
        // Arrange
        int originalDagsatsBruker = 2100;
        int revurderingRefusjon = 0;
        int revurderingDagsatsBruker = 900;

        int endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        boolean resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingRefusjon
        );

        // Assert
        assertThat(resultat).isFalse();
    }


    @Test
    public void skal_gi_nei_hvis_dagsats_for_bruker_opphører_og_refusjon_finnes_ikke_i_forrige_og_revurdering() {
        // Arrange
        int originalDagsatsBruker = 2100;
        int revurderingRefusjon = 0;
        int revurderingDagsatsBruker = 0;

        int endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        boolean resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingRefusjon
        );

        // Assert
        assertThat(resultat).isFalse();
    }


    @Test
    public void skal_gi_nei_hvis_inntekt_redusert_refusjon_lik_og_dagsats_for_bruker_redusert() {
        // Arrange
        int originalDagsatsBruker = 900;
        int revurderingRefusjon = 1200;
        int revurderingDagsatsBruker = 200;

        int endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        boolean resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingRefusjon
        );

        // Assert
        assertThat(resultat).isTrue();
    }


    @Test
    public void skal_gi_nei_hvis_inntekt_redusert_refusjon_lik_dagsats_for_bruker_redusert_mer_enn_ny_refusjon() {
        // Arrange
        int originalDagsatsBruker = 1500;
        int revurderingRefusjon = 600;
        int revurderingDagsatsBruker = 100;

        int endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        boolean resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingRefusjon
        );

        // Assert
        assertThat(resultat).isFalse();
    }


    @Test
    public void skal_gi_nei_hvis_inntekt_redusert_refusjon_lik_og_dagsats_for_bruker_opphører() {
        // Arrange
        int originalDagsatsBruker = 1400;
        int revurderingRefusjon = 700;
        int revurderingDagsatsBruker = 0;

        int endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        boolean resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingRefusjon
        );

        // Assert
        assertThat(resultat).isFalse();
    }


    @Test
    public void skal_gi_nei_hvis_inntekt_redusert_refusjon_redusert_og_dagsats_for_bruker_er_lik() {
        // Arrange
        int originalDagsatsBruker = 1000;
        int revurderingRefusjon = 400;
        int revurderingDagsatsBruker = 1000;

        int endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        boolean resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingRefusjon
        );

        // Assert
        assertThat(resultat).isFalse();
    }


    @Test
    public void skal_gi_nei_hvis_inntekt_redusert_dagsats_for_bruker_lik_og_refusjon_opphører() {
        // Arrange
        int originalDagsatsBruker = 1400;
        int revurderingRefusjon = 0;
        int revurderingDagsatsBruker = 1400;

        int endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        boolean resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingRefusjon
        );

        // Assert
        assertThat(resultat).isFalse();
    }


    @Test
    public void skal_gi_ja_hvis_inntekt_redusert_refusjon_redusert_og_dagsats_for_bruker_redusert() {
        // Arrange
        int originalDagsatsBruker = 1000;
        int revurderingRefusjon = 300;
        int revurderingDagsatsBruker = 900;

        int endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        boolean resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingRefusjon
        );

        // Assert
        assertThat(resultat).isTrue();
    }


    @Test
    public void skal_gi_ja_hvis_inntekt_redusert_refusjon_redusert_og_dagsats_for_bruker_redusert_like_mye() {
        // Arrange
        int originalDagsatsBruker = 1400;
        int revurderingRefusjon = 600;
        int revurderingDagsatsBruker = 800;

        int endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        boolean resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingRefusjon
        );

        // Assert
        assertThat(resultat).isTrue();
    }


    @Test
    public void skal_gi_nei_hvis_inntekt_redusert_refusjon_redusert_og_dagsats_for_bruker_redusert_mer_enn_refusjon() {
        // Arrange
        int originalDagsatsBruker = 1900;
        int revurderingRefusjon = 100;
        int revurderingDagsatsBruker = 1300;

        int endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        boolean resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingRefusjon
        );

        // Assert
        assertThat(resultat).isFalse();
    }


    @Test
    public void skal_gi_ja_hvis_inntekt_redusert_refusjon_redusert_og_dagsats_for_bruker_opphørt() {
        // Arrange
        int originalDagsatsBruker = 1500;
        int revurderingRefusjon = 1400;
        int revurderingDagsatsBruker = 0;

        int endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        boolean resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingRefusjon
        );

        // Assert
        assertThat(resultat).isFalse();
    }


    @Test
    public void skal_gi_nei_hvis_inntekt_redusert_refusjon_redusert_og_dagsats_for_bruker_økt() {
        // Arrange
        int originalDagsatsBruker = 500;
        int revurderingRefusjon = 800;
        int revurderingDagsatsBruker = 600;

        int endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        boolean resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingRefusjon
        );

        // Assert
        assertThat(resultat).isFalse();
    }


    @Test
    public void skal_gi_nei_hvis_inntekt_redusert_refusjon_opphørt_og_dagsats_for_bruker_økt() {
        // Arrange
        int originalDagsatsBruker = 500;
        int revurderingRefusjon = 0;
        int revurderingDagsatsBruker = 1400;

        int endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        boolean resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingRefusjon
        );

        // Assert
        assertThat(resultat).isFalse();
    }


    @Test
    public void skal_gi_nei_hvis_inntekt_redusert_refusjon_redusert_og_dagsats_for_bruker_økt_fra_null() {
        // Arrange
        int originalDagsatsBruker = 0;
        int revurderingRefusjon = 1200;
        int revurderingDagsatsBruker = 200;

        int endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        boolean resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingRefusjon
        );

        // Assert
        assertThat(resultat).isFalse();
    }


    @Test
    public void skal_gi_ja_hvis_inntekt_redusert_refusjon_økt_og_dagsats_for_bruker_redusert() {
        // Arrange
        int originalDagsatsBruker = 1400;
        int revurderingRefusjon = 800;
        int revurderingDagsatsBruker = 600;

        int endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        boolean resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingRefusjon
        );

        // Assert
        assertThat(resultat).isTrue();
    }


    @Test
    public void skal_gi_ja_hvis_inntekt_redusert_refusjon_økt_dagsats_for_bruker_redusert_mer_enn_ny_refusjon() {
        // Arrange
        int originalDagsatsBruker = 2000;
        int revurderingRefusjon = 200;
        int revurderingDagsatsBruker = 1200;

        int endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        boolean resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingRefusjon
        );

        // Assert
        assertThat(resultat).isFalse();
    }


    @Test
    public void skal_gi_ja_hvis_inntekt_redusert_refusjon_økt_dagsats_for_bruker_opphørt() {
        // Arrange
        int originalDagsatsBruker = 800;
        int revurderingRefusjon = 1400;
        int revurderingDagsatsBruker = 0;

        int endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        boolean resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingRefusjon
        );

        // Assert
        assertThat(resultat).isTrue();
    }


    @Test
    public void skal_gi_ja_hvis_inntekt_redusert_refusjon_økt_dagsats_for_bruker_redusert_til_null_hvor_endring_er_mer_enn_ny_refusjon() {
        // Arrange
        int originalDagsatsBruker = 2000;
        int revurderingRefusjon = 200;
        int revurderingDagsatsBruker = 0;

        int endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        boolean resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingRefusjon
        );

        // Assert
        assertThat(resultat).isFalse();
    }


    @Test
    public void skal_gi_nei_hvis_inntekt_redusert_refusjon_økt_fra_null_og_dagsats_for_bruker_redusert_mer_enn_ny_refusjon() {
        // Arrange
        int originalDagsatsBruker = 2100;
        int revurderingRefusjon = 800;
        int revurderingDagsatsBruker = 600;

        int endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        boolean resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingRefusjon
        );

        // Assert
        assertThat(resultat).isFalse();
    }


    @Test
    public void skal_gi_nei_hvis_inntekt_redusert_refusjon_økt_fra_null_dagsats_for_bruker_redusert_hvor_endring_er_mer_enn_ny_refusjon() {
        // Arrange
        int originalDagsatsBruker = 2100;
        int revurderingRefusjon = 200;
        int revurderingDagsatsBruker = 1800;

        int endringIDagsatsBruker = revurderingDagsatsBruker - originalDagsatsBruker;

        // Act
        boolean resultat = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingRefusjon
        );

        // Assert
        assertThat(resultat).isFalse();
    }
}

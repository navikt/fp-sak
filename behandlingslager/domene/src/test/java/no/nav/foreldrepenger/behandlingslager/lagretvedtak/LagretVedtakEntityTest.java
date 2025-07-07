package no.nav.foreldrepenger.behandlingslager.lagretvedtak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LagretVedtakEntityTest {
    private LagretVedtak.Builder lagretVedtakBuilder;
    private LagretVedtak lagretVedtak;
    private LagretVedtak lagretVedtak2;

    private static final Long FAGSAK_ID = 22L;
    private static final Long BEHANDLING_ID = 433L;
    private static final String STRING_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><element>test av xml</element>";

    @BeforeEach
    void setup() {
        lagretVedtakBuilder = LagretVedtak.builder();
        lagretVedtak = null;
    }

    @Test
    void skal_bygge_instans_med_påkrevde_felter() {
        lagretVedtak = lagBuilderMedPaakrevdeFelter().build();

        assertThat(lagretVedtak.getFagsakId()).isEqualTo(FAGSAK_ID);
        assertThat(lagretVedtak.getBehandlingId()).isEqualTo(BEHANDLING_ID);
        assertThat(lagretVedtak.getXmlClob()).isEqualTo(STRING_XML);
    }

    @Test
    void skal_ikke_bygge_instans_hvis_mangler_påkrevde_felter() {
        // mangler fagsakId
        assertThatThrownBy(() -> lagretVedtakBuilder.build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("fagsakId");

        // mangler behandlingId
        lagretVedtakBuilder.medFagsakId(FAGSAK_ID);
        assertThatThrownBy(() -> lagretVedtakBuilder.build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("behandlingId");

        // mangler behandlingId
        lagretVedtakBuilder.medFagsakId(FAGSAK_ID).medBehandlingId(BEHANDLING_ID);
        assertThatThrownBy(() -> lagretVedtakBuilder.build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("xmlClob");
    }

    @Test
    void skal_håndtere_null_this_feilKlasse_i_equals() {
        lagretVedtak = lagBuilderMedPaakrevdeFelter().build();

        assertThat(lagretVedtak).isNotNull();
    }

    @Test
    void skal_ha_refleksiv_equalsOgHashCode() {
        lagretVedtakBuilder = lagBuilderMedPaakrevdeFelter();
        lagretVedtak = lagretVedtakBuilder.build();
        lagretVedtak2 = lagretVedtakBuilder.build();

        assertThat(lagretVedtak).isEqualTo(lagretVedtak2);
        assertThat(lagretVedtak2).isEqualTo(lagretVedtak);

        lagretVedtak2 = lagretVedtakBuilder.medFagsakId(252L).build();
        assertThat(lagretVedtak).isNotEqualTo(lagretVedtak2);
        assertThat(lagretVedtak2).isNotEqualTo(lagretVedtak);
    }

    @Test
    void skal_bruke_fagsakId_i_equalsOgHashCode() {
        lagretVedtakBuilder = lagBuilderMedPaakrevdeFelter();
        lagretVedtak = lagretVedtakBuilder.build();
        lagretVedtakBuilder.medFagsakId(302L);
        lagretVedtak2 = lagretVedtakBuilder.build();

        assertThat(lagretVedtak).isNotEqualTo(lagretVedtak2);
        assertThat(lagretVedtak.hashCode()).isNotEqualTo(lagretVedtak2.hashCode());
    }

    @Test
    void skal_bruke_behandlingId_i_equalsOgHashCode() {
        lagretVedtakBuilder = lagBuilderMedPaakrevdeFelter();
        lagretVedtak = lagretVedtakBuilder.build();
        lagretVedtakBuilder.medBehandlingId(525L);
        lagretVedtak2 = lagretVedtakBuilder.build();

        assertThat(lagretVedtak).isNotEqualTo(lagretVedtak2);
        assertThat(lagretVedtak.hashCode()).isNotEqualTo(lagretVedtak2.hashCode());

    }

    @Test
    void skal_bruke_dokument_i_equalsOgHashCode() {
        lagretVedtakBuilder = lagBuilderMedPaakrevdeFelter();
        lagretVedtak = lagretVedtakBuilder.build();
        lagretVedtakBuilder.medXmlClob("<?xml version=\"1.0\" encoding=\"UTF-8\"?><element>XML string</element>");
        lagretVedtak2 = lagretVedtakBuilder.build();

        assertThat(lagretVedtak).isNotEqualTo(lagretVedtak2);
        assertThat(lagretVedtak.hashCode()).isNotEqualTo(lagretVedtak2.hashCode());
    }

    // ----------------------------------------------------------

    private LagretVedtak.Builder lagBuilderMedPaakrevdeFelter() {
        return LagretVedtak.builder()
                .medFagsakId(FAGSAK_ID)
                .medBehandlingId(BEHANDLING_ID)
                .medXmlClob(STRING_XML);
    }

}

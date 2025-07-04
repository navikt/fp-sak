package no.nav.foreldrepenger.behandlingslager.behandling.vedtak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VedtakEntityTest {

    private BehandlingVedtak.Builder vedtakBuilder;
    private BehandlingVedtak vedtak;
    private BehandlingVedtak vedtak2;

    private static final LocalDateTime VEDTAKSDATO = LocalDateTime.now();
    private static final String ANSVARLIG_SAKSBEHBANDLER = "Ola Normann";
    private static final VedtakResultatType VEDTAK_RESULTAT_TYPE = VedtakResultatType.INNVILGET;

    @BeforeEach
    void setup() {
        vedtakBuilder = BehandlingVedtak.builder();
        vedtak = null;
    }

    @Test
    void skal_bygge_instans_med_påkrevde_felter() {
        vedtak = lagBuilderMedPaakrevdeFelter().build();

        assertThat(vedtak.getVedtakstidspunkt()).isEqualTo(VEDTAKSDATO);
        assertThat(vedtak.getAnsvarligSaksbehandler()).isEqualTo(ANSVARLIG_SAKSBEHBANDLER);
        assertThat(vedtak.getVedtakResultatType()).isEqualTo(VEDTAK_RESULTAT_TYPE);
    }

    @Test
    void skal_ikke_bygge_instans_hvis_mangler_påkrevde_felter() {

        // mangler vedtaksdato
        assertThatThrownBy(() -> vedtakBuilder.build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("vedtakstidspunkt");

        // mangler ansvarligSaksbehandler
        vedtakBuilder.medVedtakstidspunkt(VEDTAKSDATO);
        assertThatThrownBy(() -> vedtakBuilder.build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("ansvarligSaksbehandler");

        // mangler vedtakResultatType
        vedtakBuilder.medAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHBANDLER);
        assertThatThrownBy(() -> vedtakBuilder.build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("vedtakResultatType");
    }

    @Test
    void skal_ha_refleksiv_equalsOgHashCode() {
        vedtakBuilder = lagBuilderMedPaakrevdeFelter();
        vedtak = vedtakBuilder.build();
        vedtak2 = vedtakBuilder.build();

        assertThat(vedtak).isEqualTo(vedtak2);
        assertThat(vedtak2).isEqualTo(vedtak2);

        vedtakBuilder.medAnsvarligSaksbehandler("Kari Larsen");
        vedtak2 = vedtakBuilder.build();
        assertThat(vedtak2).isNotEqualTo(vedtak);
        assertThat(vedtak).isNotEqualTo(vedtak2);
    }

    @Test
    void skal_bruke_vedtaksdato_i_equalsOgHashCode() {
        vedtakBuilder = lagBuilderMedPaakrevdeFelter();
        vedtak = vedtakBuilder.build();
        vedtakBuilder.medVedtakstidspunkt(LocalDateTime.now().plus(1, ChronoUnit.DAYS));
        vedtak2 = vedtakBuilder.build();

        assertThat(vedtak).isNotEqualTo(vedtak2);
        assertThat(vedtak.hashCode()).isNotEqualTo(vedtak2.hashCode());
    }

    @Test
    void skal_bruke_ansvarligSaksbehandler_i_equalsOgHashCode() {
        vedtakBuilder = lagBuilderMedPaakrevdeFelter();
        vedtak = vedtakBuilder.build();
        vedtakBuilder.medAnsvarligSaksbehandler("Jostein Hansen");
        vedtak2 = vedtakBuilder.build();

        assertThat(vedtak).isNotEqualTo(vedtak2);
        assertThat(vedtak.hashCode()).isNotEqualTo(vedtak2.hashCode());
    }

    @Test
    void skal_bruke_vedtakResultatType_i_equalsOgHashCode() {
        vedtakBuilder = lagBuilderMedPaakrevdeFelter();
        vedtak = vedtakBuilder.build();
        vedtakBuilder.medVedtakResultatType(VedtakResultatType.AVSLAG);
        vedtak2 = vedtakBuilder.build();

        assertThat(vedtak).isNotEqualTo(vedtak2);
        assertThat(vedtak.hashCode()).isNotEqualTo(vedtak2.hashCode());
    }

    // ----------------------------------------------------------------

    private static BehandlingVedtak.Builder lagBuilderMedPaakrevdeFelter() {
        return BehandlingVedtak.builder()
                .medVedtakstidspunkt(VEDTAKSDATO)
                .medAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHBANDLER)
                .medVedtakResultatType(VEDTAK_RESULTAT_TYPE);
    }

}

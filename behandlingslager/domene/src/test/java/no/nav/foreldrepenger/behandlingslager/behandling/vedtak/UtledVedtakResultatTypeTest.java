package no.nav.foreldrepenger.behandlingslager.behandling.vedtak;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;

class UtledVedtakResultatTypeTest {

    @Test
    void vedtakResultatTypeSettesTilVEDTAK_I_KLAGEBEHANDLING() {
        // Act
        var vedtakResultatType = UtledVedtakResultatType.utled(BehandlingType.KLAGE, BehandlingResultatType.KLAGE_MEDHOLD);

        // Assert
        assertThat(vedtakResultatType).isEqualTo(VedtakResultatType.VEDTAK_I_KLAGEBEHANDLING);
    }

    @Test
    void vedtakResultatTypeSettesTilVEDTAK_I_ANKEBEHANDLING() {
        // Act
        var vedtakResultatType = UtledVedtakResultatType.utled(BehandlingType.ANKE, BehandlingResultatType.ANKE_MEDHOLD);

        // Assert
        assertThat(vedtakResultatType).isEqualTo(VedtakResultatType.VEDTAK_I_ANKEBEHANDLING);
    }

    @Test
    void vedtakResultatTypeSettesTilVEDTAK_I_INNSYNBEHANDLING() {
        // Act
        var vedtakResultatType = UtledVedtakResultatType.utled(BehandlingType.INNSYN, BehandlingResultatType.INNSYN_INNVILGET);

        // Assert
        assertThat(vedtakResultatType).isEqualTo(VedtakResultatType.VEDTAK_I_INNSYNBEHANDLING);
    }

    @Test
    void vedtakResultatTypeSettesTilAVSLAG() {
        // Act
        var vedtakResultatType = UtledVedtakResultatType.utled(BehandlingType.FØRSTEGANGSSØKNAD, BehandlingResultatType.AVSLÅTT);

        // Assert
        assertThat(vedtakResultatType).isEqualTo(VedtakResultatType.AVSLAG);
    }

    @Test
    void vedtakResultatTypeSettesTilINNVILGETForInnvilget() {
        // Act
        var vedtakResultatType = UtledVedtakResultatType.utled(BehandlingType.FØRSTEGANGSSØKNAD, BehandlingResultatType.INNVILGET);

        // Assert
        assertThat(vedtakResultatType).isEqualTo(VedtakResultatType.INNVILGET);
    }

    @Test
    void vedtakResultatTypeSettesTilINNVILGETForForeldrepengerEndret() {
        // Act
        var vedtakResultatType = UtledVedtakResultatType.utled(BehandlingType.FØRSTEGANGSSØKNAD,
                BehandlingResultatType.FORELDREPENGER_ENDRET);

        // Assert
        assertThat(vedtakResultatType).isEqualTo(VedtakResultatType.INNVILGET);
    }
}

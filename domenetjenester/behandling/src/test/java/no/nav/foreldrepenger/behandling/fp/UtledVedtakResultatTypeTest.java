package no.nav.foreldrepenger.behandling.fp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;

public class UtledVedtakResultatTypeTest {

    @Test
    public void vedtakResultatTypeSettesTilVEDTAK_I_KLAGEBEHANDLING() {
        // Act
        VedtakResultatType vedtakResultatType = UtledVedtakResultatType.utled(BehandlingType.KLAGE, BehandlingResultatType.KLAGE_MEDHOLD);

        // Assert
        assertThat(vedtakResultatType).isEqualTo(VedtakResultatType.VEDTAK_I_KLAGEBEHANDLING);
    }

    @Test
    public void vedtakResultatTypeSettesTilVEDTAK_I_ANKEBEHANDLING() {
        // Act
        VedtakResultatType vedtakResultatType = UtledVedtakResultatType.utled(BehandlingType.ANKE, BehandlingResultatType.ANKE_OMGJOER);

        // Assert
        assertThat(vedtakResultatType).isEqualTo(VedtakResultatType.VEDTAK_I_ANKEBEHANDLING);
    }

    @Test
    public void vedtakResultatTypeSettesTilVEDTAK_I_INNSYNBEHANDLING() {
        // Act
        VedtakResultatType vedtakResultatType = UtledVedtakResultatType.utled(BehandlingType.INNSYN, BehandlingResultatType.INNSYN_INNVILGET);

        // Assert
        assertThat(vedtakResultatType).isEqualTo(VedtakResultatType.VEDTAK_I_INNSYNBEHANDLING);
    }

    @Test
    public void vedtakResultatTypeSettesTilAVSLAG() {
        // Act
        VedtakResultatType vedtakResultatType = UtledVedtakResultatType.utled(BehandlingType.FØRSTEGANGSSØKNAD, BehandlingResultatType.AVSLÅTT);

        // Assert
        assertThat(vedtakResultatType).isEqualTo(VedtakResultatType.AVSLAG);
    }

    @Test
    public void vedtakResultatTypeSettesTilINNVILGETForInnvilget() {
        // Act
        VedtakResultatType vedtakResultatType = UtledVedtakResultatType.utled(BehandlingType.FØRSTEGANGSSØKNAD, BehandlingResultatType.INNVILGET);

        // Assert
        assertThat(vedtakResultatType).isEqualTo(VedtakResultatType.INNVILGET);
    }

    @Test
    public void vedtakResultatTypeSettesTilINNVILGETForForeldrepengerEndret() {
        // Act
        VedtakResultatType vedtakResultatType = UtledVedtakResultatType.utled(BehandlingType.FØRSTEGANGSSØKNAD,
                BehandlingResultatType.FORELDREPENGER_ENDRET);

        // Assert
        assertThat(vedtakResultatType).isEqualTo(VedtakResultatType.INNVILGET);
    }
}

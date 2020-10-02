package no.nav.foreldrepenger.behandling.es;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.fp.UtledVedtakResultatType;
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

}

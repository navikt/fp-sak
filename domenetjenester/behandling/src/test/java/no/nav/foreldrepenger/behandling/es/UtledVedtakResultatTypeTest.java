package no.nav.foreldrepenger.behandling.es;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import no.nav.foreldrepenger.behandling.fp.UtledVedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioInnsynEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioKlageEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;

public class UtledVedtakResultatTypeTest {

    private ScenarioMorSøkerEngangsstønad scenarioFørstegang;

    @Before
    public void setup() {
        scenarioFørstegang = ScenarioMorSøkerEngangsstønad.forFødsel();
    }

    @Test
    public void vedtakResultatTypeSettesTilVEDTAK_I_KLAGEBEHANDLING() {
        // Arrange
        var scenarioKlage = ScenarioKlageEngangsstønad.forMedholdNFP(scenarioFørstegang);
        Behandling behandling = scenarioKlage.lagMocked();

        // Act
        VedtakResultatType vedtakResultatType = UtledVedtakResultatType.utled(behandling);

        // Assert
        assertThat(vedtakResultatType).isEqualTo(VedtakResultatType.VEDTAK_I_KLAGEBEHANDLING);
    }

    @Test
    public void vedtakResultatTypeSettesTilVEDTAK_I_INNSYNBEHANDLING() {
        // Arrange
        var scenarioKlage = ScenarioInnsynEngangsstønad.innsyn(scenarioFørstegang);
        Behandling behandling = scenarioKlage.lagMocked();

        // Act
        VedtakResultatType vedtakResultatType = UtledVedtakResultatType.utled(behandling);

        // Assert
        assertThat(vedtakResultatType).isEqualTo(VedtakResultatType.VEDTAK_I_INNSYNBEHANDLING);
    }

    @Test
    public void vedtakResultatTypeSettesTilAVSLAG() {
        // Arrange
        scenarioFørstegang.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.AVSLÅTT));
        Behandling behandling = scenarioFørstegang.lagMocked();

        // Act
        VedtakResultatType vedtakResultatType = UtledVedtakResultatType.utled(behandling);

        // Assert
        assertThat(vedtakResultatType).isEqualTo(VedtakResultatType.AVSLAG);
    }

    @Test
    public void vedtakResultatTypeSettesTilINNVILGETForInnvilget() {
        // Arrange
        scenarioFørstegang.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        Behandling behandling = scenarioFørstegang.lagMocked();

        // Act
        VedtakResultatType vedtakResultatType = UtledVedtakResultatType.utled(behandling);

        // Assert
        assertThat(vedtakResultatType).isEqualTo(VedtakResultatType.INNVILGET);
    }

}

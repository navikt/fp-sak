package no.nav.foreldrepenger.behandling.fp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioAnkeEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioInnsynEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioKlageEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;

public class UtledVedtakResultatTypeTest {

    private ScenarioMorSøkerForeldrepenger scenarioFørstegang;

    @Before
    public void setup() {
        scenarioFørstegang = ScenarioMorSøkerForeldrepenger.forFødsel();
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
    public void vedtakResultatTypeSettesTilVEDTAK_I_ANKEBEHANDLING() {
        // Arrange
        var scenarioAnke = ScenarioAnkeEngangsstønad.forOmgjør(scenarioFørstegang);
        Behandling behandling = scenarioAnke.lagMocked();

        // Act
        VedtakResultatType vedtakResultatType = UtledVedtakResultatType.utled(behandling);

        // Assert
        assertThat(vedtakResultatType).isEqualTo(VedtakResultatType.VEDTAK_I_ANKEBEHANDLING);
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

    @Test
    public void vedtakResultatTypeSettesTilINNVILGETForForeldrepengerEndret() {
        // Arrange
        scenarioFørstegang.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.FORELDREPENGER_ENDRET));
        Behandling behandling = scenarioFørstegang.lagMocked();

        // Act
        VedtakResultatType vedtakResultatType = UtledVedtakResultatType.utled(behandling);

        // Assert
        assertThat(vedtakResultatType).isEqualTo(VedtakResultatType.INNVILGET);
    }

    /**
     * Behandling 1: Avslått
     * Behandling 2: Ingen endring
     */
    @Test
    public void vedtakResultatTypeSettesTilAVSLAGForIngenEndringNårForrigeBehandlingAvslått() {
        // Arrange
        scenarioFørstegang.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.AVSLÅTT));
        Behandling behandling1 = scenarioFørstegang.lagMocked();
        Behandling behandling2 = lagRevurdering(behandling1);

        // Act
        VedtakResultatType vedtakResultatType = UtledVedtakResultatType.utled(behandling2);

        // Assert
        assertThat(vedtakResultatType).isEqualTo(VedtakResultatType.AVSLAG);
    }

    /**
     * Behandling 1: Innvilget
     * Behandling 2: Ingen endring
     * Behandling 3: Ingen endring
     */
    @Test
    public void vedtakResultatTypeSettesTilAVSLAGForIngenEndringNårBehandling1InnvilgetOgBehandling2IngenEndring() {
        // Arrange
        scenarioFørstegang.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        Behandling behandling1 = scenarioFørstegang.lagMocked();
        Behandling behandling2 = lagRevurdering(behandling1);
        Behandling behandling3 = lagRevurdering(behandling2);

        // Act
        VedtakResultatType vedtakResultatType = UtledVedtakResultatType.utled(behandling3);

        // Assert
        assertThat(vedtakResultatType).isEqualTo(VedtakResultatType.INNVILGET);
    }

    private Behandling lagRevurdering(Behandling tidligereBehandling) {
        BehandlingÅrsak.Builder årsakBuilder = BehandlingÅrsak.builder(BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING)
            .medOriginalBehandling(tidligereBehandling);
        Behandling revurdering = Behandling.fraTidligereBehandling(tidligereBehandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(årsakBuilder)
            .build();
        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INGEN_ENDRING).buildFor(revurdering);
        return revurdering;
    }
}

package no.nav.foreldrepenger.behandling.steg.klage;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioKlageEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KlageNkStegTest {

    @Test
    void skalUtføreUtenAksjonspunktNårBehandlingsresultatTypeIkkeErYtelsesStadfestet() {
        // Arrange
        var scenario = ScenarioKlageEngangsstønad.forAvvistNFP(ScenarioMorSøkerEngangsstønad.forAdopsjon());
        var klageBehandling = scenario.lagMocked();
        var kontekst = new BehandlingskontrollKontekst(klageBehandling.getFagsakId(), klageBehandling.getAktørId(),
                new BehandlingLås(klageBehandling.getId()));
        var klageRepository = scenario.getKlageRepository();

        var steg = new KlageNkSteg(scenario.mockBehandlingRepository(), klageRepository);

        // Act
        var behandlingStegResultat = steg.utførSteg(kontekst);

        // Assert
        assertThat(behandlingStegResultat).isNotNull();
        assertThat(behandlingStegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(behandlingStegResultat.getAksjonspunktListe()).isEmpty();
    }

}

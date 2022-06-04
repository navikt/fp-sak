package no.nav.foreldrepenger.behandling.steg.klage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioKlageEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;

public class KlageNkStegTest {

    @Test
    public void skalUtføreUtenAksjonspunktNårBehandlingsresultatTypeIkkeErYtelsesStadfestet() {
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

    @Test
    public void skalOverhoppBakoverRyddeKlageVurderingRestultat() {
        // Arrange
        var scenario = ScenarioKlageEngangsstønad.forAvvistNK(ScenarioMorSøkerEngangsstønad.forAdopsjon());
        var klageBehandling = scenario.lagMocked();
        var kontekst = new BehandlingskontrollKontekst(klageBehandling.getFagsakId(), klageBehandling.getAktørId(),
                new BehandlingLås(klageBehandling.getId()));

        var klageRepository = scenario.getKlageRepository();
        var repositoryProviderMock = scenario.mockBehandlingRepositoryProvider();
        var behandlingRepository = repositoryProviderMock.getBehandlingRepository();

        var steg = new KlageNkSteg(behandlingRepository, klageRepository);
        // Act
        steg.vedTransisjon(kontekst, null, BehandlingSteg.TransisjonType.HOPP_OVER_BAKOVER, null, null);

        // Assert
        assertThat(klageRepository.hentKlageVurderingResultat(klageBehandling.getId(), KlageVurdertAv.NK)
                .filter(KlageVurderingResultat::isGodkjentAvMedunderskriver)).isEmpty();
    }

}

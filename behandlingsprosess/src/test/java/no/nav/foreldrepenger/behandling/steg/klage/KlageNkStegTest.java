package no.nav.foreldrepenger.behandling.steg.klage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioKlageEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;

public class KlageNkStegTest {

    @Test
    public void skalOppretteAksjonspunktKlageNKVedYtelsesStadfestet() {
        // Arrange
        var scenario = ScenarioKlageEngangsstønad.forStadfestetNFP(ScenarioMorSøkerEngangsstønad.forAdopsjon());
        Behandling klageBehandling = scenario.lagMocked();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(klageBehandling.getFagsakId(), klageBehandling.getAktørId(),
                new BehandlingLås(klageBehandling.getId()));

        KlageRepository klageRepository = scenario.getKlageRepository();

        KlageNkSteg steg = new KlageNkSteg(scenario.mockBehandlingRepository(), klageRepository);

        // Act
        BehandleStegResultat behandlingStegResultat = steg.utførSteg(kontekst);

        // Assert
        assertThat(behandlingStegResultat).isNotNull();
        assertThat(behandlingStegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(behandlingStegResultat.getAksjonspunktListe()).hasSize(1);

        AksjonspunktDefinisjon aksjonspunktDefinisjon = behandlingStegResultat.getAksjonspunktListe().get(0);
        assertThat(aksjonspunktDefinisjon).isEqualTo(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NK);
    }

    @Test
    public void skalUtføreUtenAksjonspunktNårBehandlingsresultatTypeIkkeErYtelsesStadfestet() {
        // Arrange
        var scenario = ScenarioKlageEngangsstønad.forAvvistNFP(ScenarioMorSøkerEngangsstønad.forAdopsjon());
        Behandling klageBehandling = scenario.lagMocked();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(klageBehandling.getFagsakId(), klageBehandling.getAktørId(),
                new BehandlingLås(klageBehandling.getId()));
        KlageRepository klageRepository = scenario.getKlageRepository();

        KlageNkSteg steg = new KlageNkSteg(scenario.mockBehandlingRepository(), klageRepository);

        // Act
        BehandleStegResultat behandlingStegResultat = steg.utførSteg(kontekst);

        // Assert
        assertThat(behandlingStegResultat).isNotNull();
        assertThat(behandlingStegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(behandlingStegResultat.getAksjonspunktListe()).isEmpty();
    }

    @Test
    public void skalOverhoppBakoverRyddeKlageVurderingRestultat() {
        // Arrange
        var scenario = ScenarioKlageEngangsstønad.forAvvistNK(ScenarioMorSøkerEngangsstønad.forAdopsjon());
        Behandling klageBehandling = scenario.lagMocked();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(klageBehandling.getFagsakId(), klageBehandling.getAktørId(),
                new BehandlingLås(klageBehandling.getId()));

        KlageRepository klageRepository = scenario.getKlageRepository();
        BehandlingRepositoryProvider repositoryProviderMock = scenario.mockBehandlingRepositoryProvider();
        BehandlingRepository behandlingRepository = repositoryProviderMock.getBehandlingRepository();

        KlageNkSteg steg = new KlageNkSteg(behandlingRepository, klageRepository);
        // Act
        steg.vedTransisjon(kontekst, null, BehandlingSteg.TransisjonType.HOPP_OVER_BAKOVER, null, null);

        // Assert
        assertThat(klageRepository.hentKlageVurderingResultat(klageBehandling.getId(), KlageVurdertAv.NK)
                .filter(KlageVurderingResultat::isGodkjentAvMedunderskriver)).isEmpty();
    }

}

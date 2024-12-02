package no.nav.foreldrepenger.behandling.steg.klage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioKlageEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;

class KlageNfpStegTest {

    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private KlageNfpSteg steg;
    private OrganisasjonsEnhet enhet = new OrganisasjonsEnhet("4806", "NFP Drammen");

    @BeforeEach
    public void setUp() {
        behandlendeEnhetTjeneste = mock(BehandlendeEnhetTjeneste.class);
        var behandlingRepositoryMock = mock(BehandlingRepository.class);
        when(behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(any(Fagsak.class))).thenReturn(enhet);

        steg = new KlageNfpSteg(behandlingRepositoryMock, behandlendeEnhetTjeneste);
    }

    @Test
    void skalOppretteAksjonspunktManuellVurderingAvKlageNfpNårStegKjøres() {
        var scenario = ScenarioKlageEngangsstønad.forMedholdNK(ScenarioMorSøkerEngangsstønad.forFødsel());
        var klageBehandling = scenario.lagMocked();
        var kontekst = new BehandlingskontrollKontekst(klageBehandling.getSaksnummer(), klageBehandling.getFagsakId(),
                new BehandlingLås(klageBehandling.getId()));

        // Act
        var behandlingStegResultat = steg.utførSteg(kontekst);

        // Assert
        assertThat(behandlingStegResultat).isNotNull();
        assertThat(behandlingStegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(behandlingStegResultat.getAksjonspunktListe()).hasSize(1);

        var aksjonspunktDefinisjon = behandlingStegResultat.getAksjonspunktListe().get(0);
        assertThat(aksjonspunktDefinisjon).isEqualTo(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP);
    }

    @Test
    void skalOverhoppBakoverRyddeKlageVurderingRestultatOgLageHistorikkInnslag() {
        // Arrange

        var scenario = ScenarioKlageEngangsstønad.forMedholdNK(ScenarioMorSøkerEngangsstønad.forFødsel());
        var klageBehandling = scenario.lagMocked();
        var kontekst = new BehandlingskontrollKontekst(klageBehandling.getSaksnummer(), klageBehandling.getFagsakId(),
                new BehandlingLås(klageBehandling.getId()));
        var repositoryProviderMock = scenario.mockBehandlingRepositoryProvider();
        steg = new KlageNfpSteg(repositoryProviderMock.getBehandlingRepository(), behandlendeEnhetTjeneste);

        // Act
        steg.vedTransisjon(kontekst, null, BehandlingSteg.TransisjonType.HOPP_OVER_BAKOVER, null, null);

        // Assert
        verify(behandlendeEnhetTjeneste, times(1)).oppdaterBehandlendeEnhet(any(), any(), any(), any());
    }
}

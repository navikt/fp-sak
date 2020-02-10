package no.nav.foreldrepenger.behandling.steg.klage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioKlageEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;

public class KlageNfpStegTest {

    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private KlageNfpSteg steg;
    private OrganisasjonsEnhet enhet = new OrganisasjonsEnhet("4806", "NFP Drammen");

    @Before
    public void setUp() {
        behandlendeEnhetTjeneste = mock(BehandlendeEnhetTjeneste.class);
        BehandlingRepository behandlingRepositoryMock = mock(BehandlingRepository.class);
        when(behandlendeEnhetTjeneste.sjekkEnhetVedNyAvledetBehandling(any(Behandling.class))).thenReturn(Optional.of(enhet));

        steg = new KlageNfpSteg(behandlingRepositoryMock, null, behandlendeEnhetTjeneste);
    }

    @Test
    public void skalOppretteAksjonspunktManuellVurderingAvKlageNfpNårStegKjøres() {
        ScenarioKlageEngangsstønad scenario = ScenarioKlageEngangsstønad.forMedholdNK(ScenarioMorSøkerEngangsstønad.forFødsel());
        Behandling klageBehandling = scenario.lagMocked();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(klageBehandling.getFagsakId(), klageBehandling.getAktørId(), new BehandlingLås(klageBehandling.getId()));

        // Act
        BehandleStegResultat behandlingStegResultat = steg.utførSteg(kontekst);

        // Assert
        assertThat(behandlingStegResultat).isNotNull();
        assertThat(behandlingStegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(behandlingStegResultat.getAksjonspunktListe()).hasSize(1);

        AksjonspunktDefinisjon aksjonspunktDefinisjon = behandlingStegResultat.getAksjonspunktListe().get(0);
        assertThat(aksjonspunktDefinisjon).isEqualTo(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP);
    }

    @Test
    public void skalOverhoppBakoverRyddeKlageVurderingRestultatOgLageHistorikkInnslag() {
        // Arrange

        var scenario = ScenarioKlageEngangsstønad.forMedholdNK(ScenarioMorSøkerEngangsstønad.forFødsel());
        Behandling klageBehandling = scenario.lagMocked();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(klageBehandling.getFagsakId(), klageBehandling.getAktørId(), new BehandlingLås(klageBehandling.getId()));
        BehandlingRepositoryProvider repositoryProviderMock = scenario.mockBehandlingRepositoryProvider();
        steg = new KlageNfpSteg(repositoryProviderMock.getBehandlingRepository(), scenario.getKlageRepository(), behandlendeEnhetTjeneste);

        // Act
        steg.vedTransisjon(kontekst, null, BehandlingSteg.TransisjonType.HOPP_OVER_BAKOVER, null, null);

        // Assert
        verify(behandlendeEnhetTjeneste, times(1)).oppdaterBehandlendeEnhet(any(), any(), any(), any());
    }
}

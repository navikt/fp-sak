package no.nav.foreldrepenger.behandling.steg.iverksettevedtak;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerApplikasjonTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;

public class IverksetteInnsynVedtakStegFellesTest {

    @Test
    public void skalBestilleVedtaksbrev() {
        ScenarioMorSøkerEngangsstønad scenario = innsynsScenario();
        DokumentBestillerApplikasjonTjeneste dokumentBestillerTjeneste = mock(DokumentBestillerApplikasjonTjeneste.class);
        BehandlingRepositoryProvider repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        IverksetteInnsynVedtakStegFelles steg = new IverksetteInnsynVedtakStegFelles(dokumentBestillerTjeneste, repositoryProvider);
        Behandling behandling = scenario.getBehandling();
        Fagsak fagsak = behandling.getFagsak();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));
        steg.utførSteg(kontekst);

        ArgumentCaptor<BestillBrevDto> argumentCaptor = ArgumentCaptor.forClass(BestillBrevDto.class);
        verify(dokumentBestillerTjeneste, times(1))
            .bestillDokument(argumentCaptor.capture(), any(HistorikkAktør.class), Mockito.anyBoolean());
    }

    @Test
    public void skalDefaulteFritekstTilMellomrom() {
        ScenarioMorSøkerEngangsstønad scenario = innsynsScenario();
        DokumentBestillerApplikasjonTjeneste dokumentBestillerTjeneste = mock(DokumentBestillerApplikasjonTjeneste.class);
        BehandlingRepositoryProvider repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        IverksetteInnsynVedtakStegFelles steg = new IverksetteInnsynVedtakStegFelles(dokumentBestillerTjeneste, repositoryProvider);
        Behandling behandling = scenario.getBehandling();
        Fagsak fagsak = behandling.getFagsak();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));
        steg.utførSteg(kontekst);

        ArgumentCaptor<BestillBrevDto> argumentCaptor = ArgumentCaptor.forClass(BestillBrevDto.class);
        verify(dokumentBestillerTjeneste, times(1))
            .bestillDokument(argumentCaptor.capture(), any(HistorikkAktør.class), Mockito.anyBoolean());

        assertThat(argumentCaptor.getValue().getFritekst()).isEqualTo(" ");
    }

    @Test
    public void skalBrukeBegrunnelseFraAksjonspunktSomFritekst() {
        String begrunnelse = "begrunnelse!!";
        ScenarioMorSøkerEngangsstønad scenario = innsynsScenario(begrunnelse);
        DokumentBestillerApplikasjonTjeneste dokumentBestillerTjeneste = mock(DokumentBestillerApplikasjonTjeneste.class);
        BehandlingRepositoryProvider repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        IverksetteInnsynVedtakStegFelles steg = new IverksetteInnsynVedtakStegFelles(dokumentBestillerTjeneste, repositoryProvider);
        Behandling behandling = scenario.getBehandling();
        Fagsak fagsak = behandling.getFagsak();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));
        steg.utførSteg(kontekst);


        ArgumentCaptor<BestillBrevDto> argumentCaptor = ArgumentCaptor.forClass(BestillBrevDto.class);
        verify(dokumentBestillerTjeneste, times(1))
            .bestillDokument(argumentCaptor.capture(), any(HistorikkAktør.class), Mockito.anyBoolean());

        assertThat(argumentCaptor.getValue().getFritekst()).isEqualTo(begrunnelse);
    }

    private ScenarioMorSøkerEngangsstønad innsynsScenario() {
        return innsynsScenario(null);
    }

    private ScenarioMorSøkerEngangsstønad innsynsScenario(String begrunnelse) {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medBehandlingType(BehandlingType.INNSYN);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.FORESLÅ_VEDTAK, BehandlingStegType.FORESLÅ_VEDTAK);
        scenario.lagMocked();
        Aksjonspunkt aksjonspunkt = scenario.getBehandling().getAksjonspunktFor(AksjonspunktDefinisjon.FORESLÅ_VEDTAK);
        AksjonspunktTestSupport.setTilUtført(aksjonspunkt, begrunnelse);
        return scenario;
    }

}

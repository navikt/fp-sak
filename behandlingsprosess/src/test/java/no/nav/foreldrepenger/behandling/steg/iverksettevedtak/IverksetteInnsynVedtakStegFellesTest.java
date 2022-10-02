package no.nav.foreldrepenger.behandling.steg.iverksettevedtak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;
import no.nav.foreldrepenger.domene.vedtak.impl.BehandlingVedtakEventPubliserer;

public class IverksetteInnsynVedtakStegFellesTest {

    @Test
    public void skalBestilleVedtaksbrev() {
        var scenario = innsynsScenario();
        var dokumentBestillerTjeneste = mock(DokumentBestillerTjeneste.class);
        var repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        var steg = new IverksetteInnsynVedtakStegFelles(dokumentBestillerTjeneste, repositoryProvider, mock(BehandlingVedtakEventPubliserer.class));
        var behandling = scenario.getBehandling();
        var fagsak = behandling.getFagsak();
        var kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
                repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));
        steg.utførSteg(kontekst);

        var argumentCaptor = ArgumentCaptor.forClass(BestillBrevDto.class);
        verify(dokumentBestillerTjeneste, times(1))
                .bestillDokument(argumentCaptor.capture(), any(HistorikkAktør.class));
    }

    @Test
    public void skalDefaulteFritekstTilMellomrom() {
        var scenario = innsynsScenario();
        var dokumentBestillerTjeneste = mock(DokumentBestillerTjeneste.class);
        var repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        var steg = new IverksetteInnsynVedtakStegFelles(dokumentBestillerTjeneste, repositoryProvider, mock(BehandlingVedtakEventPubliserer.class));
        var behandling = scenario.getBehandling();
        var fagsak = behandling.getFagsak();
        var kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
                repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));
        steg.utførSteg(kontekst);

        var argumentCaptor = ArgumentCaptor.forClass(BestillBrevDto.class);
        verify(dokumentBestillerTjeneste, times(1))
                .bestillDokument(argumentCaptor.capture(), any(HistorikkAktør.class));

        assertThat(argumentCaptor.getValue().getFritekst()).isEqualTo(" ");
    }

    @Test
    public void skalBrukeBegrunnelseFraAksjonspunktSomFritekst() {
        var begrunnelse = "begrunnelse!!";
        var scenario = innsynsScenario(begrunnelse);
        var dokumentBestillerTjeneste = mock(DokumentBestillerTjeneste.class);
        var repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        var steg = new IverksetteInnsynVedtakStegFelles(dokumentBestillerTjeneste, repositoryProvider, mock(BehandlingVedtakEventPubliserer.class));
        var behandling = scenario.getBehandling();
        var fagsak = behandling.getFagsak();
        var kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
                repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));
        steg.utførSteg(kontekst);

        var argumentCaptor = ArgumentCaptor.forClass(BestillBrevDto.class);
        verify(dokumentBestillerTjeneste, times(1))
                .bestillDokument(argumentCaptor.capture(), any(HistorikkAktør.class));

        assertThat(argumentCaptor.getValue().getFritekst()).isEqualTo(begrunnelse);
    }

    private ScenarioMorSøkerEngangsstønad innsynsScenario() {
        return innsynsScenario(null);
    }

    private ScenarioMorSøkerEngangsstønad innsynsScenario(String begrunnelse) {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medBehandlingType(BehandlingType.INNSYN);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.FORESLÅ_VEDTAK, BehandlingStegType.FORESLÅ_VEDTAK);
        scenario.lagMocked();
        var aksjonspunkt = scenario.getBehandling().getAksjonspunktFor(AksjonspunktDefinisjon.FORESLÅ_VEDTAK);
        AksjonspunktTestSupport.setTilUtført(aksjonspunkt, begrunnelse);
        return scenario;
    }

}

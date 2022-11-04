package no.nav.foreldrepenger.web.app.oppgave;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKobling;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKoblingRepository;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;

@SuppressWarnings({ "deprecation", "resource" })
public class OppgaveRedirectTjenesteTest {

    private final OppgaveBehandlingKoblingRepository oppgaveRepo = Mockito.mock(OppgaveBehandlingKoblingRepository.class);
    private final FagsakRepository fagsakRepo = Mockito.mock(FagsakRepository.class);
    private final BehandlingRepository behandlingRepository = Mockito.mock(BehandlingRepository.class);

    private final RedirectFactory redirectFactory = new RedirectFactory();
    private final OppgaveRedirectTjeneste tjeneste = new OppgaveRedirectTjeneste(oppgaveRepo, fagsakRepo, behandlingRepository,
        redirectFactory);

    private final Saksnummer saksnummer = new Saksnummer("022");

    @BeforeEach
    public void setLoadBalancerUrl() {
        redirectFactory.setLoadBalancerUrl("https://erstatter.nav.no");
    }

    @Test
    public void skal_lage_url_med_feilmelding_når_hverken_oppgaveId_eller_sakId_finnes() {
        var response = tjeneste.doRedirect(null, null);

        var feilmelding = "Sak+kan+ikke+%C3%A5pnes%2C+da+referanse+mangler.";
        assertThat(response.getStatus()).isEqualTo(Response.Status.TEMPORARY_REDIRECT.getStatusCode());
        assertThat(response.getLocation().toString()).isEqualTo("https://erstatter.nav.no/fpsak/#?errormessage=" + feilmelding);
    }

    @Test
    public void skal_lage_url_med_feilmelding_når_både_oppgaveId_og_sakId_finnes_i_url_men_ikke_finnes_ikke_i_vl() {
        var response = tjeneste.doRedirect(new OppgaveIdDto("1"), new SaksnummerDto("2"));
        var feilmelding = "Det+finnes+ingen+sak+med+dette+saksnummeret%3A+2";
        assertThat(response.getStatus()).isEqualTo(Response.Status.TEMPORARY_REDIRECT.getStatusCode());
        assertThat(response.getLocation().toString()).isEqualTo("https://erstatter.nav.no/fpsak/#?errormessage=" + feilmelding);
    }

    @Test
    public void skal_lage_url_med_saksnummer_og_behandlingId_når_oppgave_finnes_og_sakId_ikke_finnes_i_url() {
        var behandlingId = 10L;
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, null, null, saksnummer);
        fagsak.setId(2L);

        var behandling = Behandling.forFørstegangssøknad(fagsak).build();

        behandling.setId(behandlingId);
        var kobling = new OppgaveBehandlingKobling(OppgaveÅrsak.BEHANDLE_SAK, "1", saksnummer, behandlingId);
        when(oppgaveRepo.hentOppgaveBehandlingKobling("1")).thenReturn(Optional.of(kobling));
        when(fagsakRepo.finnEksaktFagsak(2)).thenReturn(fagsak);
        when(behandlingRepository.hentBehandling(behandling.getId())).thenReturn(behandling);

        var response = tjeneste.doRedirect(new OppgaveIdDto("1"), null);
        assertThat(response.getStatus()).isEqualTo(Response.Status.TEMPORARY_REDIRECT.getStatusCode());
        assertThat(response.getLocation().toString())
                .isEqualTo("https://erstatter.nav.no/fpsak/fagsak/" + saksnummer.getVerdi() + "/behandling/" + behandling.getUuid().toString() + "/?punkt=default&fakta=default");
    }

    @Test
    public void skal_lage_url_med_saksnummer_når_oppgave_ikke_finnes() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, null, null, saksnummer);
        fagsak.setId(12L);
        when(fagsakRepo.hentSakGittSaksnummer(saksnummer)).thenReturn(Optional.of(fagsak));
        when(fagsakRepo.finnUnikFagsak(12L)).thenReturn(Optional.of(fagsak));

        var response = tjeneste.doRedirect(new OppgaveIdDto("1"), new SaksnummerDto(saksnummer));
        assertThat(response.getStatus()).isEqualTo(Response.Status.TEMPORARY_REDIRECT.getStatusCode());
        assertThat(response.getLocation().toString()).isEqualTo("https://erstatter.nav.no/fpsak/fagsak/" + saksnummer.getVerdi() + "/");
    }

    @Test
    public void skal_lage_url_med_behandlingsid_og_saksnummer_når_oppgave_finnes() {
        var behandlingId = 11L;
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, null, null, saksnummer);
        fagsak.setId(5L);
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();

        behandling.setId(behandlingId);
        var kobling = new OppgaveBehandlingKobling(OppgaveÅrsak.BEHANDLE_SAK, "1", saksnummer, behandlingId);
        when(fagsakRepo.hentSakGittSaksnummer(saksnummer)).thenReturn(Optional.of(fagsak));
        when(fagsakRepo.finnEksaktFagsak(5)).thenReturn(fagsak);
        when(behandlingRepository.hentBehandling(behandling.getId())).thenReturn(behandling);

        when(oppgaveRepo.hentOppgaveBehandlingKobling("1", saksnummer)).thenReturn(Optional.of(kobling));

        var response = tjeneste.doRedirect(new OppgaveIdDto("1"), new SaksnummerDto(saksnummer));
        assertThat(response.getStatus()).isEqualTo(Response.Status.TEMPORARY_REDIRECT.getStatusCode());
        assertThat(response.getLocation().toString())
                .isEqualTo("https://erstatter.nav.no/fpsak/fagsak/" + saksnummer.getVerdi() + "/behandling/" + behandling.getUuid().toString() + "/?punkt=default&fakta=default");
    }

    @Test
    public void skal_lage_url_med_saksnummer_når_oppgave_ikke_oppgitt() {
        var saksnummer = new Saksnummer("22");
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, null, null, saksnummer);
        when(fagsakRepo.hentSakGittSaksnummer(saksnummer)).thenReturn(Optional.of(fagsak));

        var responseSnr = tjeneste.doRedirect(null, new SaksnummerDto(saksnummer));
        assertThat(responseSnr.getStatus()).isEqualTo(Response.Status.TEMPORARY_REDIRECT.getStatusCode());
        assertThat(responseSnr.getLocation().toString()).isEqualTo("https://erstatter.nav.no/fpsak/fagsak/22/");

    }
}

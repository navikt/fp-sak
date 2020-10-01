package no.nav.foreldrepenger.web.app.oppgave;

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKoblingRepository;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt;

@Path("")
@ApplicationScoped
public class OppgaveRedirectTjeneste {

    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository;
    private FagsakRepository fagsakRepository;
    private RedirectFactory redirectFactory; // For å kunne endre til alternativ implementasjon på Jetty

    public OppgaveRedirectTjeneste() {
    }

    @Inject
    public OppgaveRedirectTjeneste(OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository,
            FagsakRepository fagsakRepository, RedirectFactory redirectFactory) {
        this.oppgaveBehandlingKoblingRepository = oppgaveBehandlingKoblingRepository;
        this.fagsakRepository = fagsakRepository;
        this.redirectFactory = redirectFactory;
    }

    @GET
    @Operation(description = "redirect til oppgave", tags = "redirect", hidden = true)
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response doRedirect(@QueryParam("oppgaveId") @Valid OppgaveIdDto oppgaveId,
            @QueryParam("sakId") @Valid SaksnummerDto saksnummerDto) {
        OppgaveRedirectData data = OppgaveRedirectData.hent(oppgaveBehandlingKoblingRepository, fagsakRepository, oppgaveId, saksnummerDto);
        String url = redirectFactory.lagRedirect(data);
        Response.ResponseBuilder responser = Response.temporaryRedirect(URI.create(url));
        responser.encoding("UTF-8");
        return responser.build();
    }

}

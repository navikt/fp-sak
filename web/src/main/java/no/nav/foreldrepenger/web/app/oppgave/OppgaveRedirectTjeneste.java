package no.nav.foreldrepenger.web.app.oppgave;

import java.net.URI;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKoblingRepository;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerAbacSupplier;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path("")
@ApplicationScoped
public class OppgaveRedirectTjeneste {

    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private RedirectFactory redirectFactory; // For å kunne endre til alternativ implementasjon på Jetty

    public OppgaveRedirectTjeneste() {
    }

    @Inject
    public OppgaveRedirectTjeneste(OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository,
                                   FagsakRepository fagsakRepository,
                                   BehandlingRepository behandlingRepository,
                                   RedirectFactory redirectFactory) {
        this.oppgaveBehandlingKoblingRepository = oppgaveBehandlingKoblingRepository;
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.redirectFactory = redirectFactory;
    }

    @GET
    @Operation(description = "redirect til oppgave", tags = "redirect", hidden = true)
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public Response doRedirect(@QueryParam("oppgaveId") @TilpassetAbacAttributt(supplierClass = OppgaveSupplier.class) @Valid OppgaveIdDto oppgaveId,
                               @QueryParam("sakId") @TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class) @Valid SaksnummerDto saksnummerDto) {
        var data = OppgaveRedirectData.hent(oppgaveBehandlingKoblingRepository, fagsakRepository,
            behandlingRepository, oppgaveId, saksnummerDto);
        var url = redirectFactory.lagRedirect(data);
        var responser = Response.temporaryRedirect(URI.create(url));
        responser.encoding("UTF-8");
        return responser.build();
    }

    public static class OppgaveSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (OppgaveIdDto) obj;
            return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.OPPGAVE_ID, req.getVerdi());
        }
    }

}

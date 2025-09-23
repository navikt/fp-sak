package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak;

import static no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers.UuidAbacDataSupplier;

import java.util.List;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.app.OppgaveDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.dto.OppgaveDto;
import no.nav.foreldrepenger.web.app.tjenester.tilbake.TilbakeRestTjeneste;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path(OppgaverRestTjeneste.BASE_PATH)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class OppgaverRestTjeneste {

    static final String BASE_PATH = "/behandling/vedtak";
    private static final String HENT_OPPGAVER_PART_PATH = "/hent-oppgaver";
    public static final String HENT_OPPGAVER_PATH = BASE_PATH + HENT_OPPGAVER_PART_PATH;
    private static final String FERDIGSTILL_OPPGAVE_PART_PATH = "/ferdigstill-oppgave";
    public static final String FERDIGSTILL_OPPGAVE_PATH = BASE_PATH + FERDIGSTILL_OPPGAVE_PART_PATH;

    private BehandlingRepository behandlingRepository;
    private OppgaveDtoTjeneste oppgaveDtoTjeneste;
    private OppgaveTjeneste oppgaveTjeneste;

    @Inject
    public OppgaverRestTjeneste(BehandlingRepository behandlingRepository, OppgaveDtoTjeneste oppgaveDtoTjeneste, OppgaveTjeneste oppgaveTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.oppgaveDtoTjeneste = oppgaveDtoTjeneste;
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    OppgaverRestTjeneste() {
        // for CDI proxy
    }

    @GET
    @Path(HENT_OPPGAVER_PART_PATH)
    @Operation(description = "Henter åpne oppgaver", tags = "vedtak")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    public List<OppgaveDto> hentOppgaver(@TilpassetAbacAttributt(supplierClass = UuidAbacDataSupplier.class) @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        return oppgaveDtoTjeneste.mapTilDto(behandling.getAktørId());
    }

    @POST
    @Path(FERDIGSTILL_OPPGAVE_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Ferdigstiller valgt oppgave", tags = "vedtak")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    public Response ferdigstillOppgave(@TilpassetAbacAttributt(supplierClass = OppgaveAbacDataSupplier.class) @NotNull @Parameter(description = "OppgaveId for oppgave som skal ferdigstilles") @Valid OppgaveDto.OppgaveId oppgaveId) {
        oppgaveTjeneste.ferdigstillOppgave(oppgaveId.id());
        return Response.ok().build();
    }

    public static class OppgaveAbacDataSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            return AbacDataAttributter.opprett();
        }
    }
}

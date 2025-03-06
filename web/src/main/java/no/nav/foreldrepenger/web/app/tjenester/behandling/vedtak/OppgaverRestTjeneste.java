package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak;

import static no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers.UuidAbacDataSupplier;

import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.app.OppgaveDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.dto.OppgaveDto;
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

    private BehandlingRepository behandlingRepository;
    private OppgaveDtoTjeneste oppgaveDtoTjeneste;

    @Inject
    public OppgaverRestTjeneste(BehandlingRepository behandlingRepository, OppgaveDtoTjeneste oppgaveDtoTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.oppgaveDtoTjeneste = oppgaveDtoTjeneste;
    }

    OppgaverRestTjeneste() {
        // for CDI proxy
    }

    @GET
    @Path(HENT_OPPGAVER_PART_PATH)
    @Operation(description = "Henter åpne oppgaver", tags = "vedtak")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public List<OppgaveDto> hentOppgaver(@TilpassetAbacAttributt(supplierClass = UuidAbacDataSupplier.class) @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        if (!FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType())) {
            return oppgaveDtoTjeneste.mapTilDto(behandling.getAktørId());
        }
        return Collections.emptyList();
    }
}

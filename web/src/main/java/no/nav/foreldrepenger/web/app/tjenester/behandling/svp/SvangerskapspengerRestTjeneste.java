package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

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
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path(SvangerskapspengerRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class SvangerskapspengerRestTjeneste {

    static final String BASE_PATH = "/behandling/svangerskapspenger";
    private static final String TILRETTELEGGING_V2_PART_PATH = "/tilrettelegging-v2";
    public static final String TILRETTELEGGING_V2_PATH = BASE_PATH + TILRETTELEGGING_V2_PART_PATH;

    private SvangerskapspengerTjeneste svangerskapspengerTjeneste;
    private BehandlingRepository behandlingRepository;

    public SvangerskapspengerRestTjeneste() {
        // Creatively Disorganised Illusions
    }

    @Inject
    public SvangerskapspengerRestTjeneste(SvangerskapspengerTjeneste svangerskapspengerTjeneste, BehandlingRepository behandlingRepository) {
        this.svangerskapspengerTjeneste = svangerskapspengerTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path(TILRETTELEGGING_V2_PART_PATH)
    @Operation(description = "Hent informasjon om tilretteleggingbehov ved svangerskapspenger", summary = "Returnerer termindato og liste med tilretteleggingsinfo pr. arbeidsforhold ved svangerskapspenger", tags = "svangerskapspenger")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    public SvpTilretteleggingDto tilrettelegging(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
        @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        return svangerskapspengerTjeneste.hentTilrettelegging(behandling);
    }
}

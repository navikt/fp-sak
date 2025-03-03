package no.nav.foreldrepenger.web.app.tjenester.formidling.beregningsgrunnlag;

import java.util.Optional;

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
import jakarta.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

/**
 * Beregningsgrunnlag knyttet til en behandling.
 */
@ApplicationScoped
@Path(BeregningsgrunnlagFormidlingRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class BeregningsgrunnlagFormidlingRestTjeneste {

    static final String BASE_PATH = "/formidling";
    private static final String BEREGNINGSGRUNNLAG_PART_PATH = "/beregningsgrunnlag/v2";
    public static final String BEREGNINGSGRUNNLAG_PATH = BASE_PATH + BEREGNINGSGRUNNLAG_PART_PATH;

    private BehandlingRepository behandlingRepository;
    private BeregningTjeneste beregningTjeneste;

    public BeregningsgrunnlagFormidlingRestTjeneste() {
        // CDI
    }

    @Inject
    public BeregningsgrunnlagFormidlingRestTjeneste(BehandlingRepository behandlingRepository,
                                                    BeregningTjeneste beregningTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.beregningTjeneste = beregningTjeneste;
    }

    @GET
    @Operation(description = "Hent beregningsgrunnlag for angitt behandling for formidlingsbruk", summary = "Returnerer beregningsgrunnlag for behandling for formidlingsbruk.", tags = "formidling")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    @Path(BEREGNINGSGRUNNLAG_PART_PATH)
    public Response hentBeregningsgrunnlagFormidlingV2(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
                                                     @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var uid = Optional.ofNullable(uuidDto.getBehandlingUuid());
        var dto = uid.flatMap(behandlingRepository::hentBehandlingHvisFinnes)
            .map(BehandlingReferanse::fra)
            .flatMap(beh -> beregningTjeneste.hent(beh))
            .flatMap(bggr -> new BeregningsgrunnlagFormidlingV2DtoTjeneste(bggr).map());

        if (dto.isEmpty()) {
            var responseBuilder = Response.ok();
            return responseBuilder.build();
        }
        var responseBuilder = Response.ok(dto.get());
        return responseBuilder.build();
    }
}

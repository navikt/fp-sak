package no.nav.foreldrepenger.web.app.tjenester.formidling.beregningsgrunnlag;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

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
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;

    public BeregningsgrunnlagFormidlingRestTjeneste() {
        // CDI
    }

    @Inject
    public BeregningsgrunnlagFormidlingRestTjeneste(BehandlingRepository behandlingRepository,
                                                    BeregningsgrunnlagRepository beregningsgrunnlagRepository) {
        this.behandlingRepository = behandlingRepository;
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
    }

    @GET
    @Operation(description = "Hent beregningsgrunnlag for angitt behandling for formidlingsbruk", summary = ("Returnerer beregningsgrunnlag for behandling for formidlingsbruk."), tags = "formidling")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    @Path(BEREGNINGSGRUNNLAG_PART_PATH)
    public Response hentBeregningsgrunnlagFormidlingV2(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
                                                     @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var uid = Optional.ofNullable(uuidDto.getBehandlingUuid());
        var dto = uid.flatMap(behandlingRepository::hentBehandlingHvisFinnes)
            .flatMap(beh -> beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(beh.getId()))
            .flatMap(bggr -> new BeregningsgrunnlagFormidlingV2DtoTjeneste(bggr).map());

        if (dto.isEmpty()) {
            var responseBuilder = Response.ok();
            return responseBuilder.build();
        }
        var responseBuilder = Response.ok(dto.get());
        return responseBuilder.build();
    }
}

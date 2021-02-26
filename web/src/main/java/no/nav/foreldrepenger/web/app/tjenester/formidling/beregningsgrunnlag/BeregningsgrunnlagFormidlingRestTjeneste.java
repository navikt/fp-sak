package no.nav.foreldrepenger.web.app.tjenester.formidling.beregningsgrunnlag;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandling.UuidDto;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.web.app.tjenester.formidling.beregningsgrunnlag.dto.BeregningsgrunnlagDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

/**
 * Beregningsgrunnlag knyttet til en behandling.
 */
@ApplicationScoped
@Path(BeregningsgrunnlagFormidlingRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class BeregningsgrunnlagFormidlingRestTjeneste {

    static final String BASE_PATH = "/formidling";
    private static final String BEREGNINGSGRUNNLAG_PART_PATH = "/beregningsgrunnlag";
    public static final String BEREGNINGSGRUNNLAG_PATH = BASE_PATH + BEREGNINGSGRUNNLAG_PART_PATH;

    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;

    public BeregningsgrunnlagFormidlingRestTjeneste() {
        // for resteasy
    }

    @Inject
    public BeregningsgrunnlagFormidlingRestTjeneste(BehandlingRepository behandlingRepository,
                                                    BeregningsgrunnlagRepository beregningsgrunnlagRepository) {
        this.behandlingRepository = behandlingRepository;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent beregningsgrunnlag for angitt behandling for formidlingsbruk", summary = ("Returnerer beregningsgrunnlag for behandling for formidlingsbruk."), tags = "formidling")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    @Path(BEREGNINGSGRUNNLAG_PART_PATH)
    @Deprecated

    public Response hentBeregningsgrunnlag(
        @NotNull @Parameter(description = "BehandlingUid for aktuell behandling") @Valid UuidDto uuidDto) {
        Optional<UUID> uid = Optional.ofNullable(uuidDto.getBehandlingUuid());
        Optional<BeregningsgrunnlagDto> dto = uid.flatMap(behandlingRepository::hentBehandlingHvisFinnes)
            .flatMap(beh -> beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(beh.getId()))
            .flatMap(bggr -> new BeregningsgrunnlagFormidlingDtoTjeneste(bggr).map());

        if (dto.isEmpty()) {
            Response.ResponseBuilder responseBuilder = Response.ok();
            return responseBuilder.build();
        }
        Response.ResponseBuilder responseBuilder = Response.ok(dto.get());
        return responseBuilder.build();
    }
}

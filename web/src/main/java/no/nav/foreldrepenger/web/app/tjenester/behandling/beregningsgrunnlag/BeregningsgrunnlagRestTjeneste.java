package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

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
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
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
@Path(BeregningsgrunnlagRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class BeregningsgrunnlagRestTjeneste {

    static final String BASE_PATH = "/behandling";
    private static final String BEREGNINGSGRUNNLAG_PART_PATH = "/beregningsgrunnlag";
    public static final String BEREGNINGSGRUNNLAG_PATH = BASE_PATH + BEREGNINGSGRUNNLAG_PART_PATH;

    private BehandlingRepository behandlingRepository;
    private OpptjeningRepository opptjeningRepository;
    private BeregningTjeneste beregningTjeneste;

    public BeregningsgrunnlagRestTjeneste() {
        // CDI
    }

    @Inject
    public BeregningsgrunnlagRestTjeneste(BehandlingRepository behandlingRepository,
                                          OpptjeningRepository opptjeningRepository,
                                          BeregningTjeneste beregningTjeneste) {
        this.opptjeningRepository = opptjeningRepository;
        this.behandlingRepository = behandlingRepository;
        this.beregningTjeneste = beregningTjeneste;
    }

    @GET
    @Operation(description = "Hent beregningsgrunnlag for angitt behandling", summary = "Returnerer beregningsgrunnlag for behandling.", tags = "beregningsgrunnlag")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    @Path(BEREGNINGSGRUNNLAG_PART_PATH)
    public BeregningsgrunnlagDto hentBeregningsgrunnlag(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        var opptjening = opptjeningRepository.finnOpptjening(behandling.getId());
        if (!opptjening.map(Opptjening::erOpptjeningPeriodeVilkårOppfylt).orElse(Boolean.FALSE)) {
            return null;
        }
        return beregningTjeneste.hentGuiDto(BehandlingReferanse.fra(behandling)).orElse(null);
    }
}

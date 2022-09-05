package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagDto;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagGUIInputFelles;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.rest.BeregningDtoTjeneste;
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
    private BeregningDtoTjeneste beregningDtoTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private OpptjeningRepository opptjeningRepository;
    private BeregningsgrunnlagInputProvider inputTjenesteProvider;

    public BeregningsgrunnlagRestTjeneste() {
        // CDI
    }

    @Inject
    public BeregningsgrunnlagRestTjeneste(BehandlingRepository behandlingRepository,
            OpptjeningRepository opptjeningRepository,
            BeregningsgrunnlagInputProvider inputTjenesteProvider,
            BeregningDtoTjeneste beregningDtoTjeneste,
            InntektArbeidYtelseTjeneste iayTjeneste) {
        this.opptjeningRepository = opptjeningRepository;
        this.inputTjenesteProvider = inputTjenesteProvider;
        this.beregningDtoTjeneste = beregningDtoTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.iayTjeneste = iayTjeneste;
    }

    @GET
    @Operation(description = "Hent beregningsgrunnlag for angitt behandling", summary = ("Returnerer beregningsgrunnlag for behandling."), tags = "beregningsgrunnlag")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    @Path(BEREGNINGSGRUNNLAG_PART_PATH)
    public BeregningsgrunnlagDto hentBeregningsgrunnlag(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        final var opptjening = opptjeningRepository.finnOpptjening(behandling.getId());
        if (!opptjening.map(Opptjening::erOpptjeningPeriodeVilkårOppfylt).orElse(Boolean.FALSE)) {
            return null;
        }

        var iayGrunnlagOpt = iayTjeneste.finnGrunnlag(behandling.getId());
        return iayGrunnlagOpt.flatMap(iayGrunnlag -> {
            var input = getInputTjeneste(behandling.getFagsakYtelseType()).lagInput(behandling, iayGrunnlag);
            if (input.isPresent()) {
                return beregningDtoTjeneste.lagBeregningsgrunnlagDto(input.get());
            }
            return Optional.empty();
        }).orElse(null);
    }

    private BeregningsgrunnlagGUIInputFelles getInputTjeneste(FagsakYtelseType ytelseType) {
        return inputTjenesteProvider.getRestInputTjeneste(ytelseType);
    }

}

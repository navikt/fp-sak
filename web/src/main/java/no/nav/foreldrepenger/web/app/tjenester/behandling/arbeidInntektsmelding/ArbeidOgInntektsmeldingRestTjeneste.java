package no.nav.foreldrepenger.web.app.tjenester.behandling.arbeidInntektsmelding;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.dto.arbeidInntektsmelding.ArbeidOgInntektsmeldingDto;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

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

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Path(ArbeidOgInntektsmeldingRestTjeneste.BASE_PATH)
@Transactional
public class ArbeidOgInntektsmeldingRestTjeneste {
    static final String BASE_PATH = "/behandling";
    private static final String ARBEID_OG_INNTEKTSMELDING_PART_PATH = "/arbeid-inntektsmelding";
    public static final String ARBEID_OG_INNTEKTSMELDING_PATH = BASE_PATH + ARBEID_OG_INNTEKTSMELDING_PART_PATH;

    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private ArbeidOgInntektsmeldingDtoTjeneste arbeidOgInntektsmeldingDtoTjeneste;

    ArbeidOgInntektsmeldingRestTjeneste() {
        // CDI
    }

    @Inject
    public ArbeidOgInntektsmeldingRestTjeneste(BehandlingRepository behandlingRepository,
                                               SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                               ArbeidOgInntektsmeldingDtoTjeneste arbeidOgInntektsmeldingDtoTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.arbeidOgInntektsmeldingDtoTjeneste = arbeidOgInntektsmeldingDtoTjeneste;
    }

    @GET
    @Path(ARBEID_OG_INNTEKTSMELDING_PART_PATH)
    @Operation(description = "Hent informasjon arbeidsforhold og tilhørende inntektsmeldinger", summary = ("Returnerer info om arbeidsforhold og inntektsmeldinger tilknyttet saken."), tags = "arbeid-intektsmelding", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer ArbeidOgInntektsmeldingDto, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ArbeidOgInntektsmeldingDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public ArbeidOgInntektsmeldingDto getInntektArbeidYtelser(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
                                                          @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        var ref = BehandlingReferanse.fra(behandling, stp);
        return arbeidOgInntektsmeldingDtoTjeneste.lagDto(ref);
    }

}

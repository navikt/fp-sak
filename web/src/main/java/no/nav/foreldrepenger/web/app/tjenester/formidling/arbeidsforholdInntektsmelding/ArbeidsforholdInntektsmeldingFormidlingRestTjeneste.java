package no.nav.foreldrepenger.web.app.tjenester.formidling.arbeidsforholdInntektsmelding;

import java.util.Collections;
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
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingMangelTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
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
@Path(ArbeidsforholdInntektsmeldingFormidlingRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class ArbeidsforholdInntektsmeldingFormidlingRestTjeneste {

    static final String BASE_PATH = "/formidling/arbeidInntektsmelding";
    private static final String INNTEKTSMELDING_STATUS_PART_PATH = "/inntektsmelding-status";
    public static final String INNTEKTSMELDING_STATUS_PATH = BASE_PATH + INNTEKTSMELDING_STATUS_PART_PATH;

    private BehandlingRepository behandlingRepository;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    public ArbeidsforholdInntektsmeldingFormidlingRestTjeneste() {
        // CDI
    }

    @Inject
    public ArbeidsforholdInntektsmeldingFormidlingRestTjeneste(BehandlingRepository behandlingRepository,
                                                               InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                               ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste,
                                                               SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.arbeidsforholdInntektsmeldingMangelTjeneste = arbeidsforholdInntektsmeldingMangelTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    @GET
    @Operation(description = "Hent status for inntektsmeldinger for angitt behandling for formidlingsbruk", summary = "Returnerer status for inntektsmeldinger for behandling for formidlingsbruk.", tags = "formidling")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    @Path(INNTEKTSMELDING_STATUS_PART_PATH)
    public Response hentStatusInntektsmeldinger(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
                                                     @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var uid = Optional.ofNullable(uuidDto.getBehandlingUuid());
        var behandling = uid.flatMap(u -> behandlingRepository.hentBehandlingHvisFinnes(u));
        if (behandling.isEmpty()) {
            var responseBuilder = Response.noContent();
            return responseBuilder.build();
        }
        var ref = BehandlingReferanse.fra(behandling.get());
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(ref.behandlingId());
        var alleYrkesaktiviteter = inntektArbeidYtelseTjeneste.hentGrunnlag(ref.behandlingId()).getAktørArbeidFraRegister(ref.aktørId())
            .map(AktørArbeid::hentAlleYrkesaktiviteter)
            .orElse(Collections.emptyList());
        var arbeidsforholdInntektsmeldingStatuser = arbeidsforholdInntektsmeldingMangelTjeneste.finnStatusForInntektsmeldingArbeidsforhold(ref, stp);


        var arbeidsforholdInntektsmeldinger = ArbeidsforholdInntektsmeldingDtoTjeneste.mapInntektsmeldingStatus(arbeidsforholdInntektsmeldingStatuser,
            alleYrkesaktiviteter, stp.getUtledetSkjæringstidspunkt());

        var responseBuilder = Response.ok(arbeidsforholdInntektsmeldinger);
        return responseBuilder.build();
    }
}

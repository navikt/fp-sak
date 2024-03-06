package no.nav.foreldrepenger.web.app.tjenester.formidling.arbeidsforholdInntektsmelding;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

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

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.abakus.AbakusTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InntektsmeldingRegisterTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.kontrakter.fpsak.inntektsmeldinger.ArbeidsforholdInntektsmeldinger;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
    private InntektsmeldingRegisterTjeneste inntektsmeldingRegisterTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    public ArbeidsforholdInntektsmeldingFormidlingRestTjeneste() {
        // CDI
    }

    @Inject
    public ArbeidsforholdInntektsmeldingFormidlingRestTjeneste(BehandlingRepository behandlingRepository,
                                                               InntektsmeldingRegisterTjeneste inntektsmeldingRegisterTjeneste,
                                                               InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                               SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.inntektsmeldingRegisterTjeneste = inntektsmeldingRegisterTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    @GET
    @Operation(description = "Hent status for inntektsmeldinger for angitt behandling for formidlingsbruk", summary = "Returnerer status for inntektsmeldinger for behandling for formidlingsbruk.", tags = "formidling")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    @Path(INNTEKTSMELDING_STATUS_PART_PATH)
    public Response hentStatusInntektsmeldinger(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
                                                     @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var uid = Optional.ofNullable(uuidDto.getBehandlingUuid());
        var behandling = uid.flatMap(u -> behandlingRepository.hentBehandlingHvisFinnes(u));
        if (behandling.isEmpty()) {
            var responseBuilder = Response.noContent();
            return responseBuilder.build();
        }
        var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.get().getId());
        var ref = BehandlingReferanse.fra(behandling.get(), skjæringstidspunkter);
        var alleYrkesaktiviteter = inntektArbeidYtelseTjeneste.hentGrunnlag(ref.behandlingId()).getAktørArbeidFraRegister(ref.aktørId())
            .map(AktørArbeid::hentAlleYrkesaktiviteter)
            .orElse(Collections.emptyList());
        var allePåkrevde = inntektsmeldingRegisterTjeneste.hentAllePåkrevdeInntektsmeldinger(ref);
        var alleManglende = inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(ref, false);

        var arbeidsforholdInntektsmeldinger = ArbeidsforholdInntektsmeldingDtoTjeneste.mapInntektsmeldingStatus(allePåkrevde, alleManglende,
            alleYrkesaktiviteter, ref.getUtledetSkjæringstidspunkt());

        var responseBuilder = Response.ok(arbeidsforholdInntektsmeldinger);
        return responseBuilder.build();
    }
}

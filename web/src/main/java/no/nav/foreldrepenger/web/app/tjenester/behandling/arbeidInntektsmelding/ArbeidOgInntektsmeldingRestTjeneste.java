package no.nav.foreldrepenger.web.app.tjenester.behandling.arbeidInntektsmelding;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdKomplettVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingMangelTjeneste;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ManglendeOpplysningerVurderingDto;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ManueltArbeidsforholdDto;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto.ArbeidOgInntektsmeldingDto;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto.InntektsmeldingDto;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.tilganger.AnsattInfoKlient;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsutredningTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingIdVersjonDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Path(ArbeidOgInntektsmeldingRestTjeneste.BASE_PATH)
@Transactional
public class ArbeidOgInntektsmeldingRestTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(ArbeidOgInntektsmeldingRestTjeneste.class);

    static final String BASE_PATH = "/behandling";
    private static final String ARBEID_OG_INNTEKTSMELDING_PART_PATH = "/arbeid-inntektsmelding";
    private static final String LAGRE_VURDERING_PART_PATH = "/arbeid-inntektsmelding/lagre-vurdering";
    private static final String REGISTRER_ARBEIDSFORHOLD_PART_PATH = "/arbeid-inntektsmelding/lagre-arbeidsforhold";
    private static final String ÅPNE_FOR_NY_VURDERING_PART_PATH = "/arbeid-inntektsmelding/apne-for-ny-vurdering";
    private static final String HENT_ALLE_INNTEKTSMELDINGER_PART_PATH = "/arbeid-inntektsmelding/alle-inntektsmeldinger";
    public static final String ARBEID_OG_INNTEKTSMELDING_PATH = BASE_PATH + ARBEID_OG_INNTEKTSMELDING_PART_PATH;
    public static final String REGISTRER_ARBEIDSFORHOLD_PATH = BASE_PATH + REGISTRER_ARBEIDSFORHOLD_PART_PATH;
    public static final String LAGRE_VURDERING_PATH = BASE_PATH + LAGRE_VURDERING_PART_PATH;
    public static final String ÅPNE_FOR_NY_VURDERING_PATH = BASE_PATH + ÅPNE_FOR_NY_VURDERING_PART_PATH;
    public static final String HENT_ALLE_INNTEKTSMELDINGER_PATH = BASE_PATH + HENT_ALLE_INNTEKTSMELDINGER_PART_PATH;

    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private ArbeidOgInntektsmeldingDtoTjeneste arbeidOgInntektsmeldingDtoTjeneste;
    private ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste;
    private ArbeidOgInntektsmeldingProsessTjeneste arbeidOgInntektsmeldingProsessTjeneste;
    private AnsattInfoKlient ansattInfoKlient;
    private BehandlingsutredningTjeneste behandlingutredningTjeneste;

    ArbeidOgInntektsmeldingRestTjeneste() {
        // CDI
    }

    @Inject
    public ArbeidOgInntektsmeldingRestTjeneste(BehandlingRepository behandlingRepository,
                                               SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                               ArbeidOgInntektsmeldingDtoTjeneste arbeidOgInntektsmeldingDtoTjeneste,
                                               ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste,
                                               ArbeidOgInntektsmeldingProsessTjeneste arbeidOgInntektsmeldingProsessTjeneste,
                                               AnsattInfoKlient ansattInfoKlient,
                                               BehandlingsutredningTjeneste behandlingutredningTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.arbeidOgInntektsmeldingDtoTjeneste = arbeidOgInntektsmeldingDtoTjeneste;
        this.arbeidsforholdInntektsmeldingMangelTjeneste = arbeidsforholdInntektsmeldingMangelTjeneste;
        this.arbeidOgInntektsmeldingProsessTjeneste = arbeidOgInntektsmeldingProsessTjeneste;
        this.ansattInfoKlient = ansattInfoKlient;
        this.behandlingutredningTjeneste = behandlingutredningTjeneste;
    }

    @GET
    @Path(ARBEID_OG_INNTEKTSMELDING_PART_PATH)
    @Operation(description = "Hent informasjon arbeidsforhold og tilhørende inntektsmeldinger", summary = "Returnerer info om arbeidsforhold og inntektsmeldinger tilknyttet saken.", tags = "arbeid-intektsmelding", responses = {@ApiResponse(responseCode = "200", description = "Returnerer ArbeidOgInntektsmeldingDto, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ArbeidOgInntektsmeldingDto.class)))})
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    public ArbeidOgInntektsmeldingDto getArbeidOgInntektsmeldinger(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
                                                          @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        var ref = BehandlingReferanse.fra(behandling);
        var stp = safeSkjæringstidspunktOpptjening(behandling.getId());
        return arbeidOgInntektsmeldingDtoTjeneste.lagDto(ref, stp).orElse(null) ;
    }

    @GET
    @Path(HENT_ALLE_INNTEKTSMELDINGER_PART_PATH)
    @Operation(description = "Henter alle inntektsmeldinger som hører til en fagsak", summary = "Returnerer liste av alle inntektsmeldinger til saken.", tags = "arbeid-intektsmelding", responses = {@ApiResponse(responseCode = "200", description = "", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = InntektsmeldingDto.class)))})
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    public List<InntektsmeldingDto> getAlleInntektsmeldinger(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
                                                                   @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        var ref = BehandlingReferanse.fra(behandling);
        var stp = Optional.ofNullable(safeSkjæringstidspunktOpptjening(behandling.getId())).map(Skjæringstidspunkt::getUtledetSkjæringstidspunkt).orElse(null);

        return arbeidOgInntektsmeldingDtoTjeneste.hentAlleInntektsmeldingerForFagsak(ref, stp);
    }

    private Skjæringstidspunkt safeSkjæringstidspunktOpptjening(Long behandlingId) {
        try {
            return skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        } catch (Exception e) {
            return null;
        }
    }


    @POST
    @Path(LAGRE_VURDERING_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Lagre vurdering av arbeidsforhold som mangler inntektsmelding, eller inntektsmelding som mangler arbeidsforhold", summary =
        "Lagrer vurdering av manglende inntektsmelding for et enkelt arbeidsforhold, "
            + "eller manglende arbeidsforhold for en enkelt inntektsmelding.", tags = "arbeid-intektsmelding")
    @BeskyttetRessurs(actionType = ActionType.UPDATE, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Response lagreVurderingAvManglendeOpplysninger(@TilpassetAbacAttributt(supplierClass = ManglendeInntektsmeldingVurderingAbacDataSupplier.class)
                                                                   @NotNull @Parameter(description = "Vurdering av opplysning som mangler.") @Valid ManglendeOpplysningerVurderingDto manglendeOpplysningerVurderingDto) {
        LOG.info("Lagrer valg på behandling {}", manglendeOpplysningerVurderingDto.getBehandlingUuid());
        if (manglendeOpplysningerVurderingDto.getBehandlingVersjon() != null) {
            behandlingutredningTjeneste.kanEndreBehandling(behandlingRepository.hentBehandling(manglendeOpplysningerVurderingDto.getBehandlingUuid()), manglendeOpplysningerVurderingDto.getBehandlingVersjon());
        }
        var behandling = behandlingRepository.hentBehandling(manglendeOpplysningerVurderingDto.getBehandlingUuid());
        var ref = BehandlingReferanse.fra(behandling);
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());

        arbeidsforholdInntektsmeldingMangelTjeneste.lagreManglendeOpplysningerVurdering(ref, stp, manglendeOpplysningerVurderingDto);
        return Response.ok().build();
    }

    @POST
    @Path(REGISTRER_ARBEIDSFORHOLD_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Lagre registrering av arbeidsforhold", summary = "Lagrer registrering av arbeidsforhold fra saksbehandler", tags = "arbeid-intektsmelding")
    @BeskyttetRessurs(actionType = ActionType.UPDATE, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Response lagreManuelleArbeidsforhold(@TilpassetAbacAttributt(supplierClass = ManueltArbeidsforholdDtoAbacDataSupplier.class)
                                                 @NotNull @Parameter(description = "Registrering av arbeidsforhold.") @Valid ManueltArbeidsforholdDto manueltArbeidsforholdDto) {
        var behandling = behandlingRepository.hentBehandling(manueltArbeidsforholdDto.getBehandlingUuid());
        var ref = BehandlingReferanse.fra(behandling);
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());

        if (manueltArbeidsforholdDto.getBehandlingVersjon() != null) {
            behandlingutredningTjeneste.kanEndreBehandling(behandlingRepository.hentBehandling(manueltArbeidsforholdDto.getBehandlingUuid()), manueltArbeidsforholdDto.getBehandlingVersjon());
        }
        if (endringGjelderHelmanueltArbeidsforhold(manueltArbeidsforholdDto) && !erOverstyringLovlig()) {
            var msg = String.format(
                    "Feil: Prøve å gjøre endringer på et helmanuelt arbeidsforhold uten å være overstyrer på behandling %s", ref.behandlingId());
            throw new TekniskException("FP-657812", msg);
        }
        arbeidsforholdInntektsmeldingMangelTjeneste.lagreManuelleArbeidsforhold(ref, stp, manueltArbeidsforholdDto);
        return Response.ok().build();
    }

    @POST
    @Path(ÅPNE_FOR_NY_VURDERING_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Åpner behandling for endring av vurdering i arbeid og inntektsmelding, hvis dette er mulig.", summary = "Åpner behandling for endring ved å rulle saken tilbake til korrekt steg", tags = "arbeid-intektsmelding")
    @BeskyttetRessurs(actionType = ActionType.UPDATE, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Response åpneForEndring(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.BehandlingIdAbacDataSupplier.class)
                                                @NotNull @Parameter(description = "BehandlingUID og versjon på behadlingen.") @Valid BehandlingIdVersjonDto behandlingIdVersjonDto) {
        var behandling = behandlingRepository.hentBehandling(behandlingIdVersjonDto.getBehandlingUuid());
        if (behandling.harAksjonspunktMedType(AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD_INNTEKTSMELDING)) {
            arbeidOgInntektsmeldingProsessTjeneste.tillTilbakeOgOpprettAksjonspunkt(behandlingIdVersjonDto, false);
        } else if (erOverstyringLovlig()) {
            LOG.info("Legger til aksjonspunkt 5085 ved overstyring på behandling {}", behandlingIdVersjonDto.getBehandlingUuid());
            arbeidOgInntektsmeldingProsessTjeneste.tillTilbakeOgOpprettAksjonspunkt(behandlingIdVersjonDto, true);
        }
        return Response.ok().build();
    }

    private boolean erOverstyringLovlig() {
        return ansattInfoKlient.innloggetNavAnsatt().kanOverstyre();
    }

    private boolean endringGjelderHelmanueltArbeidsforhold(ManueltArbeidsforholdDto manueltArbeidsforholdDto) {
        return manueltArbeidsforholdDto.getVurdering().equals(ArbeidsforholdKomplettVurderingType.MANUELT_OPPRETTET_AV_SAKSBEHANDLER)
                || manueltArbeidsforholdDto.getVurdering().equals(ArbeidsforholdKomplettVurderingType.FJERN_FRA_BEHANDLINGEN);
    }

    public static class ManglendeInntektsmeldingVurderingAbacDataSupplier implements Function<Object, AbacDataAttributter> {
        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (ManglendeOpplysningerVurderingDto) obj;
            return AbacDataAttributter.opprett()
                .leggTil(AppAbacAttributtType.BEHANDLING_UUID, req.getBehandlingUuid());
        }
    }


    public static class ManueltArbeidsforholdDtoAbacDataSupplier implements Function<Object, AbacDataAttributter> {
        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (ManueltArbeidsforholdDto) obj;
            return AbacDataAttributter.opprett()
                .leggTil(AppAbacAttributtType.BEHANDLING_UUID, req.getBehandlingUuid());
        }
    }


}

package no.nav.foreldrepenger.web.app.tjenester.behandling.arbeidInntektsmelding;

import java.util.UUID;
import java.util.function.Function;

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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdKomplettVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingMangelTjeneste;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ManglendeOpplysningerVurderingDto;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ManueltArbeidsforholdDto;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto.ArbeidOgInntektsmeldingDto;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.tilganger.TilgangerTjeneste;
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
    public static final String ARBEID_OG_INNTEKTSMELDING_PATH = BASE_PATH + ARBEID_OG_INNTEKTSMELDING_PART_PATH;
    public static final String REGISTRER_ARBEIDSFORHOLD_PATH = BASE_PATH + REGISTRER_ARBEIDSFORHOLD_PART_PATH;
    public static final String LAGRE_VURDERING_PATH = BASE_PATH + LAGRE_VURDERING_PART_PATH;
    public static final String ÅPNE_FOR_NY_VURDERING_PATH = BASE_PATH + ÅPNE_FOR_NY_VURDERING_PART_PATH;

    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private ArbeidOgInntektsmeldingDtoTjeneste arbeidOgInntektsmeldingDtoTjeneste;
    private ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste;
    private ArbeidOgInntektsmeldingProsessTjeneste arbeidOgInntektsmeldingProsessTjeneste;
    private TilgangerTjeneste tilgangerTjeneste;

    ArbeidOgInntektsmeldingRestTjeneste() {
        // CDI
    }

    @Inject
    public ArbeidOgInntektsmeldingRestTjeneste(BehandlingRepository behandlingRepository,
                                               SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                               ArbeidOgInntektsmeldingDtoTjeneste arbeidOgInntektsmeldingDtoTjeneste,
                                               ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste,
                                               ArbeidOgInntektsmeldingProsessTjeneste arbeidOgInntektsmeldingProsessTjeneste,
                                               TilgangerTjeneste tilgangerTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.arbeidOgInntektsmeldingDtoTjeneste = arbeidOgInntektsmeldingDtoTjeneste;
        this.arbeidsforholdInntektsmeldingMangelTjeneste = arbeidsforholdInntektsmeldingMangelTjeneste;
        this.arbeidOgInntektsmeldingProsessTjeneste = arbeidOgInntektsmeldingProsessTjeneste;
        this.tilgangerTjeneste = tilgangerTjeneste;
    }

    @GET
    @Path(ARBEID_OG_INNTEKTSMELDING_PART_PATH)
    @Operation(description = "Hent informasjon arbeidsforhold og tilhørende inntektsmeldinger", summary = ("Returnerer info om arbeidsforhold og inntektsmeldinger tilknyttet saken."), tags = "arbeid-intektsmelding", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer ArbeidOgInntektsmeldingDto, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ArbeidOgInntektsmeldingDto.class)))
    })
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public ArbeidOgInntektsmeldingDto getArbeidOgInntektsmeldinger(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
                                                          @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var ref = lagReferanse(uuidDto.getBehandlingUuid());
        return arbeidOgInntektsmeldingDtoTjeneste.lagDto(ref).orElse(null);
    }


    @POST
    @Path(LAGRE_VURDERING_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Lagre vurdering av arbeidsforhold som mangler inntektsmelding, eller inntektsmelding som mangler arbeidsforhold", summary = ("Lagrer vurdering av manglende inntektsmelding for et enkelt arbeidsforhold, " +
        "eller manglende arbeidsforhold for en enkelt inntektsmelding."), tags = "arbeid-intektsmelding")
    @BeskyttetRessurs(actionType = ActionType.UPDATE, resourceType = ResourceType.FAGSAK)
    public Response lagreVurderingAvManglendeOpplysninger(@TilpassetAbacAttributt(supplierClass = ManglendeInntektsmeldingVurderingAbacDataSupplier.class)
                                                                   @NotNull @Parameter(description = "Vurdering av opplysning som mangler.") @Valid ManglendeOpplysningerVurderingDto manglendeOpplysningerVurderingDto) {
        var ref = lagReferanse(manglendeOpplysningerVurderingDto.getBehandlingUuid());
        arbeidsforholdInntektsmeldingMangelTjeneste.lagreManglendeOpplysningerVurdering(ref, manglendeOpplysningerVurderingDto);
        return Response.ok().build();
    }

    @POST
    @Path(REGISTRER_ARBEIDSFORHOLD_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Lagre registrering av arbeidsforhold", summary = ("Lagrer registrering av arbeidsforhold fra saksbehandler"), tags = "arbeid-intektsmelding")
    @BeskyttetRessurs(actionType = ActionType.UPDATE, resourceType = ResourceType.FAGSAK)
    public Response lagreManuelleArbeidsforhold(@TilpassetAbacAttributt(supplierClass = ManueltArbeidsforholdDtoAbacDataSupplier.class)
                                                 @NotNull @Parameter(description = "Registrering av arbeidsforhold.") @Valid ManueltArbeidsforholdDto manueltArbeidsforholdDto) {
        var ref = lagReferanse(manueltArbeidsforholdDto.getBehandlingUuid());
        if (endringGjelderHelmanueltArbeidsforhold(manueltArbeidsforholdDto) && !erOverstyringLovlig()) {
            var msg = String.format(
                    "Feil: Prøve å gjøre endringer på et helmanuelt arbeidsforhold uten å være overstyrer på behandling %s", ref.behandlingId());
            throw new TekniskException("FP-657812", msg);
        }
        arbeidsforholdInntektsmeldingMangelTjeneste.lagreManuelleArbeidsforhold(ref, manueltArbeidsforholdDto);
        return Response.ok().build();
    }

    @POST
    @Path(ÅPNE_FOR_NY_VURDERING_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Åpner behandling for endring av vurdering i arbeid og inntektsmelding, hvis dette er mulig.", summary = ("Åpner behandling for endring ved å rulle saken tilbake til korrekt steg"), tags = "arbeid-intektsmelding")
    @BeskyttetRessurs(actionType = ActionType.UPDATE, resourceType = ResourceType.FAGSAK)
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

    private BehandlingReferanse lagReferanse(UUID uuid) {
        var behandling = behandlingRepository.hentBehandling(uuid);
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        return BehandlingReferanse.fra(behandling, stp);
    }

    private boolean erOverstyringLovlig() {
        var innloggetBruker = tilgangerTjeneste.innloggetBruker();
        return innloggetBruker.kanOverstyre();
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

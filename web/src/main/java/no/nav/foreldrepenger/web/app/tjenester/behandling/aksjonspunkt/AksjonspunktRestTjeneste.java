package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.UPDATE;

import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

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
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandling.BehandlingIdDto;
import no.nav.foreldrepenger.behandling.UuidDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktKode;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.Totrinnsvurdering;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.Redirect;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@ApplicationScoped
@Transactional
@Path(AksjonspunktRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class AksjonspunktRestTjeneste {

    static final String BASE_PATH = "/behandling";
    private static final String AKSJONSPUNKT_OVERSTYR_PART_PATH = "/aksjonspunkt/overstyr";
    public static final String AKSJONSPUNKT_OVERSTYR_PATH = BASE_PATH + AKSJONSPUNKT_OVERSTYR_PART_PATH;
    private static final String AKSJONSPUNKT_PART_PATH = "/aksjonspunkt";
    public static final String AKSJONSPUNKT_PATH = BASE_PATH + AKSJONSPUNKT_PART_PATH;
    private static final String AKSJONSPUNKT_V2_PART_PATH = "/aksjonspunkt-v2";
    public static final String AKSJONSPUNKT_V2_PATH = BASE_PATH + AKSJONSPUNKT_V2_PART_PATH;
    private static final String AKSJONSPUNKT_RISIKO_PART_PATH = "/aksjonspunkt/risiko";
    public static final String AKSJONSPUNKT_RISIKO_PATH = BASE_PATH + AKSJONSPUNKT_RISIKO_PART_PATH;
    private static final String AKSJONSPUNKT_KONTROLLER_REVURDERING_PART_PATH = "/aksjonspunkt/kontroller-revurdering";
    public static final String AKSJONSPUNKT_KONTROLLER_REVURDERING_PATH = BASE_PATH + AKSJONSPUNKT_KONTROLLER_REVURDERING_PART_PATH;

    private AksjonspunktTjeneste applikasjonstjeneste;
    private BehandlingRepository behandlingRepository;
    private BehandlingsutredningTjeneste behandlingutredningTjeneste;
    private TotrinnTjeneste totrinnTjeneste;

    public AksjonspunktRestTjeneste() {
        // Bare for RESTeasy
    }

    @Inject
    public AksjonspunktRestTjeneste(
        AksjonspunktTjeneste aksjonpunktApplikasjonTjeneste,
        BehandlingRepository behandlingRepository,
        BehandlingsutredningTjeneste behandlingutredningTjeneste, TotrinnTjeneste totrinnTjeneste) {

        this.applikasjonstjeneste = aksjonpunktApplikasjonTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.behandlingutredningTjeneste = behandlingutredningTjeneste;
        this.totrinnTjeneste = totrinnTjeneste;
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(AKSJONSPUNKT_PART_PATH)
    @Operation(description = "Hent aksjonspunter for en behandling", tags = "aksjonspunkt", responses = {
            @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(uniqueItems = true, arraySchema = @Schema(implementation = Set.class), schema = @Schema(implementation = AksjonspunktDto.class)), mediaType = MediaType.APPLICATION_JSON))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response getAksjonspunkter(@NotNull @QueryParam("behandlingId") @Valid BehandlingIdDto behandlingIdDto) { // NOSONAR
        Long behandlingId = behandlingIdDto.getBehandlingId();
        Behandling behandling = behandlingId != null
                ? behandlingRepository.hentBehandling(behandlingId)
                : behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());
        Collection<Totrinnsvurdering> ttVurderinger = totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(behandling);
        Set<AksjonspunktDto> dto = AksjonspunktDtoMapper.lagAksjonspunktDto(behandling, ttVurderinger);
        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);
        return Response.ok(dto).cacheControl(cc).build();
    }

    @GET
    @Path(AKSJONSPUNKT_V2_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent aksjonspunter for en behandling", tags = "aksjonspunkt", responses = {
            @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(uniqueItems = true, arraySchema = @Schema(implementation = Set.class), schema = @Schema(implementation = AksjonspunktDto.class)), mediaType = MediaType.APPLICATION_JSON))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response getAksjonspunkter(@NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        return getAksjonspunkter(new BehandlingIdDto(uuidDto));
    }

    @GET
    @Path(AKSJONSPUNKT_RISIKO_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent risikoaksjonspunkt for en behandling", tags = "aksjonspunkt", responses = {
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = AksjonspunktDto.class), mediaType = MediaType.APPLICATION_JSON))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response getRisikoAksjonspunkt(@NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        Behandling behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        AksjonspunktDto dto = AksjonspunktDtoMapper.lagAksjonspunktDto(behandling, Collections.emptyList())
                .stream()
                .filter(ap -> AksjonspunktKodeDefinisjon.VURDER_FARESIGNALER_KODE.equals(ap.getDefinisjon().getKode()))
                .findFirst()
                .orElse(null);

        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);
        return Response.ok(dto).cacheControl(cc).build();
    }

    @GET
    @Path(AKSJONSPUNKT_KONTROLLER_REVURDERING_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Har behandling åpent kontroller revurdering aksjonspunkt", tags = "aksjonspunkt", responses = {
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Boolean.class), mediaType = MediaType.APPLICATION_JSON))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response erKontrollerRevurderingAksjonspunktÅpent(
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        Behandling behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        boolean harÅpentAksjonspunkt = behandling
                .harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.KONTROLLER_REVURDERINGSBEHANDLING_VARSEL_VED_UGUNST);
        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);
        return Response.ok(harÅpentAksjonspunkt).cacheControl(cc).build();
    }

    /**
     * Håndterer prosessering av aksjonspunkt og videre behandling.
     * <p>
     * MERK: Det skal ikke ligge spesifikke sjekker som avhenger av status på
     * behanlding, steg eller knytning til spesifikke aksjonspunkter idenne
     * tjenesten.
     *
     * @throws URISyntaxException
     */
    @POST
    @Path(AKSJONSPUNKT_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Lagre endringer gitt av aksjonspunktene og rekjør behandling fra gjeldende steg", tags = "aksjonspunkt")
    @BeskyttetRessurs(action = UPDATE, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response bekreft(
            @Parameter(description = "Liste over aksjonspunkt som skal bekreftes, inklusiv data som trengs for å løse de.") @Valid BekreftedeAksjonspunkterDto apDto)
            throws URISyntaxException { // NOSONAR

        Long behandlingId = apDto.getBehandlingId().getBehandlingId();
        Collection<BekreftetAksjonspunktDto> bekreftedeAksjonspunktDtoer = apDto.getBekreftedeAksjonspunktDtoer();

        Behandling behandling = behandlingId != null
                ? behandlingRepository.hentBehandling(behandlingId)
                : behandlingRepository.hentBehandling(apDto.getBehandlingId().getBehandlingUuid());

        behandlingutredningTjeneste.kanEndreBehandling(behandling.getId(), apDto.getBehandlingVersjon());

        validerBetingelserForAksjonspunkt(behandling, apDto.getBekreftedeAksjonspunktDtoer());

        applikasjonstjeneste.bekreftAksjonspunkter(bekreftedeAksjonspunktDtoer, behandling.getId());

        return Redirect.tilBehandlingPollStatus(behandling.getUuid());
    }

    /**
     * Oppretting og prosessering av aksjonspunkt som har type overstyringspunkt.
     * <p>
     * MERK: Det skal ikke ligge spesifikke sjekker som avhenger av status på
     * behanlding, steg eller knytning til spesifikke aksjonspunkter idenne
     * tjenesten.
     */
    @POST
    @Path(AKSJONSPUNKT_OVERSTYR_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Overstyrer stegene", tags = "aksjonspunkt")
    @BeskyttetRessurs(action = UPDATE, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response overstyr(@Parameter(description = "Liste over overstyring aksjonspunkter.") @Valid OverstyrteAksjonspunkterDto apDto) { // NOSONAR

        BehandlingIdDto behandlingIdDto = apDto.getBehandlingId();
        Long behandlingId = behandlingIdDto.getBehandlingId();
        Behandling behandling = behandlingId != null
                ? behandlingRepository.hentBehandling(behandlingId)
                : behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());

        behandlingutredningTjeneste.kanEndreBehandling(behandling.getId(), apDto.getBehandlingVersjon());

        validerBetingelserForAksjonspunkt(behandling, apDto.getOverstyrteAksjonspunktDtoer());

        applikasjonstjeneste.overstyrAksjonspunkter(apDto.getOverstyrteAksjonspunktDtoer(), behandling.getId());

        return Redirect.tilBehandlingPollStatus(behandling.getUuid());
    }

    private static void validerBetingelserForAksjonspunkt(Behandling behandling, Collection<? extends AksjonspunktKode> aksjonspunktDtoer) {
        // TODO (FC): skal ikke ha spesfikke pre-conditions inne i denne tjenesten
        // (sjekk på status FATTER_VEDTAK). Se
        // om kan håndteres annerledes.
        if (behandling.getStatus().equals(BehandlingStatus.FATTER_VEDTAK) && !erFatteVedtakAkpt(aksjonspunktDtoer)) {
            throw AksjonspunktRestTjenesteFeil.FACTORY.totrinnsbehandlingErStartet(String.valueOf(behandling.getId())).toException();
        }
    }

    private static boolean erFatteVedtakAkpt(Collection<? extends AksjonspunktKode> aksjonspunktDtoer) {
        return aksjonspunktDtoer.size() == 1 &&
                aksjonspunktDtoer.iterator().next().getKode().equals(AksjonspunktDefinisjon.FATTER_VEDTAK.getKode());
    }
}

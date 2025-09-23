package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import no.nav.foreldrepenger.web.app.tjenester.tilbake.TilbakeRestTjeneste;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktKode;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.person.verge.dto.AvklarVergeDto;
import no.nav.foreldrepenger.domene.vedtak.TotrinnTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.Redirect;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringDto;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@ApplicationScoped
@Transactional
@Path(AksjonspunktRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class AksjonspunktRestTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(AksjonspunktRestTjeneste.class);

    static final String BASE_PATH = "/behandling";
    private static final String AKSJONSPUNKT_OVERSTYR_PART_PATH = "/aksjonspunkt/overstyr";
    public static final String AKSJONSPUNKT_OVERSTYR_PATH = BASE_PATH + AKSJONSPUNKT_OVERSTYR_PART_PATH;
    private static final String AKSJONSPUNKT_BESLUTT_PART_PATH = "/aksjonspunkt/beslutt";
    public static final String AKSJONSPUNKT_BESLUTT_PATH = BASE_PATH + AKSJONSPUNKT_BESLUTT_PART_PATH;
    private static final String AKSJONSPUNKT_PART_PATH = "/aksjonspunkt";
    public static final String AKSJONSPUNKT_PATH = BASE_PATH + AKSJONSPUNKT_PART_PATH;
    private static final String AKSJONSPUNKT_V2_PART_PATH = "/aksjonspunkt-v2";
    public static final String AKSJONSPUNKT_V2_PATH = BASE_PATH + AKSJONSPUNKT_V2_PART_PATH;

    private AksjonspunktTjeneste applikasjonstjeneste;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BehandlingsutredningTjeneste behandlingutredningTjeneste;
    private TotrinnTjeneste totrinnTjeneste;

    public AksjonspunktRestTjeneste() {
        // Bare CDI
    }

    @Inject
    public AksjonspunktRestTjeneste(
        AksjonspunktTjeneste aksjonpunktApplikasjonTjeneste,
        BehandlingRepository behandlingRepository,
        BehandlingsresultatRepository behandlingsresultatRepository,
        BehandlingsutredningTjeneste behandlingutredningTjeneste, TotrinnTjeneste totrinnTjeneste) {

        this.applikasjonstjeneste = aksjonpunktApplikasjonTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.behandlingutredningTjeneste = behandlingutredningTjeneste;
        this.totrinnTjeneste = totrinnTjeneste;
    }

    @GET
    @Path(AKSJONSPUNKT_V2_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent aksjonspunter for en behandling", tags = "aksjonspunkt", responses = {
            @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(uniqueItems = true, arraySchema = @Schema(implementation = Set.class), schema = @Schema(implementation = AksjonspunktDto.class)), mediaType = MediaType.APPLICATION_JSON))
    })
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    public Response getAksjonspunkter(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
        @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        var behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).orElse(null);
        var ttVurderinger = totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(behandling.getId());
        var dto = AksjonspunktDtoMapper.lagAksjonspunktDto(behandling, behandlingsresultat, ttVurderinger);
        var cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);
        return Response.ok(dto).cacheControl(cc).build();
    }

    /**
     * Håndterer prosessering av aksjonspunkt og videre behandling.
     * <p>
     * MERK: Det skal ikke ligge spesifikke sjekker som avhenger av status på
     * behanlding, steg eller knytning til spesifikke aksjonspunkter idenne
     * tjenesten.
     *
     */
    @POST
    @Path(AKSJONSPUNKT_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Lagre endringer gitt av aksjonspunktene og rekjør behandling fra gjeldende steg", tags = "aksjonspunkt")
    @BeskyttetRessurs(actionType = ActionType.UPDATE, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Response bekreft(@Context HttpServletRequest request,
                            @TilpassetAbacAttributt(supplierClass = BekreftetAbacDataSupplier.class)
            @Parameter(description = "Liste over aksjonspunkt som skal bekreftes, inklusiv data som trengs for å løse de.") @Valid BekreftedeAksjonspunkterDto apDto)
        throws URISyntaxException {

        if (fatterVedtak(apDto.getBekreftedeAksjonspunktDtoer())) {
            throw new IllegalArgumentException("Fatter vedtak aksjonspunkt løses i eget endepunkt");
        }
        return bekreftAksjonspunkt(request, apDto);
    }

    private Response bekreftAksjonspunkt(HttpServletRequest request, BekreftedeAksjonspunkterDto apDto) throws URISyntaxException {
        var bekreftedeAksjonspunktDtoer = apDto.getBekreftedeAksjonspunktDtoer();

        var lås = behandlingRepository.taSkriveLås(apDto.getBehandlingUuid());
        var behandling = behandlingRepository.hentBehandling(apDto.getBehandlingUuid());

        behandlingutredningTjeneste.kanEndreBehandling(behandling, apDto.getBehandlingVersjon());

        validerBetingelserForAksjonspunkt(behandling, apDto.getBekreftedeAksjonspunktDtoer());

        if (bekreftedeAksjonspunktDtoer.size() > 1) {
            LOG.info("Bekrefter flere aksjonspunkt {}", bekreftedeAksjonspunktDtoer.stream().map(a -> a.getAksjonspunktDefinisjon().getKode()).toList());
        }

        applikasjonstjeneste.bekreftAksjonspunkter(bekreftedeAksjonspunktDtoer, behandling, lås);

        return Redirect.tilBehandlingPollStatus(request, behandling.getUuid());
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
    @BeskyttetRessurs(actionType = ActionType.UPDATE, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Response overstyr(@Context HttpServletRequest request,
                             @TilpassetAbacAttributt(supplierClass = OverstyrtAbacDataSupplier.class)
        @Parameter(description = "Liste over overstyring aksjonspunkter.") @Valid OverstyrteAksjonspunkterDto apDto) throws URISyntaxException {

        var lås = behandlingRepository.taSkriveLås(apDto.getBehandlingUuid());
        var behandling = behandlingRepository.hentBehandling(apDto.getBehandlingUuid());

        behandlingutredningTjeneste.kanEndreBehandling(behandling, apDto.getBehandlingVersjon());

        var overstyrteAksjonspunktDtoer = apDto.getOverstyrteAksjonspunktDtoer();
        validerBetingelserForAksjonspunkt(behandling, overstyrteAksjonspunktDtoer);

        if (overstyrteAksjonspunktDtoer.size() > 1) {
            LOG.info("Overstyrer flere aksjonspunkt {}", overstyrteAksjonspunktDtoer.stream().map(a -> a.getAksjonspunktDefinisjon().getKode()).toList());
        }

        applikasjonstjeneste.overstyrAksjonspunkter(overstyrteAksjonspunktDtoer, behandling, lås);

        return Redirect.tilBehandlingPollStatus(request, behandling.getUuid());
    }

    @POST
    @Path(AKSJONSPUNKT_BESLUTT_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Lagre totrinnsvurdering aksjonspunkt", tags = "aksjonspunkt")
    @BeskyttetRessurs(actionType = ActionType.UPDATE, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Response beslutt(@Context HttpServletRequest request,
                            @TilpassetAbacAttributt(supplierClass = BekreftetAbacDataSupplier.class)
                            @Parameter(description = "Liste over aksjonspunkt som skal bekreftes, inklusiv data som trengs for å løse de.") @Valid BekreftedeAksjonspunkterDto apDto)
        throws URISyntaxException {

        var bekreftedeAksjonspunktDtoer = apDto.getBekreftedeAksjonspunktDtoer();
        if (bekreftedeAksjonspunktDtoer.size() > 1) {
            throw new IllegalArgumentException("Forventer kun ett aksjonspunkt");
        }
        if (!fatterVedtak(bekreftedeAksjonspunktDtoer)) {
            throw new IllegalArgumentException("Forventer aksjonspunkt FATTER_VEDTAK");
        }
        return bekreftAksjonspunkt(request, apDto);
    }

    private static boolean fatterVedtak(Collection<BekreftetAksjonspunktDto> bekreftedeAksjonspunktDtoer) {
        return bekreftedeAksjonspunktDtoer.stream().anyMatch(dto -> dto.getAksjonspunktDefinisjon() == AksjonspunktDefinisjon.FATTER_VEDTAK);
    }

    private static void validerBetingelserForAksjonspunkt(Behandling behandling, Collection<? extends AksjonspunktKode> aksjonspunktDtoer) {
        // TODO (FC): skal ikke ha spesfikke pre-conditions inne i denne tjenesten (sjekk på status FATTER_VEDTAK). Se om kan håndteres annerledes.
        if (behandling.getStatus().equals(BehandlingStatus.FATTER_VEDTAK) && !erFatteVedtakAkpt(aksjonspunktDtoer)) {
            throw new FunksjonellException("FP-760743",
                String.format("Det kan ikke akseptere endringer siden totrinnsbehandling er startet og behandlingen med behandlingId: %s er hos beslutter", behandling.getId()),
                "Avklare med beslutter");
        }
    }

    private static boolean erFatteVedtakAkpt(Collection<? extends AksjonspunktKode> aksjonspunktDtoer) {
        return aksjonspunktDtoer.size() == 1 &&
                aksjonspunktDtoer.iterator().next().getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.FATTER_VEDTAK);
    }

    public static class BekreftetAbacDataSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (BekreftedeAksjonspunkterDto) obj;
            var abac = AbacDataAttributter.opprett()
                .leggTil(AppAbacAttributtType.BEHANDLING_UUID, req.getBehandlingUuid());

            req.getBekreftedeAksjonspunktDtoer().forEach(apDto -> {
                abac.leggTil(AppAbacAttributtType.AKSJONSPUNKT_DEFINISJON, apDto.getAksjonspunktDefinisjon());
                if (apDto instanceof AvklarVergeDto avklarVergeDto && avklarVergeDto.getFnr() != null) {
                    abac.leggTil(AppAbacAttributtType.FNR, avklarVergeDto.getFnr());
                }
                if (apDto instanceof ManuellRegistreringDto manuellRegistreringDto && manuellRegistreringDto.getAnnenForelder() != null && manuellRegistreringDto.getAnnenForelder().getFoedselsnummer() != null) {
                    abac.leggTil(AppAbacAttributtType.FNR, manuellRegistreringDto.getAnnenForelder().getFoedselsnummer());
                }
            });
            return abac;
        }
    }

    public static class OverstyrtAbacDataSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (OverstyrteAksjonspunkterDto) obj;
            var abac = AbacDataAttributter.opprett()
                .leggTil(AppAbacAttributtType.BEHANDLING_UUID, req.getBehandlingUuid());

            req.getOverstyrteAksjonspunktDtoer().forEach(apDto -> abac.leggTil(AppAbacAttributtType.AKSJONSPUNKT_DEFINISJON, apDto.getAksjonspunktDefinisjon()));
            return abac;
        }
    }
}

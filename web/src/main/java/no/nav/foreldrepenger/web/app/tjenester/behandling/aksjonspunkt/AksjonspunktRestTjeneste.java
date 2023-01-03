package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktKode;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.person.verge.dto.AvklarVergeDto;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.TotrinnTjeneste;
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
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public Response getAksjonspunkter(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
        @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        var behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).orElse(null);
        var ttVurderinger = totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(behandling);
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
    @BeskyttetRessurs(actionType = ActionType.UPDATE, resourceType = ResourceType.FAGSAK)
    public Response bekreft(@Context HttpServletRequest request,
                            @TilpassetAbacAttributt(supplierClass = BekreftetAbacDataSupplier.class)
            @Parameter(description = "Liste over aksjonspunkt som skal bekreftes, inklusiv data som trengs for å løse de.") @Valid BekreftedeAksjonspunkterDto apDto)
        throws URISyntaxException { // NOSONAR

        var bekreftedeAksjonspunktDtoer = apDto.getBekreftedeAksjonspunktDtoer();

        var behandling = behandlingRepository.hentBehandling(apDto.getBehandlingUuid());

        behandlingutredningTjeneste.kanEndreBehandling(behandling, apDto.getBehandlingVersjon());

        validerBetingelserForAksjonspunkt(behandling, apDto.getBekreftedeAksjonspunktDtoer());

        applikasjonstjeneste.bekreftAksjonspunkter(bekreftedeAksjonspunktDtoer, behandling.getId());

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
    @BeskyttetRessurs(actionType = ActionType.UPDATE, resourceType = ResourceType.FAGSAK)
    public Response overstyr(@Context HttpServletRequest request,
                             @TilpassetAbacAttributt(supplierClass = OverstyrtAbacDataSupplier.class)
        @Parameter(description = "Liste over overstyring aksjonspunkter.") @Valid OverstyrteAksjonspunkterDto apDto) throws URISyntaxException {

        var behandling = behandlingRepository.hentBehandling(apDto.getBehandlingUuid());

        behandlingutredningTjeneste.kanEndreBehandling(behandling, apDto.getBehandlingVersjon());

        validerBetingelserForAksjonspunkt(behandling, apDto.getOverstyrteAksjonspunktDtoer());

        applikasjonstjeneste.overstyrAksjonspunkter(apDto.getOverstyrteAksjonspunktDtoer(), behandling.getId());

        return Redirect.tilBehandlingPollStatus(request, behandling.getUuid());
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

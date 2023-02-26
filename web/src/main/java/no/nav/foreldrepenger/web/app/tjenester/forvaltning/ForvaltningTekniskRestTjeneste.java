package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.List;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.foreldrepenger.poststed.PostnummerSynkroniseringTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.BehandlingAksjonspunktDto;
import no.nav.vedtak.felles.prosesstask.rest.dto.ProsessTaskIdDto;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path("/forvaltningTeknisk")
@ApplicationScoped
@Transactional
public class ForvaltningTekniskRestTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(ForvaltningTekniskRestTjeneste.class);
    private static final String MANGLER_AP = "Utvikler-feil: Har ikke aksjonspunkt av type: ";

    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private OppgaveTjeneste oppgaveTjeneste;
    private PostnummerSynkroniseringTjeneste postnummerTjeneste;
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;

    public ForvaltningTekniskRestTjeneste() {
        // For CDI
    }

    @Inject
    public ForvaltningTekniskRestTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                          FagsakProsessTaskRepository fagsakProsessTaskRepository,
            OppgaveTjeneste oppgaveTjeneste,
            PostnummerSynkroniseringTjeneste postnummerTjeneste,
            BehandlingskontrollTjeneste behandlingskontrollTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.oppgaveTjeneste = oppgaveTjeneste;
        this.postnummerTjeneste = postnummerTjeneste;
        this.fagsakProsessTaskRepository = fagsakProsessTaskRepository;
    }

    @POST
    @Path("/sett-oppgave-ferdigstilt")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Ferdigstill Gosys-oppgave", tags = "FORVALTNING-teknisk", responses = {
        @ApiResponse(responseCode = "200", description = "Oppgave satt til ferdig."),
        @ApiResponse(responseCode = "400", description = "Fant ikke aktuell oppgave."),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
    })
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT)
    public Response ferdigstillOppgave(
        @TilpassetAbacAttributt(supplierClass = ForvaltningTekniskRestTjeneste.AbacDataSupplier.class)
        @Parameter(description = "Oppgave som skal settes ferdig") @NotNull @Valid ProsessTaskIdDto oppgaveIdDto) {
        try {
            oppgaveTjeneste.ferdigstillOppgaveForForvaltning(oppgaveIdDto.getProsessTaskId().toString());
        } catch (Exception e) {
            LOG.info("Feil fra Gosys ved ferdigstillelse", e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        return Response.ok().build();
    }

    @POST
    @Path("/hent-alle-aapne-oppgaver")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Ferdigstill Gosys-oppgave", tags = "FORVALTNING-teknisk", responses = {
            @ApiResponse(responseCode = "200", description = "Oppgave satt til ferdig."),
            @ApiResponse(responseCode = "400", description = "Fant ikke aktuell oppgave."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
    })
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT)
    public Response hentAlleÅpneOppgaver() {
        try {
            return Response.ok().entity(oppgaveTjeneste.alleÅpneOppgaver()).build();
        } catch (Exception e) {
            LOG.info("Feil fra Gosys ved henting", e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @POST
    @Path("/sett-aksjonspunkt-avbrutt")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Setter åpent aksjonspunkt til status AVBR", tags = "FORVALTNING-teknisk", responses = {
            @ApiResponse(responseCode = "200", description = "Aksjonspunkt avbrutt."),
            @ApiResponse(responseCode = "400", description = "Fant ikke aktuelt aksjonspunkt."),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
    })
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT)
    public Response setAksjonspunktAvbrutt(@BeanParam @Valid BehandlingAksjonspunktDto dto) {
        var behandlingId = dto.getBehandlingUuid();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var lås = behandlingRepository.taSkriveLås(behandling.getId());
        var aksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(dto.getAksjonspunktDefinisjon())
                .orElseThrow(() -> new ForvaltningException(MANGLER_AP + dto.getAksjonspunktKode()));
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
        behandlingskontrollTjeneste.lagreAksjonspunkterAvbrutt(kontekst, behandling.getAktivtBehandlingSteg(), List.of(aksjonspunkt));
        behandlingRepository.lagre(behandling, lås);
        return Response.ok().build();
    }

    public static class AbacDataSupplier implements Function<Object, AbacDataAttributter> {
        @Override
        public AbacDataAttributter apply(Object obj) {
            return AbacDataAttributter.opprett();
        }
    }

    @POST
    @Path("/synk-postnummer")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Hente og lagre kodeverk Postnummer", tags = "FORVALTNING-teknisk")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response synkPostnummer() {
        postnummerTjeneste.synkroniserPostnummer();
        return Response.ok().build();
    }

    @POST
    @Path("/fjern-fagsak-prosesstask-avsluttet")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Fjern fagsakprosesstask for avsluttede behandlinger", tags = "FORVALTNING-teknisk")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response fjernFagsakProsesstaskAvsluttetBehandling() {
        fagsakProsessTaskRepository.fjernForAvsluttedeBehandlinger();
        return Response.ok().build();
    }

}

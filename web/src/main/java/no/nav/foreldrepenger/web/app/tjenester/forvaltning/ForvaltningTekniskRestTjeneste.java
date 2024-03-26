package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

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

    private EntityManager entityManager;
    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private OppgaveTjeneste oppgaveTjeneste;
    private PostnummerSynkroniseringTjeneste postnummerTjeneste;
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;

    public ForvaltningTekniskRestTjeneste() {
        // For CDI
    }

    @Inject
    public ForvaltningTekniskRestTjeneste(EntityManager entityManager,
                                          BehandlingRepositoryProvider repositoryProvider,
                                          FagsakProsessTaskRepository fagsakProsessTaskRepository,
                                          OppgaveTjeneste oppgaveTjeneste,
                                          PostnummerSynkroniseringTjeneste postnummerTjeneste,
                                          BehandlingskontrollTjeneste behandlingskontrollTjeneste) {
        this.entityManager = entityManager;
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

    @POST
    @Path("/rekjor-sob")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Rekjøre SOB", tags = "FORVALTNING-teknisk")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response rerunSoB() {
        var updateSql = """
            update PROSESS_TASK set status = 'KLAR' , versjon=versjon+1, prioritet=100
            WHERE task_type = 'behandlingskontroll.oppdatersakogbehandling' and opprettet_tid > :tidligst
            """;

        var tasks = entityManager.createNativeQuery(updateSql)
            .setParameter("tidligst", LocalDate.of(2023, Month.SEPTEMBER, 27).atStartOfDay())
            .executeUpdate();
        return Response.ok(tasks).build();
    }

    @POST
    @Path("/rekjor-personopprettet")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Rekjøre PersonOversiktOpprettet", tags = "FORVALTNING-teknisk")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response rerunPersonOversiktOpprettet() {
        var updateSql = """
            update PROSESS_TASK set status = 'KLAR', versjon=versjon+1, prioritet=100
            WHERE id in (select pid from (select id as pid from prosess_task
               where status = 'FERDIG' and task_type = 'oppgavebehandling.oppdaterpersonoversikt' and task_parametere like '%status=OPPRE%' and siste_kjoering_ts < :tidligst order by id
               ) where rownum < 5000)
            """;

        var tasks = entityManager.createNativeQuery(updateSql)
            .setParameter("tidligst", LocalDateTime.of(2024, Month.MARCH, 26, 16, 0))
            .executeUpdate();
        return Response.ok(tasks).build();
    }

    @POST
    @Path("/rekjor-personavsluttet")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Rekjøre PersonOversiktAvsluttet", tags = "FORVALTNING-teknisk")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response rerunPersonOversiktAvsluttet() {
        var updateSql = """
            update PROSESS_TASK set status = 'KLAR', versjon=versjon+1, prioritet=100
            WHERE id in (select pid from (select id as pid from prosess_task
               where status = 'FERDIG' and task_type = 'oppgavebehandling.oppdaterpersonoversikt' and task_parametere like '%status=AVSLU%' and siste_kjoering_ts < :tidligst order by id
               ) where rownum < 8000)
            """;

        var tasks = entityManager.createNativeQuery(updateSql)
            .setParameter("tidligst", LocalDateTime.of(2024, Month.MARCH, 26, 20, 0))
            .executeUpdate();
        return Response.ok(tasks).build();
    }

}

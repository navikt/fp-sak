package no.nav.foreldrepenger.web.app.tjenester.forvaltning.stonadsstatistikk;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkTjeneste;
import no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.ForvaltningBehandlingIdDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path("/forvaltning-stonadsstatistikk")
@ApplicationScoped
@Transactional
public class ForvaltningStønadsstatistikkRestTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(ForvaltningStønadsstatistikkRestTjeneste.class);

    private StønadsstatistikkTjeneste stønadsstatistikkTjeneste;
    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private ProsessTaskTjeneste taskTjeneste;
    private EntityManager entityManager;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private FagsakRepository fagsakRepository;

    @Inject
    public ForvaltningStønadsstatistikkRestTjeneste(StønadsstatistikkTjeneste stønadsstatistikkTjeneste,
                                                    BehandlingRepository behandlingRepository,
                                                    SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                                    BehandlingsresultatRepository behandlingsresultatRepository,
                                                    ProsessTaskTjeneste taskTjeneste,
                                                    EntityManager entityManager,
                                                    FagsakRelasjonRepository fagsakRelasjonRepository, FagsakRepository fagsakRepository) {
        this.stønadsstatistikkTjeneste = stønadsstatistikkTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.taskTjeneste = taskTjeneste;
        this.entityManager = entityManager;
        this.fagsakRelasjonRepository = fagsakRelasjonRepository;
        this.fagsakRepository = fagsakRepository;
    }

    ForvaltningStønadsstatistikkRestTjeneste() {
        // CDI
    }

    @POST
    @Operation(description = "Oppretter task for migrering for fagsaker opprettet mellom fom og tom dato", tags = "FORVALTNING-stønadsstatistikk")
    @Path("/opprettTaskForPeriode")
    @Consumes(MediaType.APPLICATION_JSON)
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response opprettTaskForPeriode(@Valid MigreringTaskInput taskInput) {
        var sakDato = taskInput.fom;
        var baseline = LocalDateTime.now();
        while (!sakDato.isAfter(taskInput.tom)) {
            var task = opprettTaskForDato(sakDato, taskInput.spreTasksPåAntallTimer, baseline);
            taskTjeneste.lagre(task);
            sakDato = sakDato.plusDays(1);
        }
        return Response.ok().build();
    }

    private static ProsessTaskData opprettTaskForDato(LocalDate dato, int antallTimerKjøring, LocalDateTime baseline) {
        var prosessTaskData = ProsessTaskData.forProsessTask(StønadsstatistikkMigreringTask.class);

        prosessTaskData.setProperty(StønadsstatistikkMigreringTask.DATO_KEY, dato.toString());
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskData.setPrioritet(150);
        var nesteKjøringEtter = baseline.plusSeconds(LocalDateTime.now().getNano() % (antallTimerKjøring * 3600L - 1));
        prosessTaskData.setNesteKjøringEtter(nesteKjøringEtter);
        return prosessTaskData;
    }

    record MigreringTaskInput(LocalDate fom, LocalDate tom, int spreTasksPåAntallTimer) implements AbacDto {

        @Override
        public AbacDataAttributter abacAttributter() {
            return AbacDataAttributter.opprett();
        }
    }

    @POST
    @Path("/for-behandling")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Genereres vedtak json for en behandling", tags = "FORVALTNING-stønadsstatistikk")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public StønadsstatistikkVedtak genererVedtakJson(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        var behandling = behandlingRepository.hentBehandling(dto.getBehandlingUuid());
        if (!behandling.erYtelseBehandling() || behandlingsresultatRepository.hent(behandling.getId()).isBehandlingHenlagt()) {
            return null;
        }
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        return stønadsstatistikkTjeneste.genererVedtak(BehandlingReferanse.fra(behandling, stp));
    }
}

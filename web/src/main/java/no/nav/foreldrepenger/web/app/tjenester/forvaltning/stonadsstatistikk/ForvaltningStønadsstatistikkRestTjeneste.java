package no.nav.foreldrepenger.web.app.tjenester.forvaltning.stonadsstatistikk;

import static no.nav.foreldrepenger.web.app.tjenester.forvaltning.stonadsstatistikk.StønadsstatistikkMigreringTask.opprettTaskForDato;

import java.time.LocalDate;
import java.util.List;

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

import org.hibernate.jpa.HibernateHints;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.datavarehus.v2.SendStønadsstatistikkForVedtakTask;
import no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkTjeneste;
import no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.ForvaltningBehandlingIdDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
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

    private StønadsstatistikkTjeneste stønadsstatistikkTjeneste;
    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private ProsessTaskTjeneste taskTjeneste;
    private EntityManager entityManager;

    @Inject
    public ForvaltningStønadsstatistikkRestTjeneste(StønadsstatistikkTjeneste stønadsstatistikkTjeneste,
                                                    BehandlingRepository behandlingRepository,
                                                    SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                                    BehandlingsresultatRepository behandlingsresultatRepository,
                                                    ProsessTaskTjeneste taskTjeneste,
                                                    EntityManager entityManager) {
        this.stønadsstatistikkTjeneste = stønadsstatistikkTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.taskTjeneste = taskTjeneste;
        this.entityManager = entityManager;
    }

    ForvaltningStønadsstatistikkRestTjeneste() {
        // CDI
    }

    @POST
    @Operation(description = "Oppretter task for migrering for vedtak opprettet etter fom dato", tags = "FORVALTNING-stønadsstatistikk")
    @Path("/opprettTaskForPeriode")
    @Consumes(MediaType.APPLICATION_JSON)
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response opprettTaskForPeriode(@Valid MigreringTaskInput taskInput) {
        var task = opprettTaskForDato(taskInput.fom, null);
        taskTjeneste.lagre(task);
        return Response.ok().build();
    }

    record MigreringTaskInput(LocalDate fom) implements AbacDto {

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
        return stønadsstatistikkTjeneste.genererVedtak(BehandlingReferanse.fra(behandling), stp);
    }

    @POST
    @Path("/migrerStebarnsadopsjonSaker")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Sender vedtak json på kafka", tags = "FORVALTNING-stønadsstatistikk")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response migrerStebarnsadopsjonSaker() {
        var taskGruppe = new ProsessTaskGruppe();

        var behandlinger = finnVedtakMedStebarnsadopsjon();

        for (var behandling : behandlinger) {
            var task = ProsessTaskData.forProsessTask(SendStønadsstatistikkForVedtakTask.class);
            task.setCallIdFraEksisterende();
            task.setBehandling(behandling.getFagsak().getId(), behandling.getId());
            task.setPrioritet(4); // Skal gå som batch

            taskGruppe.addNesteSekvensiell(task);
        }
        taskTjeneste.lagre(taskGruppe);
        return Response.accepted().build();
    }

    private List<Behandling> finnVedtakMedStebarnsadopsjon() {
        return entityManager.createNativeQuery("""
            select b.* from BEHANDLING b
             join GR_FAMILIE_HENDELSE grfh on grfh.behandling_id = b.id and grfh.aktiv = 'J'
             join FH_ADOPSJON adop on adop.FAMILIE_HENDELSE_ID = nvl(grfh.OVERSTYRT_FAMILIE_HENDELSE_ID, nvl(grfh.BEKREFTET_FAMILIE_HENDELSE_ID, grfh.SOEKNAD_FAMILIE_HENDELSE_ID))
             join BEHANDLING_RESULTAT br on br.BEHANDLING_ID = b.ID
             join BEHANDLING_VEDTAK bv on bv.BEHANDLING_RESULTAT_ID = br.ID
             where b.BEHANDLING_TYPE in ('BT-002','BT-004')
             and adop.EKTEFELLES_BARN = 'J'
            """, Behandling.class)
            .setHint(HibernateHints.HINT_READ_ONLY, "true")
            .getResultList();
    }
}

package no.nav.foreldrepenger.web.app.tjenester.forvaltning.stonadsstatistikk;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerAbacSupplier;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
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

    @POST
    @Operation(description = "Oppretter task for migrering enkeltssak", tags = "FORVALTNING-stønadsstatistikk")
    @Path("/opprettTaskEnkeltSak")
    @Consumes(MediaType.APPLICATION_JSON)
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response opprettTaskEnkeltSak(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class)
                                             @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto s) {
        var sak = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(s.getVerdi())).orElseThrow();
        taskTjeneste.lagre(opprettTaskForFagsak(sak.getId()));
        return Response.ok().build();

    }

    @POST
    @Operation(description = "Oppretter task for migrering", tags = "FORVALTNING-stønadsstatistikk")
    @Path("/opprettTask")
    @Consumes(MediaType.APPLICATION_JSON)
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response opprettTask() {
        var query = entityManager.createNativeQuery("""
                select distinct fagsak_id from
                (select *
                 from fh_terminbekreftelse fht join gr_familie_hendelse gf on gf.soeknad_familie_hendelse_id = fht.familie_hendelse_id
                                               join behandling b on b.id = gf.behandling_id join fagsak f on f.id=b.fagsak_id
                                               join behandling_resultat br on br.behandling_id = b.id join behandling_vedtak bv on bv.behandling_resultat_id = br.id
                 where to_char(termindato, 'YYYY-MM') = '2019-11' and gf.aktiv = 'J'
                     fetch first 3000 rows only)
            """);
        @SuppressWarnings("unchecked")
        List<Number> resultatList = query.getResultList();
        var fagsakIds = resultatList.stream().map(Number::longValue).toList();
        var relaterte = fagsakIds.stream().flatMap(f -> finnRelatertFagsak(f).stream()).collect(Collectors.toSet());
        var samlet = new HashSet<>(fagsakIds);
        samlet.addAll(relaterte);

        samlet.forEach(f -> taskTjeneste.lagre(opprettTaskForFagsak(f)));

        return Response.ok().build();
    }

    @POST
    @Operation(description = "Oppretter task for migrering", tags = "FORVALTNING-stønadsstatistikk")
    @Path("/opprettTasker")
    @Consumes(MediaType.APPLICATION_JSON)
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response opprettTasker() {
        var query = entityManager.createNativeQuery("""
                select distinct fagsak_id from
                (select *
                 from fh_terminbekreftelse fht join gr_familie_hendelse gf on gf.soeknad_familie_hendelse_id = fht.familie_hendelse_id
                                               join behandling b on b.id = gf.behandling_id join fagsak f on f.id=b.fagsak_id
                                               join behandling_resultat br on br.behandling_id = b.id join behandling_vedtak bv on bv.behandling_resultat_id = br.id
                 where to_char(termindato, 'YYYY') = :migrereMedTermindatoI and gf.aktiv = 'J'
                     fetch first 3000 rows only)
            """);
        query.setParameter("migrereMedTermindatoI", )

        @SuppressWarnings("unchecked")
        List<Number> resultatList = query.getResultList();
        var fagsakIds = resultatList.stream().map(Number::longValue).toList();
        var relaterte = fagsakIds.stream().flatMap(f -> finnRelatertFagsak(f).stream()).collect(Collectors.toSet());
        var samlet = new HashSet<>(fagsakIds);
        samlet.addAll(relaterte);

        samlet.forEach(f -> taskTjeneste.lagre(opprettTaskForFagsak(f)));

        return Response.ok().build();
    }

    static ProsessTaskData opprettTaskForFagsak(Long fagsakId) {
        LOG.info("Oppretter stønadsstatistikk task for fagsakId {}", fagsakId);
        var prosessTaskData = ProsessTaskData.forProsessTask(StønadsstatistikkMigreringTask.class);
        prosessTaskData.setProperty(StønadsstatistikkMigreringTask.FAGSAK_ID, String.valueOf(fagsakId));
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskData.setPrioritet(150);
        return prosessTaskData;
    }

    private Optional<Long> finnRelatertFagsak(Long fagsakId) {
        return fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(fagsakId)
            .flatMap(fr -> fr.getRelatertFagsakFraId(fagsakId))
            .map(Fagsak::getId);
    }

    private record TaskInput(LocalDate fagsakOpprettetFom, LocalDate fagsakOpprettetTom, @Min(0) @Max(60) int secondsBetween) implements AbacDto {

        @Override
        public AbacDataAttributter abacAttributter() {
            return AbacDataAttributter.opprett();
        }
    }
}

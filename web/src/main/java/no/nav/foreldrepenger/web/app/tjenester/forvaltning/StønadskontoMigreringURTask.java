package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.util.Optional;
import java.util.stream.Stream;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.domene.uttak.beregnkontoer.BeregnStønadskontoerTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@Dependent
@ProsessTask(value = "stønadskonto.behandling.migrering", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class StønadskontoMigreringURTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(StønadskontoMigreringURTask.class);
    private static final String FRA_ID = "fraId";
    private static final String MAX_ID = "maxId";
    private static final String DRY_RUN = "dryRun";

    private final FagsakRelasjonRepository fagsakRelasjonRepository;
    private final EntityManager entityManager;
    private final ProsessTaskTjeneste prosessTaskTjeneste;
    private final UttakInputTjeneste uttakInputTjeneste;
    private final FagsakRelasjonRepository fagsakRelasjonTjeneste;
    private final BeregnStønadskontoerTjeneste beregnStønadskontoerTjeneste;

    @Inject
    public StønadskontoMigreringURTask(FagsakRelasjonRepository fagsakRelasjonRepository,
                                       EntityManager entityManager,
                                       ProsessTaskTjeneste prosessTaskTjeneste,
                                       UttakInputTjeneste uttakInputTjeneste,
                                       FagsakRelasjonRepository fagsakRelasjonTjeneste,
                                       BeregnStønadskontoerTjeneste beregnStønadskontoerTjeneste) {
        this.fagsakRelasjonRepository = fagsakRelasjonRepository;
        this.entityManager = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.beregnStønadskontoerTjeneste = beregnStønadskontoerTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var fraId = Optional.ofNullable(prosessTaskData.getPropertyValue(FRA_ID)).map(Long::valueOf).orElse(null);
        var maxId = Optional.ofNullable(prosessTaskData.getPropertyValue(MAX_ID)).map(Long::valueOf).orElse(null);
        var dryRun = Optional.ofNullable(prosessTaskData.getPropertyValue(DRY_RUN)).filter("false"::equalsIgnoreCase).isEmpty();

        var beregninger = finnNesteHundreStønadskonti(fraId).toList();

        beregninger.stream().filter(b -> maxId == null || b.getId() < maxId).forEach(ur -> håndterBeregning(ur, dryRun));

        beregninger.stream()
            .map(UttakResultatEntitet::getId)
            .max(Long::compareTo)
            .filter(v -> maxId == null || v < maxId)
            .ifPresent(nesteId -> prosessTaskTjeneste.lagre(opprettNesteTask(nesteId, dryRun, maxId)));
    }

    private Stream<UttakResultatEntitet> finnNesteHundreStønadskonti(Long fraId) {
        var sql ="""
            select * from (
            select ur.* from UTTAK_RESULTAT ur
            where ur.ID >:fraId and ur.KONTO_BEREGNING_ID is null
            order by ur.id)
            where ROWNUM <= 100
            """;

        var query = entityManager.createNativeQuery(sql, UttakResultatEntitet.class)
            .setParameter("fraId", fraId == null ? 0 : fraId);
        return query.getResultStream();
    }

    private void håndterBeregning(UttakResultatEntitet uttakResultatEntitet, boolean dryRun) {
        var behandling = uttakResultatEntitet.getBehandlingsresultat().getBehandling();
        var uttakInput = uttakInputTjeneste.lagInput(behandling);
        var fagsakRelasjon = fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(behandling.getFagsakId(), uttakResultatEntitet.getOpprettetTidspunkt())
            .or(() -> fagsakRelasjonRepository.finnTidligsteRelasjonForHvisEksisterer(behandling.getFagsakId()))
            .or(() -> fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(behandling.getFagsak()))
            .orElseThrow();
        var kontoFagsakRelasjon = fagsakRelasjon.getGjeldendeStønadskontoberegning().orElseThrow();
        var dekningsgrad = fagsakRelasjon.getGjeldendeDekningsgrad();
        if (dryRun) {
            beregnStønadskontoerTjeneste.loggForBehandling(uttakInput, dekningsgrad, kontoFagsakRelasjon, behandling.getFagsak().erStengt());
            return;
        }
        var endretBeregning = beregnStønadskontoerTjeneste.beregnForBehandling(uttakInput, dekningsgrad, kontoFagsakRelasjon.getStønadskontoutregning());
        endretBeregning.ifPresent(fagsakRelasjonRepository::persisterFlushStønadskontoberegning);
        var kontoBeregningId = endretBeregning.map(Stønadskontoberegning::getId).orElse(kontoFagsakRelasjon.getId());
        oppdaterUttakMedKontoId(uttakResultatEntitet, kontoBeregningId);
    }

    private int oppdaterUttakMedKontoId(UttakResultatEntitet uttakResultatEntitet, Long beregningId) {
        return entityManager.createNativeQuery(
            "UPDATE UTTAK_RESULTAT SET KONTO_BEREGNING_ID = :kontoid WHERE id = :urid")
            .setParameter("kontoid", beregningId)
            .setParameter("urid", uttakResultatEntitet.getId())
            .executeUpdate();
    }

    public static ProsessTaskData opprettNesteTask(Long fraVedtakId, boolean dryRun, Long maxId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(StønadskontoMigreringURTask.class);

        prosessTaskData.setProperty(StønadskontoMigreringURTask.FRA_ID, fraVedtakId == null ? null : String.valueOf(fraVedtakId));
        prosessTaskData.setProperty(StønadskontoMigreringURTask.MAX_ID, maxId == null ? null : String.valueOf(maxId));
        prosessTaskData.setProperty(StønadskontoMigreringURTask.DRY_RUN, String.valueOf(dryRun));
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskData.setPrioritet(150);
        return prosessTaskData;
    }
}

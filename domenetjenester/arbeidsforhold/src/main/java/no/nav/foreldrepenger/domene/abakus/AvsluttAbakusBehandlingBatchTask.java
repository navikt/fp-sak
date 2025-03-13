package no.nav.foreldrepenger.domene.abakus;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ProsessTask(value = "abakus.avslutt.kobling.batch", prioritet = 4, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class AvsluttAbakusBehandlingBatchTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(AvsluttAbakusBehandlingBatchTask.class);
    private static final String FRA_OG_MED = "fraOgMed";
    private static final String TIL_OG_MED = "tilOgMed";
    private static final String DRY_RUN = "dryRun";
    private static final int MAX_RESULT_BATCH_SIZE = 100;

    private final InntektArbeidYtelseTjeneste abakusTjeneste;
    private final EntityManager entityManager;
    private final ProsessTaskTjeneste taskTjeneste;

    @Inject
    public AvsluttAbakusBehandlingBatchTask(InntektArbeidYtelseTjeneste abakusTjeneste,
                                            EntityManager entityManager,
                                            ProsessTaskTjeneste taskTjeneste) {
        this.abakusTjeneste = abakusTjeneste;
        this.entityManager = entityManager;
        this.taskTjeneste = taskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var fraOgMedId = Optional.ofNullable(prosessTaskData.getPropertyValue(FRA_OG_MED)).map(Long::valueOf).orElseThrow();
        var tilOgMedId = Optional.ofNullable(prosessTaskData.getPropertyValue(TIL_OG_MED)).map(Long::valueOf).orElseThrow();
        var dryRun = Optional.ofNullable(prosessTaskData.getPropertyValue(DRY_RUN)).filter("false"::equalsIgnoreCase).isEmpty();

        var saker = finnNesteHundreSaker(fraOgMedId, tilOgMedId).toList();

        saker.forEach(sak -> håndterBehandlingerFor(sak, dryRun));

        saker.stream()
            .map(Fagsak::getId)
            .max(Long::compareTo)
            .ifPresent(nesteId -> taskTjeneste.lagre(opprettNesteTask(nesteId + 1, dryRun, tilOgMedId)));
    }

    private Stream<Fagsak> finnNesteHundreSaker(Long fraOgMedId, Long tilOgMedId) {
        var sql = """
            select fag.* from FAGSAK fag
            where fag.ID >= :fraOgMedId and fag.ID <= :tilOgMedId
            order by fag.id
            """;

        var query = entityManager.createNativeQuery(sql, Fagsak.class)
            .setParameter("fraOgMedId", fraOgMedId)
            .setParameter("tilOgMedId", tilOgMedId)
            .setMaxResults(MAX_RESULT_BATCH_SIZE);
        return query.getResultStream();
    }

    private void håndterBehandlingerFor(Fagsak fagsak, boolean dryRun) {
        if (dryRun) {
            LOG.info("Dry run: Avslutter kobling for behandling {}", fagsak.getSaksnummer());
            return;
        }
        avsluttBehandlingerFor(fagsak.getSaksnummer());
    }

    private void avsluttBehandlingerFor(Saksnummer saksnummer) {
        var behandlinger = hentAlleBehandlingerFor(saksnummer).stream()
            .filter(Behandling::erYtelseBehandling)
            .filter(Behandling::erAvsluttet)
            .toList();
        behandlinger.stream().map(Behandling::getId).forEach(abakusTjeneste::avslutt);
    }

    private List<Behandling> hentAlleBehandlingerFor(Saksnummer saksnummer) {
        Objects.requireNonNull(saksnummer, "saksnummer");
        Objects.requireNonNull(saksnummer.getVerdi());

        var query = entityManager.createQuery("select beh from Behandling as beh join fetch beh.fagsak where beh.fagsak.saksnummer = :saksnummer",
            Behandling.class);
        query.setParameter("saksnummer", saksnummer);
        return query.getResultList();
    }

    private static ProsessTaskData opprettNesteTask(Long nyFraOgMed, boolean dryRun, Long tilOgMed) {
        var prosessTaskData = ProsessTaskData.forProsessTask(AvsluttAbakusBehandlingBatchTask.class);
        prosessTaskData.setProperty(FRA_OG_MED, String.valueOf(nyFraOgMed));
        prosessTaskData.setProperty(TIL_OG_MED, String.valueOf(tilOgMed));
        prosessTaskData.setProperty(DRY_RUN, String.valueOf(dryRun));
        return prosessTaskData;
    }
}

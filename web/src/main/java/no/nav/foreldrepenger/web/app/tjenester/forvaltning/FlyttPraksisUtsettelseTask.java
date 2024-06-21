package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.util.Optional;
import java.util.stream.Stream;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.EnhetsTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

/*
 * Engangsbruk. Slett denne og sett til private EnhetsTjeneste.MIDLERTIDIG_ENHET
 */
@Dependent
@ProsessTask(value = "oppgavebehandling.praksis.utsettelse", prioritet = 4, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class FlyttPraksisUtsettelseTask implements ProsessTaskHandler {

    public static final String BEGRUNNELSE = "Praksis utsettelse";

    private static final Logger LOG = LoggerFactory.getLogger(FlyttPraksisUtsettelseTask.class);
    private static final String FRA_ID = "fraId";


    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private final EntityManager entityManager;
    private final ProsessTaskTjeneste prosessTaskTjeneste;

    @Inject
    public FlyttPraksisUtsettelseTask(BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                                      EntityManager entityManager,
                                      ProsessTaskTjeneste prosessTaskTjeneste) {
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.entityManager = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var fraId = Optional.ofNullable(prosessTaskData.getPropertyValue(FRA_ID)).map(Long::valueOf).orElse(null);

        var behandlinger = finnNesteHundreBehandlinger(fraId).toList();

        behandlinger.forEach(this::flyttTilMidlertidigEnhet);

        behandlinger.stream()
            .map(Behandling::getId)
            .max(Long::compareTo)
            .ifPresent(nesteId -> prosessTaskTjeneste.lagre(opprettNesteTask(nesteId)));
    }

    private Stream<Behandling> finnNesteHundreBehandlinger(Long fraId) {
        var sql ="""
            select * from (
            select b.* from behandling b join fagsak_egenskap fe on fe.fagsak_id = b.fagsak_id
            where b.ID > :fraId and b.behandling_status <> 'AVSLU' and fe.egenskap_value = 'PRAKSIS_UTSETTELSE' and b.behandlende_enhet = '4867'
            order by b.id)
            where ROWNUM <= 100
            """;

        var query = entityManager.createNativeQuery(sql, Behandling.class)
            .setParameter("fraId", fraId == null ? 0 : fraId);
        return query.getResultStream();
    }

    private void flyttTilMidlertidigEnhet(Behandling behandling) {
        LOG.info("Endrer behandlende enhet for behandling: {}", behandling.getId());
        behandlendeEnhetTjeneste.oppdaterBehandlendeEnhet(behandling, EnhetsTjeneste.MIDLERTIDIG_ENHET, HistorikkAktør.VEDTAKSLØSNINGEN, BEGRUNNELSE);

    }

    public static ProsessTaskData opprettNesteTask(Long fraVedtakId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(FlyttPraksisUtsettelseTask.class);

        prosessTaskData.setProperty(FlyttPraksisUtsettelseTask.FRA_ID, fraVedtakId == null ? null : String.valueOf(fraVedtakId));
        prosessTaskData.setCallIdFraEksisterende();
        return prosessTaskData;
    }
}

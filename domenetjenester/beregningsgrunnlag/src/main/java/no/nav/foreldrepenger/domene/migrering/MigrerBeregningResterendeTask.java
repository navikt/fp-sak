package no.nav.foreldrepenger.domene.migrering;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;

import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

import java.util.Optional;
import java.util.stream.Stream;

@ApplicationScoped
@ProsessTask(value = "beregningsgrunnlag.migrer.resterende", prioritet = 4)
public class MigrerBeregningResterendeTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(MigrerBeregningResterendeTask.class);
    private static final String DRY_RUN = "dryRun";

    private ProsessTaskTjeneste prosessTaskTjeneste;
    private EntityManager entityManager;

    MigrerBeregningResterendeTask() {
        // CDI
    }

    @Inject
    public MigrerBeregningResterendeTask(ProsessTaskTjeneste prosessTaskTjeneste,
                                         EntityManager entityManager) {
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.entityManager = entityManager;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        LOG.info("Starter task for å migrere resterende fagsaker.");
        var dryRun = Optional.ofNullable(prosessTaskData.getPropertyValue(DRY_RUN)).filter("false"::equalsIgnoreCase).isEmpty();
        var fagsaker = finnRest().toList();
        LOG.info("Fant {} saker å migrere ", fagsaker.size());
        if (!dryRun) {
            fagsaker.forEach(f -> {
                LOG.info("Lager task for å migrere saksnummer {}.", f.getSaksnummer());
                var migreringstask = ProsessTaskData.forProsessTask(MigrerBeregningSakTask.class);
                migreringstask.setSaksnummer(f.getSaksnummer().getVerdi());
                prosessTaskTjeneste.lagre(migreringstask);
            });
        }
        LOG.info("Avslutter task for å migrere resterende fagsaker.");
    }

    private Stream<Fagsak> finnRest() {
        var sql ="""
            select * from fagsak where id in (select distinct(fag.id) from FAGSAK fag
            inner join BEHANDLING beh on beh.fagsak_id = fag.id
            inner join gr_beregningsgrunnlag gr on gr.behandling_id = beh.id
            where gr.aktiv =:aktivFlagg and beh.behandling_status =:status
            and beh.id not in (select behandling_id from bg_ekstern_kobling))
            and ROWNUM <= 100
            """;

        var query = entityManager.createNativeQuery(sql, Fagsak.class)
            .setParameter("aktivFlagg", "J")
            .setParameter("status", "AVSLU");
        return query.getResultStream();
    }
}

package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;

import no.nav.foreldrepenger.økonomistøtte.simulering.klient.FpOppdragRestKlient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "aap.etterbetaling.beregning", prioritet = 4, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class AapEtterbetalingBeregningTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(AapEtterbetalingBeregningTask.class);
    private static final String FRA_OG_MED = "fraOgMed";
    private static final String TIL_OG_MED = "tilOgMed";

    private EntityManager entityManager;
    private FpOppdragRestKlient fpOppdragRestKlient;


    public AapEtterbetalingBeregningTask() {
        // For CDI
    }

    @Inject
    public AapEtterbetalingBeregningTask(EntityManager entityManager,
                                         FpOppdragRestKlient fpOppdragRestKlient) {
        this.entityManager = entityManager;
        this.fpOppdragRestKlient = fpOppdragRestKlient;
    }


    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var fraOgMedId = Optional.ofNullable(prosessTaskData.getPropertyValue(FRA_OG_MED)).map(Long::valueOf).orElseThrow();
        var tilOgMedId = Optional.ofNullable(prosessTaskData.getPropertyValue(TIL_OG_MED)).map(Long::valueOf).orElseThrow();

        var behandlinger = finnAktuelleBehandlinger(fraOgMedId, tilOgMedId);

        List<Long> alleEtterbetalinger = behandlinger.stream().map(behandling -> {
            var simulering = fpOppdragRestKlient.hentSimuleringResultatMedOgUtenInntrekk(behandling.getId(), behandling.getUuid(),
                behandling.getSaksnummer().getVerdi());
            return simulering.filter(sim -> sim.simuleringResultat() != null && sim.simuleringResultat().sumEtterbetaling() != null)
                .map(sim -> sim.simuleringResultat().sumEtterbetaling());
        }).flatMap(Optional::stream).toList();

        if (alleEtterbetalinger.isEmpty()) {
            LOG.info("AAP_ETTERBETALINGER: Behandlinger med id fra {} til {} hadde ingen etterbetalinger", fraOgMedId, tilOgMedId);
        } else {
            var sumEtterbetalinger = alleEtterbetalinger.stream().reduce(Long::sum).orElseThrow();
            var snittEtterbetaling = BigDecimal.valueOf(sumEtterbetalinger).divide(BigDecimal.valueOf(alleEtterbetalinger.size()), 2, RoundingMode.HALF_EVEN);

            LOG.info("AAP_ETTERBETALINGER: Behandlinger med id fra {} til {} hadde i snitt {} i etterbetaling fordelt over totalt {} simuleringsresultat", fraOgMedId, tilOgMedId, snittEtterbetaling, alleEtterbetalinger.size());
        }
    }

    private List<Behandling> finnAktuelleBehandlinger(Long fraOgMedId, Long tilOgMedId) {
        var query = entityManager.createNativeQuery("""
            select * from (select * from behandling where id in (select distinct(beh.id) from Behandling beh
             inner join BEHANDLING_ARSAK ar on ar.behandling_id = beh.id
             where ar.behandling_arsak_type = 'FEIL_PRAKSIS_BG_AAP_KOMBI' and beh.behandling_status = 'AVSLU'
             and beh.id >= :fraOgMedId and beh.id <= :tilOgMedId) order by id)
             where ROWNUM <= 50""", Behandling.class).setParameter("fraOgMedId", fraOgMedId).setParameter("tilOgMedId", tilOgMedId);
        // Hardkoder inn grense på 50 saker for å ikke ta for mye ressurser
        return query.getResultList();
    }
}

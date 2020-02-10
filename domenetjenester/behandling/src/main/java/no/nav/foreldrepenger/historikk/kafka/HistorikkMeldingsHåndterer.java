package no.nav.foreldrepenger.historikk.kafka;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.vedtak.felles.AktiverContextOgTransaksjon;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
@AktiverContextOgTransaksjon
public class HistorikkMeldingsHåndterer {
    private ProsessTaskRepository prosessTaskRepository;

    public HistorikkMeldingsHåndterer() {
    }

    @Inject
    public HistorikkMeldingsHåndterer(ProsessTaskRepository prosessTaskRepository) {
        this.prosessTaskRepository = prosessTaskRepository;
    }

    void lagreMelding(@SuppressWarnings("unused") String header, String payload) { // NOSONAR
        ProsessTaskData data = new ProsessTaskData(LagreHistorikkTask.TASKTYPE);
        data.setPayload(payload);
        prosessTaskRepository.lagre(data);
    }
}

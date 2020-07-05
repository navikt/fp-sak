package no.nav.foreldrepenger.domene.risikoklassifisering.konsument;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.transaction.Transactional;

import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
@ActivateRequestContext
@Transactional
public class RisikoklassifiseringMeldingsHåndterer {
    private ProsessTaskRepository prosessTaskRepository;

    public RisikoklassifiseringMeldingsHåndterer() {
    }

    @Inject
    public RisikoklassifiseringMeldingsHåndterer(ProsessTaskRepository prosessTaskRepository) {
        this.prosessTaskRepository = prosessTaskRepository;
    }

    void lagreMelding(String payload) {
        ProsessTaskData data = new ProsessTaskData(LesKontrollresultatTask.TASKTYPE);
        data.setPayload(payload);
        prosessTaskRepository.lagre(data);
    }
}

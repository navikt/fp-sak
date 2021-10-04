package no.nav.foreldrepenger.domene.risikoklassifisering.konsument;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.transaction.Transactional;

import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
@ActivateRequestContext
@Transactional
public class RisikoklassifiseringMeldingsHåndterer {
    private ProsessTaskTjeneste taskTjeneste;

    public RisikoklassifiseringMeldingsHåndterer() {
    }

    @Inject
    public RisikoklassifiseringMeldingsHåndterer(ProsessTaskTjeneste taskTjeneste) {
        this.taskTjeneste = taskTjeneste;
    }

    void lagreMelding(String payload) {
        var data = ProsessTaskData.forProsessTask(LesKontrollresultatTask.class);
        data.setPayload(payload);
        taskTjeneste.lagre(data);
    }
}

package no.nav.foreldrepenger.domene.risikoklassifisering.konsument;

import no.nav.vedtak.felles.AktiverContextOgTransaksjon;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
@AktiverContextOgTransaksjon
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

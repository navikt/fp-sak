package no.nav.foreldrepenger.web.app.tjenester.forvaltning.praksisutsettelse;

import java.time.LocalDateTime;
import java.util.Comparator;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.dokumentbestiller.infobrev.FeilPraksisUtsettelseRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@Dependent
@ProsessTask(value = "behandling.feilpraksisutsettelse.alle", prioritet = 4, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class FeilPraksisUtsettelseAlleTask implements ProsessTaskHandler {

    private static final String FRA_FAGSAK_ID = "fraFagsakId";
    private final FeilPraksisUtsettelseRepository utvalgRepository;
    private final ProsessTaskTjeneste prosessTaskTjeneste;

    @Inject
    public FeilPraksisUtsettelseAlleTask(FeilPraksisUtsettelseRepository utvalgRepository,
                                         ProsessTaskTjeneste prosessTaskTjeneste) {
        this.utvalgRepository = utvalgRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var fagsakIdProperty = prosessTaskData.getPropertyValue(FRA_FAGSAK_ID);
        var fraFagsakId = fagsakIdProperty == null ? null : Long.valueOf(fagsakIdProperty);

        var saker = utvalgRepository.finnNesteHundreSakerSomErMerketFeilIverksettelseFriUtsettelse(fraFagsakId);

        saker.stream().map(FeilPraksisUtsettelseAlleTask::opprettTaskForEnkeltSak).forEach(prosessTaskTjeneste::lagre);

        saker.stream().max(Comparator.naturalOrder())
            .map(FeilPraksisUtsettelseAlleTask::opprettTaskForNesteUtvalg)
            .ifPresent(prosessTaskTjeneste::lagre);

    }

    public static ProsessTaskData opprettTaskForEnkeltSak(Long fagsakId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(FeilPraksisUtsettelseSingleTask.class);
        prosessTaskData.setProperty(FeilPraksisUtsettelseSingleTask.FAGSAK_ID, String.valueOf(fagsakId));
        return prosessTaskData;
    }


    public static ProsessTaskData opprettTaskForNesteUtvalg(Long fraFagsakId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(FeilPraksisUtsettelseAlleTask.class);
        prosessTaskData.setProperty(FeilPraksisUtsettelseAlleTask.FRA_FAGSAK_ID, fraFagsakId == null ? null : String.valueOf(fraFagsakId));
        prosessTaskData.setNesteKjøringEtter(LocalDateTime.now().plusSeconds(10));
        return prosessTaskData;
    }
}

package no.nav.foreldrepenger.web.app.tjenester.forvaltning.praksisutsettelse;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.dokumentbestiller.infobrev.FeilPraksisUtsettelseRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@Dependent
@ProsessTask(value = "behandling.saksmerkepraksisutsettelse.alle", prioritet = 4, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class FeilPraksisSaksmerkingAlleTask implements ProsessTaskHandler {

    private static final String UTVALG = "utvalg";
    private static final String FRA_FAGSAK_ID = "fraFagsakId";
    private final FeilPraksisUtsettelseRepository utvalgRepository;
    private final ProsessTaskTjeneste prosessTaskTjeneste;

    public enum Utvalg { MOR, FAR_BEGGE_RETT }

    @Inject
    public FeilPraksisSaksmerkingAlleTask(FeilPraksisUtsettelseRepository utvalgRepository,
                                          ProsessTaskTjeneste prosessTaskTjeneste) {
        this.utvalgRepository = utvalgRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var fagsakIdProperty = prosessTaskData.getPropertyValue(FRA_FAGSAK_ID);
        var fraFagsakId = fagsakIdProperty == null ? null : Long.valueOf(fagsakIdProperty);
        var utvalg = Optional.ofNullable(prosessTaskData.getPropertyValue(UTVALG))
            .map(Utvalg::valueOf).orElseThrow();

        var saker = switch (utvalg) {
            case MOR -> utvalgRepository.finnNesteHundreSakerForMerkingMor(fraFagsakId);
            case FAR_BEGGE_RETT -> utvalgRepository.finnNesteHundreSakerForMerkingFarBeggeEllerAlene(fraFagsakId);
        };
        saker.stream().map(FeilPraksisSaksmerkingAlleTask::opprettTaskForEnkeltSak).forEach(prosessTaskTjeneste::lagre);

        saker.stream().max(Comparator.naturalOrder())
            .map(maxfid -> opprettTaskForNesteUtvalg(maxfid, utvalg))
            .ifPresent(prosessTaskTjeneste::lagre);

    }

    public static ProsessTaskData opprettTaskForEnkeltSak(Long fagsakId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(FeilPraksisSaksmerkingSingleTask.class);
        prosessTaskData.setProperty(FeilPraksisSaksmerkingSingleTask.FAGSAK_ID, String.valueOf(fagsakId));
        return prosessTaskData;
    }


    public static ProsessTaskData opprettTaskForNesteUtvalg(Long fraFagsakId, Utvalg utvalg) {
        var prosessTaskData = ProsessTaskData.forProsessTask(FeilPraksisSaksmerkingAlleTask.class);
        prosessTaskData.setProperty(FeilPraksisSaksmerkingAlleTask.FRA_FAGSAK_ID, fraFagsakId == null ? null : String.valueOf(fraFagsakId));
        prosessTaskData.setProperty(FeilPraksisSaksmerkingAlleTask.UTVALG, utvalg.name());
        prosessTaskData.setNesteKjøringEtter(LocalDateTime.now().plusSeconds(30));
        return prosessTaskData;
    }
}

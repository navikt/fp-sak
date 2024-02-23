package no.nav.foreldrepenger.web.app.tjenester.forvaltning.praksisutsettelse;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.dokumentbestiller.infobrev.FeilPraksisUtsettelseRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@Dependent
@ProsessTask(value = "behandling.feilpraksisutsettelse.alle", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class FeilPraksisUtsettelseAlleTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(FeilPraksisUtsettelseAlleTask.class);
    private static final String UTVALG = "utvalg";
    private static final String FRA_FAGSAK_ID = "fraFagsakId";
    private final FeilPraksisUtsettelseRepository utvalgRepository;
    private final ProsessTaskTjeneste prosessTaskTjeneste;

    public enum Utvalg { MOR, FAR_BEGGE_RETT, BARE_FAR_RETT }

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
        var utvalg = Optional.ofNullable(prosessTaskData.getPropertyValue(UTVALG))
            .map(Utvalg::valueOf).orElseThrow();

        var saker = switch (utvalg) {
            case MOR -> utvalgRepository.finnNesteHundreAktuelleSakerMor(fraFagsakId);
            case FAR_BEGGE_RETT -> utvalgRepository.finnNesteHundreAktuelleSakerFarBeggeEllerAlene(fraFagsakId);
            case BARE_FAR_RETT -> utvalgRepository.finnNesteHundreAktuelleSakerBareFarHarRett(fraFagsakId);
        };
        saker.stream().map(FeilPraksisUtsettelseAlleTask::opprettTaskForEnkeltSak).forEach(prosessTaskTjeneste::lagre);

        saker.stream().max(Comparator.naturalOrder())
            .map(maxfid -> opprettTaskForNesteUtvalg(maxfid, utvalg))
            .ifPresent(prosessTaskTjeneste::lagre);

    }

    public static ProsessTaskData opprettTaskForEnkeltSak(Long fagsakId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(FeilPraksisUtsettelseSingleTask.class);
        prosessTaskData.setProperty(FeilPraksisUtsettelseSingleTask.FAGSAK_ID, String.valueOf(fagsakId));
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskData.setPrioritet(150);
        return prosessTaskData;
    }


    public static ProsessTaskData opprettTaskForNesteUtvalg(Long fraFagsakId, Utvalg utvalg) {
        var prosessTaskData = ProsessTaskData.forProsessTask(FeilPraksisUtsettelseAlleTask.class);
        prosessTaskData.setProperty(FeilPraksisUtsettelseAlleTask.FRA_FAGSAK_ID, fraFagsakId == null ? null : String.valueOf(fraFagsakId));
        prosessTaskData.setProperty(FeilPraksisUtsettelseAlleTask.UTVALG, utvalg.name());
        prosessTaskData.setNesteKjøringEtter(LocalDateTime.now().plusSeconds(30));
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskData.setPrioritet(150);
        return prosessTaskData;
    }
}

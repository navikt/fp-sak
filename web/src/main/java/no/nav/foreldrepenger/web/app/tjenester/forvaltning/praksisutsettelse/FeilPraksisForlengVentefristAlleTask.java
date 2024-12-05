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
@ProsessTask(value = "behandling.ventefristpraksisutsettelse.alle", prioritet = 4, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class FeilPraksisForlengVentefristAlleTask implements ProsessTaskHandler {

    private static final String FRA_BEHANDLING_ID = "frBehandlingId";
    private final FeilPraksisUtsettelseRepository utvalgRepository;
    private final ProsessTaskTjeneste prosessTaskTjeneste;

    @Inject
    public FeilPraksisForlengVentefristAlleTask(FeilPraksisUtsettelseRepository utvalgRepository, ProsessTaskTjeneste prosessTaskTjeneste) {
        this.utvalgRepository = utvalgRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var behandlingIdProperty = prosessTaskData.getPropertyValue(FRA_BEHANDLING_ID);
        var fraBehandlingId = behandlingIdProperty == null ? null : Long.valueOf(behandlingIdProperty);

        var behandlinger = utvalgRepository.finnNesteHundreBehandlingerSomErPåVentTilDesember(fraBehandlingId);

        behandlinger.stream().map(behandling -> opprettTaskForEnkeltSak(behandling.fagsakId(), behandling.id(), behandling.saksnummer())).forEach(prosessTaskTjeneste::lagre);

        behandlinger.stream()
            .map(FeilPraksisUtsettelseRepository.BehandlingMedFagsakId::id)
            .max(Comparator.naturalOrder())
            .map(FeilPraksisForlengVentefristAlleTask::opprettTaskForNesteUtvalg)
            .ifPresent(prosessTaskTjeneste::lagre);

    }

    public static ProsessTaskData opprettTaskForEnkeltSak(Long fagsakId, Long behandlingId, String saksnummer) {
        var prosessTaskData = ProsessTaskData.forProsessTask(FeilPraksisForlengVentefristSingleTask.class);
        prosessTaskData.setBehandling(saksnummer, fagsakId, behandlingId);
        return prosessTaskData;
    }


    public static ProsessTaskData opprettTaskForNesteUtvalg(Long fraBehandlingId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(FeilPraksisForlengVentefristAlleTask.class);
        prosessTaskData.setProperty(FeilPraksisForlengVentefristAlleTask.FRA_BEHANDLING_ID,
            fraBehandlingId == null ? null : String.valueOf(fraBehandlingId));
        prosessTaskData.setNesteKjøringEtter(LocalDateTime.now().plusSeconds(10));
        return prosessTaskData;
    }
}

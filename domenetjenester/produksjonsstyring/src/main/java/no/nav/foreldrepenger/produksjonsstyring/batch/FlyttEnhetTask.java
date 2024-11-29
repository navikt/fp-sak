package no.nav.foreldrepenger.produksjonsstyring.batch;

import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingKandidaterRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.task.OppdaterBehandlendeEnhetTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@Dependent
@ProsessTask(value = "oppgavebehandling.flytt.enhet", prioritet = 2)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class FlyttEnhetTask implements ProsessTaskHandler {

    public static final String ENHET_ID = "enhetId";

    private BehandlingKandidaterRepository behandlingKandidaterRepository;
    private ProsessTaskTjeneste taskTjeneste;

    @Inject
    public FlyttEnhetTask(BehandlingKandidaterRepository behandlingKandidaterRepository, ProsessTaskTjeneste taskTjeneste) {
        this.taskTjeneste =taskTjeneste;
        this.behandlingKandidaterRepository = behandlingKandidaterRepository;

    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var enhet = Optional.ofNullable(prosessTaskData.getPropertyValue(ENHET_ID)).orElseThrow();
        var kandidater = behandlingKandidaterRepository.finnBehandlingerIkkeAvsluttetPåAngittEnhet(enhet);
        kandidater.forEach(beh -> {
            var taskData = ProsessTaskData.forProsessTask(OppdaterBehandlendeEnhetTask.class);
            taskData.setBehandling(beh.getSaksnummer().getVerdi(), beh.getFagsakId(), beh.getId());
            taskData.setCallIdFraEksisterende();
            taskTjeneste.lagre(taskData);
        });
    }


}

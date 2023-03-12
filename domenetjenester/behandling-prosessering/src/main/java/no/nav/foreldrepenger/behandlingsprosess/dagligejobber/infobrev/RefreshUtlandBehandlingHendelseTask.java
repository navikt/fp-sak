package no.nav.foreldrepenger.behandlingsprosess.dagligejobber.infobrev;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.impl.HendelseForBehandling;
import no.nav.foreldrepenger.behandling.impl.PubliserBehandlingHendelseTask;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@Dependent
@ProsessTask("oppgavebehandling.oppdater.utland")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class RefreshUtlandBehandlingHendelseTask implements ProsessTaskHandler {

    public static final String HENDELSE_TYPE = "hendelseType";

    private final ProsessTaskTjeneste taskTjeneste;
    private final InformasjonssakRepository informasjonssakRepository;

    @Inject
    public RefreshUtlandBehandlingHendelseTask(ProsessTaskTjeneste taskTjeneste,
                                               InformasjonssakRepository informasjonssakRepository) {
        this.taskTjeneste =taskTjeneste;
        this.informasjonssakRepository = informasjonssakRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        informasjonssakRepository.finnUtlandBehandlingerMedÅpentAksjonspunkt().forEach(this::opprettProsessTask);
    }

    private void opprettProsessTask(Long behandlingId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(PubliserBehandlingHendelseTask.class);
        prosessTaskData.setBehandling(behandlingId, behandlingId);
        prosessTaskData.setProperty(PubliserBehandlingHendelseTask.HENDELSE_TYPE, HendelseForBehandling.AKSJONSPUNKT.name());
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskData.setPrioritet(90);
        taskTjeneste.lagre(prosessTaskData);
    }

}

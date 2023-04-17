package no.nav.foreldrepenger.behandling.steg.mottatteopplysninger;

import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingsprosess.dagligejobber.infobrev.InformasjonssakRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@Dependent
@ProsessTask("oppgavebehandling.ventende.utland")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class VentendeUtlandBehandlingTask implements ProsessTaskHandler {

    public static final String ENHET_ID = "enhetId";

    private final ProsessTaskTjeneste taskTjeneste;
    private final InformasjonssakRepository informasjonssakRepository;

    @Inject
    public VentendeUtlandBehandlingTask(ProsessTaskTjeneste taskTjeneste,
                                        InformasjonssakRepository informasjonssakRepository) {
        this.taskTjeneste = taskTjeneste;
        this.informasjonssakRepository = informasjonssakRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        informasjonssakRepository.finnAktiveBehandlingerUtenUtlandMarkering(Optional.ofNullable(prosessTaskData.getPropertyValue(ENHET_ID)).orElseThrow())
            .forEach(fsid -> opprettProsessTask(fsid));
    }

    private void opprettProsessTask(Long behandlingId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(SjekkUtlandBehandlingTask.class);
        prosessTaskData.setProperty(SjekkUtlandBehandlingTask.BEHANDLING_ID, behandlingId.toString());
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskData.setPrioritet(90);
        taskTjeneste.lagre(prosessTaskData);
    }

}

package no.nav.foreldrepenger.mottak.hendelser;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.hendelser.HendelsemottakRepository;
import no.nav.foreldrepenger.mottak.hendelser.kontrakt.Hendelse;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
public class MottattHendelseTjeneste {

    private HendelsemottakRepository hendelsemottakRepository;
    private ProsessTaskRepository prosessTaskRepository;

    MottattHendelseTjeneste() {
        //CDI
    }

    @Inject
    public MottattHendelseTjeneste(HendelsemottakRepository hendelsemottakRepository, ProsessTaskRepository prosessTaskRepository) {
        this.hendelsemottakRepository = hendelsemottakRepository;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    public boolean erHendelseNy(String uid) {
        return hendelsemottakRepository.hendelseErNy(uid);
    }

    public void registrerHendelse(String uid, Hendelse hendelse) {
        hendelsemottakRepository.registrerMottattHendelse(uid);
        ProsessTaskData taskData = new ProsessTaskData(KlargjørHendelseTask.TASKTYPE);
        taskData.setPayload(JsonMapper.toJson(hendelse));
        taskData.setProperty(KlargjørHendelseTask.PROPERTY_HENDELSE_TYPE, hendelse.getHendelseKode());
        taskData.setProperty(KlargjørHendelseTask.PROPERTY_UID, uid);
        taskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(taskData);
    }
}

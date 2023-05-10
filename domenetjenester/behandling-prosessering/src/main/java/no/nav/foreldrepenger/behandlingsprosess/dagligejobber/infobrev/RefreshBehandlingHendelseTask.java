package no.nav.foreldrepenger.behandlingsprosess.dagligejobber.infobrev;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.impl.HendelseForBehandling;
import no.nav.foreldrepenger.behandling.impl.PubliserBehandlingHendelseTask;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.FagsakMarkering;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@Dependent
@ProsessTask("oppgavebehandling.oppdater.markertsak")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class RefreshBehandlingHendelseTask implements ProsessTaskHandler {

    private final ProsessTaskTjeneste taskTjeneste;
    private final BehandlingRepository behandlingRepository;
    private final FagsakEgenskapRepository fagsakEgenskapRepository;
    private final InformasjonssakRepository informasjonssakRepository;

    @Inject
    public RefreshBehandlingHendelseTask(ProsessTaskTjeneste taskTjeneste,
                                         BehandlingRepository behandlingRepository,
                                         FagsakEgenskapRepository fagsakEgenskapRepository,
                                         InformasjonssakRepository informasjonssakRepository) {
        this.taskTjeneste =taskTjeneste;
        this.informasjonssakRepository = informasjonssakRepository;
        this.behandlingRepository = behandlingRepository;
        this.fagsakEgenskapRepository = fagsakEgenskapRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        // Fjernes etter initiell merking
        informasjonssakRepository.finnSakerSomKanMerkesNæring()
            .forEach(f -> fagsakEgenskapRepository.lagreEgenskapUtenHistorikk(f, FagsakMarkering.SELVSTENDIG_NÆRING));


        // Generell trigger for oppdatering
        informasjonssakRepository.finnAktiveBehandlingerSomSkalOppdateres().forEach(this::opprettProsessTask);
    }

    private void opprettProsessTask(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var prosessTaskData = ProsessTaskData.forProsessTask(PubliserBehandlingHendelseTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandlingId);
        prosessTaskData.setProperty(PubliserBehandlingHendelseTask.HENDELSE_TYPE, HendelseForBehandling.AKSJONSPUNKT.name());
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskData.setPrioritet(90);
        taskTjeneste.lagre(prosessTaskData);
    }

}

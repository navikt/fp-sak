package no.nav.foreldrepenger.domene.vedtak;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.domene.vedtak.ekstern.SettUtbetalingPåVentPrivatArbeidsgiverTask;
import no.nav.foreldrepenger.domene.vedtak.ekstern.VurderOppgaveArenaTask;
import no.nav.foreldrepenger.domene.vedtak.intern.AvsluttBehandlingTask;
import no.nav.foreldrepenger.domene.vedtak.intern.SendVedtaksbrevTask;
import no.nav.foreldrepenger.domene.vedtak.intern.SettFagsakRelasjonAvslutningsdatoTask;
import no.nav.foreldrepenger.domene.vedtak.task.VurderOgSendØkonomiOppdragTask;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;


public abstract class OpprettProsessTaskIverksettFelles implements OpprettProsessTaskIverksett {

    protected ProsessTaskRepository prosessTaskRepository;
    protected OppgaveTjeneste oppgaveTjeneste;

    protected OpprettProsessTaskIverksettFelles() {
        // for CDI proxy
    }

    public OpprettProsessTaskIverksettFelles(ProsessTaskRepository prosessTaskRepository,
                                             OppgaveTjeneste oppgaveTjeneste) {
        this.prosessTaskRepository = prosessTaskRepository;
        this.oppgaveTjeneste = oppgaveTjeneste;
    }


    @Override
    public void opprettIverksettingstasker(Behandling behandling, List<String> initiellTaskNavn) {
        ProsessTaskData avsluttBehandling = getProsesstaskFor(AvsluttBehandlingTask.TASKTYPE);
        Optional<ProsessTaskData> avsluttOppgave = oppgaveTjeneste.opprettTaskAvsluttOppgave(behandling, behandling.erRevurdering() ? OppgaveÅrsak.REVURDER : OppgaveÅrsak.BEHANDLE_SAK, false);
        ProsessTaskData sendVedtaksbrev = getProsesstaskFor(SendVedtaksbrevTask.TASKTYPE);

        List<ProsessTaskData> initiellTasker = new ArrayList<>();
        initiellTaskNavn.forEach(tn -> initiellTasker.add(new ProsessTaskData(tn)));

        ProsessTaskData vurderOgSendØkonomiOppdrag = getProsesstaskFor(VurderOgSendØkonomiOppdragTask.TASKTYPE);
        ProsessTaskData vedtakTilDatavarehus = getProsesstaskFor(VEDTAK_TIL_DATAVAREHUS_TASK);
        ProsessTaskData vurderOppgaveArena = getProsesstaskFor(VurderOppgaveArenaTask.TASKTYPE);
        ProsessTaskData vurderSettPåVentUtbetalingPrivat = getProsesstaskFor(SettUtbetalingPåVentPrivatArbeidsgiverTask.TASKTYPE);
        ProsessTaskData settFagsakRelasjonAvsluttningsdato = getProsesstaskFor(SettFagsakRelasjonAvslutningsdatoTask.TASKTYPE);

        ProsessTaskGruppe taskData = new ProsessTaskGruppe();
        initiellTasker.forEach(taskData::addNesteSekvensiell);

        List<ProsessTaskData> parallelle = new ArrayList<>();
        parallelle.add(sendVedtaksbrev);
        parallelle.add(vurderOgSendØkonomiOppdrag);
        avsluttOppgave.ifPresent(parallelle::add);

        taskData.addNesteParallell(parallelle);
        taskData.addNesteSekvensiell(vurderOppgaveArena);
        taskData.addNesteSekvensiell(vurderSettPåVentUtbetalingPrivat);
        taskData.addNesteSekvensiell(vedtakTilDatavarehus);
        taskData.addNesteSekvensiell(avsluttBehandling);
        taskData.addNesteSekvensiell(settFagsakRelasjonAvsluttningsdato);

        taskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());

        taskData.setCallIdFraEksisterende();

        prosessTaskRepository.lagre(taskData);
    }

    protected ProsessTaskData getProsesstaskFor(String tasktype) {
        var task = new ProsessTaskData(tasktype);
        task.setPrioritet(50);
        return task;
    }

}

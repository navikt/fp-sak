package no.nav.foreldrepenger.domene.vedtak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.datavarehus.task.VedtakTilDatavarehusTask;
import no.nav.foreldrepenger.domene.vedtak.ekstern.SettUtbetalingPåVentPrivatArbeidsgiverTask;
import no.nav.foreldrepenger.domene.vedtak.ekstern.VurderOppgaveArenaTask;
import no.nav.foreldrepenger.domene.vedtak.intern.AvsluttBehandlingTask;
import no.nav.foreldrepenger.domene.vedtak.intern.SendVedtaksbrevTask;
import no.nav.foreldrepenger.domene.vedtak.intern.SettFagsakRelasjonAvslutningsdatoTask;
import no.nav.foreldrepenger.domene.vedtak.task.VurderOgSendØkonomiOppdragTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class OpprettProsessTaskIverksett {

    private ProsessTaskTjeneste taskTjeneste;


    public OpprettProsessTaskIverksett() {
        // for CDI proxy
    }

    @Inject
    public OpprettProsessTaskIverksett(ProsessTaskTjeneste taskTjeneste) {
        this.taskTjeneste = taskTjeneste;
    }

    public void opprettIverksettingTasks(Behandling behandling) {
        var taskGruppe = new ProsessTaskGruppe();
        // Felles
        var avsluttBehandling = getProsesstaskFor(TaskType.forProsessTask(AvsluttBehandlingTask.class));

        // Send brev og oppdrag i parallell
        List<ProsessTaskData> parallelle = new ArrayList<>();
        parallelle.add(getProsesstaskFor(TaskType.forProsessTask(SendVedtaksbrevTask.class)));
        if (behandling.erYtelseBehandling()) {
            parallelle.add(getProsesstaskFor(TaskType.forProsessTask(VurderOgSendØkonomiOppdragTask.class)));
        }
        taskGruppe.addNesteParallell(parallelle);

        // Spesifikke tasks
        if (behandling.erYtelseBehandling()) {
            leggTilTasksYtelsesBehandling(behandling, taskGruppe);
        }
        // Siste i gruppen
        taskGruppe.addNesteSekvensiell(avsluttBehandling);
        taskGruppe.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        taskGruppe.setCallIdFraEksisterende();
        taskTjeneste.lagre(taskGruppe);
    }

    private void leggTilTasksYtelsesBehandling(Behandling behandling, ProsessTaskGruppe taskGruppe) {
        if (FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType())) {
            taskGruppe.addNesteSekvensiell(getProsesstaskFor(TaskType.forProsessTask(VurderOppgaveArenaTask.class)));
        }
        if (!FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType())) {
            taskGruppe.addNesteSekvensiell(getProsesstaskFor(TaskType.forProsessTask(SettUtbetalingPåVentPrivatArbeidsgiverTask.class)));
            taskGruppe.addNesteSekvensiell(getProsesstaskFor(TaskType.forProsessTask(SettFagsakRelasjonAvslutningsdatoTask.class)));
        }
        taskGruppe.addNesteSekvensiell(getProsesstaskFor(TaskType.forProsessTask(VedtakTilDatavarehusTask.class)));
    }

    private ProsessTaskData getProsesstaskFor(TaskType tasktype) {
        var task = ProsessTaskData.forTaskType(tasktype);
        task.setPrioritet(50);
        return task;
    }

}

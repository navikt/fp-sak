package no.nav.foreldrepenger.domene.vedtak;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.datavarehus.v2.SendStønadsstatistikkForVedtakTask;
import no.nav.foreldrepenger.domene.vedtak.ekstern.SettUtbetalingPåVentPrivatArbeidsgiverTask;
import no.nav.foreldrepenger.domene.vedtak.ekstern.VurderOppgaveArenaTask;
import no.nav.foreldrepenger.domene.vedtak.intern.AvsluttBehandlingTask;
import no.nav.foreldrepenger.domene.vedtak.intern.SendVedtaksbrevTask;
import no.nav.foreldrepenger.domene.vedtak.intern.SettFagsakRelasjonAvslutningsdatoTask;
import no.nav.foreldrepenger.domene.vedtak.task.VurderOgSendØkonomiOppdragTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

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
        var avsluttBehandling = ProsessTaskData.forProsessTask(AvsluttBehandlingTask.class);

        // Send brev og oppdrag i parallell
        List<ProsessTaskData> parallelle = new ArrayList<>();
        parallelle.add(ProsessTaskData.forProsessTask(SendVedtaksbrevTask.class));
        if (behandling.erYtelseBehandling()) {
            parallelle.add(ProsessTaskData.forProsessTask(VurderOgSendØkonomiOppdragTask.class));
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
            taskGruppe.addNesteSekvensiell(ProsessTaskData.forProsessTask(VurderOppgaveArenaTask.class));
        }
        if (!FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType())) {
            taskGruppe.addNesteSekvensiell(ProsessTaskData.forProsessTask(SettUtbetalingPåVentPrivatArbeidsgiverTask.class));
            taskGruppe.addNesteSekvensiell(ProsessTaskData.forProsessTask(SettFagsakRelasjonAvslutningsdatoTask.class));
        }
        taskGruppe.addNesteSekvensiell(ProsessTaskData.forProsessTask(SendStønadsstatistikkForVedtakTask.class));
    }

}

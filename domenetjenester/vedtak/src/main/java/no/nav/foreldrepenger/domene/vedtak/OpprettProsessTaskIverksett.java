package no.nav.foreldrepenger.domene.vedtak;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.datavarehus.task.VedtakTilDatavarehusTask;
import no.nav.foreldrepenger.domene.vedtak.ekstern.SettUtbetalingPåVentPrivatArbeidsgiverTask;
import no.nav.foreldrepenger.domene.vedtak.ekstern.VurderOppgaveArenaTask;
import no.nav.foreldrepenger.domene.vedtak.intern.AvsluttBehandlingTask;
import no.nav.foreldrepenger.domene.vedtak.intern.SendVedtaksbrevTask;
import no.nav.foreldrepenger.domene.vedtak.intern.SettFagsakRelasjonAvslutningsdatoTask;
import no.nav.foreldrepenger.domene.vedtak.task.VurderOgSendØkonomiOppdragTask;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveVurderKonsekvensTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

@ApplicationScoped
public class OpprettProsessTaskIverksett {

    private static final String VKY_KLAGE_BESKRIVELSE = "Vedtaket er opphevet eller omgjort. Opprett en ny behandling.";
    private static final String VKY_KLAGE_UENDRET_BESKRIVELSE = "Vedtaket er stadfestet eller klagen avvist av NAV Klageinstans, dette til informasjon.";
    private static final String VKY_ANKE_BESKRIVELSE = "Vedtaket er omgjort, opphevet eller hjemsendt. Opprett en ny behandling.";
    private static final String VKY_TRR_UENDRET_BESKRIVELSE = "Vedtaket er stadfestet eller anken avvist av Trygderetten, dette til informasjon.";

    private static final Set<AnkeVurdering> ANKE_ENDRES = Set.of(AnkeVurdering.ANKE_OPPHEVE_OG_HJEMSENDE, AnkeVurdering.ANKE_OMGJOER, AnkeVurdering.ANKE_HJEMSEND_UTEN_OPPHEV);
    private static final Set<KlageVurdering> KLAGE_ENDRES = Set.of(KlageVurdering.MEDHOLD_I_KLAGE, KlageVurdering.OPPHEVE_YTELSESVEDTAK, KlageVurdering.HJEMSENDE_UTEN_Å_OPPHEVE);

    private ProsessTaskTjeneste taskTjeneste;
    private OppgaveTjeneste oppgaveTjeneste;
    private BehandlingRepository behandlingRepository;
    private AnkeRepository ankeRepository;
    private KlageRepository klageRepository;


    public OpprettProsessTaskIverksett() {
        // for CDI proxy
    }

    @Inject
    public OpprettProsessTaskIverksett(ProsessTaskTjeneste taskTjeneste,
                                       BehandlingRepository behandlingRepository,
                                       AnkeRepository ankeRepository,
                                       KlageRepository klageRepository,
                                       OppgaveTjeneste oppgaveTjeneste) {
        this.taskTjeneste = taskTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.ankeRepository = ankeRepository;
        this.klageRepository = klageRepository;
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    public void opprettIverksettingTasks(Behandling behandling) {
        var taskGruppe = new ProsessTaskGruppe();
        // Felles
        var avsluttBehandling = getProsesstaskFor(TaskType.forProsessTask(AvsluttBehandlingTask.class));
        var avsluttOppgave = oppgaveTjeneste.opprettTaskAvsluttOppgave(behandling, behandling.erRevurdering() ? OppgaveÅrsak.REVURDER : OppgaveÅrsak.BEHANDLE_SAK, false);

        // Send brev og oppdrag i parallell
        List<ProsessTaskData> parallelle = new ArrayList<>();
        parallelle.add(getProsesstaskFor(TaskType.forProsessTask(SendVedtaksbrevTask.class)));
        avsluttOppgave.ifPresent(parallelle::add);
        if (behandling.erYtelseBehandling()) {
            parallelle.add(getProsesstaskFor(TaskType.forProsessTask(VurderOgSendØkonomiOppdragTask.class)));
        }
        taskGruppe.addNesteParallell(parallelle);

        // Spesifikke tasks
        if (behandling.erYtelseBehandling()) {
            leggTilTasksYtelsesBehandling(behandling, taskGruppe);
        } else {
            leggTilTasksIkkeYtelse(behandling, taskGruppe);
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

    private void leggTilTasksIkkeYtelse(Behandling behandling, ProsessTaskGruppe gruppe) {
        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            opprettTaskDataForKlage(behandling).ifPresent(gruppe::addNesteSekvensiell);
        }
        if (BehandlingType.ANKE.equals(behandling.getType())) {
            opprettTaskDataForAnke(behandling).ifPresent(gruppe::addNesteSekvensiell);
        }
    }

    private Optional<ProsessTaskData> opprettTaskDataForAnke(Behandling behandling) {
        var vurderingsresultat = ankeRepository.hentAnkeVurderingResultat(behandling.getId()).orElse(null);
        if (vurderingsresultat == null)
            return Optional.empty();
        var vurdering = vurderingsresultat.getAnkeVurdering();
        var trygderett = vurderingsresultat.getTrygderettVurdering();
        var sisteYtelseBeh = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(behandling.getFagsakId()).orElse(behandling);
        if (ANKE_ENDRES.contains(vurdering) || ANKE_ENDRES.contains(trygderett)) {
            return Optional.of(lagOpprettVurderKonsekvensTask(sisteYtelseBeh, VKY_ANKE_BESKRIVELSE));
        }
        return AnkeVurdering.UDEFINERT.equals(trygderett) ? Optional.empty() : Optional.of(lagOpprettVurderKonsekvensTask(sisteYtelseBeh, VKY_TRR_UENDRET_BESKRIVELSE));
    }

    private Optional<ProsessTaskData> opprettTaskDataForKlage(Behandling behandling) {
        var vurderingResultat = klageRepository.hentGjeldendeKlageVurderingResultat(behandling).orElse(null);
        if (vurderingResultat == null)
            return Optional.empty();
        var vurdering = vurderingResultat.getKlageVurdering();
        var sisteYtelseBeh = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(behandling.getFagsakId()).orElse(behandling);
        if (KLAGE_ENDRES.contains(vurdering)) {
            return Optional.of(lagOpprettVurderKonsekvensTask(sisteYtelseBeh, VKY_KLAGE_BESKRIVELSE));
        }
        return (KlageVurdering.STADFESTE_YTELSESVEDTAK.equals(vurdering) || (KlageVurdering.AVVIS_KLAGE.equals(vurdering) && KlageVurdertAv.NK.equals(vurderingResultat.getKlageVurdertAv())))
            ? Optional.of(lagOpprettVurderKonsekvensTask(sisteYtelseBeh, VKY_KLAGE_UENDRET_BESKRIVELSE)) : Optional.empty();
    }

    private ProsessTaskData lagOpprettVurderKonsekvensTask(Behandling behandling, String beskrivelse) {
        var opprettOppgave = ProsessTaskData.forProsessTask(OpprettOppgaveVurderKonsekvensTask.class);
        opprettOppgave.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_BEHANDLENDE_ENHET, behandling.getBehandlendeEnhet());
        opprettOppgave.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_BESKRIVELSE, beskrivelse);
        opprettOppgave.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_PRIORITET, OpprettOppgaveVurderKonsekvensTask.PRIORITET_HØY);
        return opprettOppgave;
    }

    private ProsessTaskData getProsesstaskFor(TaskType tasktype) {
        var task = ProsessTaskData.forTaskType(tasktype);
        task.setPrioritet(50);
        return task;
    }

}

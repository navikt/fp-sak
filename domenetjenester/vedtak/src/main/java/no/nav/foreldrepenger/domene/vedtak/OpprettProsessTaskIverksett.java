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
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
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
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
public class OpprettProsessTaskIverksett {

    public static final String VEDTAK_TIL_DATAVAREHUS_TASK = "iverksetteVedtak.vedtakTilDatavarehus";

    private static final String VKY_KLAGE_BESKRIVELSE = "Vedtaket er opphevet eller omgjort. Opprett en ny behandling.";
    private static final String VKY_ANKE_BESKRIVELSE = "Vedtaket er omgjort, opphevet eller hjemsendt. Opprett en ny behandling.";
    private static final String VKY_TRR_UENDRET_BESKRIVELSE = "Vedtaket er stadfestet eller avvist av Trygderetten, dette til informasjon.";

    private static final Set<AnkeVurdering> ANKE_ENDRES = Set.of(AnkeVurdering.ANKE_OPPHEVE_OG_HJEMSENDE, AnkeVurdering.ANKE_OMGJOER, AnkeVurdering.ANKE_HJEMSEND_UTEN_OPPHEV);

    private ProsessTaskRepository prosessTaskRepository;
    private OppgaveTjeneste oppgaveTjeneste;
    private BehandlingRepository behandlingRepository;
    private AnkeRepository ankeRepository;
    private KlageRepository klageRepository;


    public OpprettProsessTaskIverksett() {
        // for CDI proxy
    }

    @Inject
    public OpprettProsessTaskIverksett(ProsessTaskRepository prosessTaskRepository,
                                       BehandlingRepository behandlingRepository,
                                       AnkeRepository ankeRepository,
                                       KlageRepository klageRepository,
                                       OppgaveTjeneste oppgaveTjeneste) {
        this.prosessTaskRepository = prosessTaskRepository;
        this.behandlingRepository = behandlingRepository;
        this.ankeRepository = ankeRepository;
        this.klageRepository = klageRepository;
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    public void opprettIverksettingTasks(Behandling behandling) {
        ProsessTaskGruppe taskGruppe = new ProsessTaskGruppe();
        // Felles
        ProsessTaskData avsluttBehandling = getProsesstaskFor(AvsluttBehandlingTask.TASKTYPE);
        Optional<ProsessTaskData> avsluttOppgave = oppgaveTjeneste.opprettTaskAvsluttOppgave(behandling, behandling.erRevurdering() ? OppgaveÅrsak.REVURDER : OppgaveÅrsak.BEHANDLE_SAK, false);

        // Send brev og oppdrag i parallell
        List<ProsessTaskData> parallelle = new ArrayList<>();
        parallelle.add(getProsesstaskFor(SendVedtaksbrevTask.TASKTYPE));
        avsluttOppgave.ifPresent(parallelle::add);
        if (behandling.erYtelseBehandling()) {
            parallelle.add(getProsesstaskFor(VurderOgSendØkonomiOppdragTask.TASKTYPE));
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
        prosessTaskRepository.lagre(taskGruppe);
    }

    private void leggTilTasksYtelsesBehandling(Behandling behandling, ProsessTaskGruppe taskGruppe) {
        if (!FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType())) {
            taskGruppe.addNesteSekvensiell(getProsesstaskFor(VurderOppgaveArenaTask.TASKTYPE));
            taskGruppe.addNesteSekvensiell(getProsesstaskFor(SettUtbetalingPåVentPrivatArbeidsgiverTask.TASKTYPE));
            taskGruppe.addNesteSekvensiell(getProsesstaskFor(SettFagsakRelasjonAvslutningsdatoTask.TASKTYPE));
        }
        taskGruppe.addNesteSekvensiell(getProsesstaskFor(VEDTAK_TIL_DATAVAREHUS_TASK));
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
        AnkeVurderingResultatEntitet vurderingsresultat = ankeRepository.hentAnkeVurderingResultat(behandling.getId()).orElse(null);
        if (vurderingsresultat == null)
            return Optional.empty();
        AnkeVurdering vurdering = vurderingsresultat.getAnkeVurdering();
        AnkeVurdering trygderett = vurderingsresultat.getTrygderettVurdering();
        Behandling sisteYtelseBeh = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(behandling.getFagsakId()).orElse(behandling);
        if (ANKE_ENDRES.contains(vurdering) || ANKE_ENDRES.contains(trygderett)) {
            return Optional.of(lagOpprettVurderKonsekvensTask(sisteYtelseBeh, VKY_ANKE_BESKRIVELSE));
        }
        return AnkeVurdering.UDEFINERT.equals(trygderett) ? Optional.empty() : Optional.of(lagOpprettVurderKonsekvensTask(sisteYtelseBeh, VKY_TRR_UENDRET_BESKRIVELSE));
    }

    private Optional<ProsessTaskData> opprettTaskDataForKlage(Behandling behandling) {
        Optional<KlageVurderingResultat> vurderingsresultat = klageRepository.hentGjeldendeKlageVurderingResultat(behandling);
        if (vurderingsresultat.isPresent()) {
            KlageVurdering vurdering = vurderingsresultat.get().getKlageVurdering();
            if (KlageVurdering.MEDHOLD_I_KLAGE.equals(vurdering) || KlageVurdering.OPPHEVE_YTELSESVEDTAK.equals(vurdering)
                || KlageVurdering.HJEMSENDE_UTEN_Å_OPPHEVE.equals(vurdering)) {
                Behandling sisteYtelseBeh = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(behandling.getFagsakId()).orElse(behandling);
                return Optional.of(lagOpprettVurderKonsekvensTask(sisteYtelseBeh, VKY_KLAGE_BESKRIVELSE));
            }
        }
        return Optional.empty();
    }

    private ProsessTaskData lagOpprettVurderKonsekvensTask(Behandling behandling, String beskrivelse) {
        ProsessTaskData opprettOppgave = new ProsessTaskData(OpprettOppgaveVurderKonsekvensTask.TASKTYPE);
        opprettOppgave.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_BEHANDLENDE_ENHET, behandling.getBehandlendeEnhet());
        opprettOppgave.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_BESKRIVELSE, beskrivelse);
        opprettOppgave.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_PRIORITET, OpprettOppgaveVurderKonsekvensTask.PRIORITET_HØY);
        return opprettOppgave;
    }

    private ProsessTaskData getProsesstaskFor(String tasktype) {
        var task = new ProsessTaskData(tasktype);
        task.setPrioritet(50);
        return task;
    }

}

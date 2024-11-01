package no.nav.foreldrepenger.mottak.vedtak.observer;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveVurderKonsekvensTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
@ProsessTask(value = "iverksetteVedtak.håndterKlageAnke", prioritet = 2, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class MottaKlageAnkeVedtakTask extends GenerellProsessTask {

    private static final String VKY_KLAGE_BESKRIVELSE = "Vedtaket er opphevet eller omgjort. Opprett en ny behandling.";
    private static final String VKY_KLAGE_UENDRET_BESKRIVELSE = "Vedtaket er stadfestet eller klagen avvist av NAV Klageinstans, dette til informasjon.";
    private static final String VKY_ANKE_BESKRIVELSE = "Vedtaket er omgjort, opphevet eller hjemsendt. Opprett en ny behandling.";
    private static final String VKY_TRR_UENDRET_BESKRIVELSE = "Vedtaket er stadfestet eller anken avvist av Trygderetten, dette til informasjon.";

    private static final Set<AnkeVurdering> ANKE_ENDRES = Set.of(AnkeVurdering.ANKE_OPPHEVE_OG_HJEMSENDE, AnkeVurdering.ANKE_OMGJOER, AnkeVurdering.ANKE_HJEMSEND_UTEN_OPPHEV);
    private static final Set<KlageVurdering> KLAGE_ENDRES = Set.of(KlageVurdering.MEDHOLD_I_KLAGE, KlageVurdering.OPPHEVE_YTELSESVEDTAK, KlageVurdering.HJEMSENDE_UTEN_Å_OPPHEVE);

    private ProsessTaskTjeneste taskTjeneste;
    private BehandlingRepository behandlingRepository;
    private AnkeRepository ankeRepository;
    private KlageRepository klageRepository;


    public MottaKlageAnkeVedtakTask() {
        // for CDI proxy
    }

    @Inject
    public MottaKlageAnkeVedtakTask(ProsessTaskTjeneste taskTjeneste,
                                    BehandlingRepository behandlingRepository,
                                    AnkeRepository ankeRepository,
                                    KlageRepository klageRepository) {
        this.taskTjeneste = taskTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.ankeRepository = ankeRepository;
        this.klageRepository = klageRepository;
    }

    @Override
    public void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        opprettKlageAnkeTasks(behandling);
    }

    void opprettKlageAnkeTasks(Behandling behandling) {
        // TFP-4952: legg til behandlingsoppretting nedenfor
        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            opprettTaskDataForKlage(behandling);
        } else if (BehandlingType.ANKE.equals(behandling.getType())) {
            opprettTaskDataForAnke(behandling);
        }
    }

    private void opprettTaskDataForAnke(Behandling behandling) {
        var vurderingsresultat = ankeRepository.hentAnkeVurderingResultat(behandling.getId()).orElse(null);
        if (vurderingsresultat == null)
            return;
        var vurdering = vurderingsresultat.getAnkeVurdering();
        var trygderett = vurderingsresultat.getTrygderettVurdering();
        var sisteYtelseBeh = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(behandling.getFagsakId()).orElse(behandling);
        if (ANKE_ENDRES.contains(vurdering) || ANKE_ENDRES.contains(trygderett)) {
            lagOpprettVurderKonsekvensTask(sisteYtelseBeh, VKY_ANKE_BESKRIVELSE);
        } else if (!AnkeVurdering.UDEFINERT.equals(trygderett)) {
            lagOpprettVurderKonsekvensTask(sisteYtelseBeh, VKY_TRR_UENDRET_BESKRIVELSE);
        }
    }

    private void opprettTaskDataForKlage(Behandling behandling) {
        var vurderingResultat = klageRepository.hentGjeldendeKlageVurderingResultat(behandling).orElse(null);
        if (vurderingResultat == null)
            return;
        var vurdering = vurderingResultat.getKlageVurdering();
        var sisteYtelseBeh = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(behandling.getFagsakId()).orElse(behandling);
        if (KLAGE_ENDRES.contains(vurdering)) {
            lagOpprettVurderKonsekvensTask(sisteYtelseBeh, VKY_KLAGE_BESKRIVELSE);
        } else if (KlageVurdering.STADFESTE_YTELSESVEDTAK.equals(vurdering)
            || KlageVurdering.AVVIS_KLAGE.equals(vurdering) && KlageVurdertAv.NK.equals(vurderingResultat.getKlageVurdertAv())) {
            lagOpprettVurderKonsekvensTask(sisteYtelseBeh, VKY_KLAGE_UENDRET_BESKRIVELSE);
        }
    }

    private void lagOpprettVurderKonsekvensTask(Behandling sisteYtelseBehandling, String beskrivelse) {
        var opprettOppgave = ProsessTaskData.forProsessTask(OpprettOppgaveVurderKonsekvensTask.class);
        opprettOppgave.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_BEHANDLENDE_ENHET, sisteYtelseBehandling.getBehandlendeEnhet());
        opprettOppgave.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_BESKRIVELSE, beskrivelse);
        opprettOppgave.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_PRIORITET, OpprettOppgaveVurderKonsekvensTask.PRIORITET_HØY);
        opprettOppgave.setCallIdFraEksisterende();
        opprettOppgave.setBehandling(sisteYtelseBehandling.getFagsakId(), sisteYtelseBehandling.getId(), sisteYtelseBehandling.getAktørId().getId());
        taskTjeneste.lagre(opprettOppgave);
    }

}

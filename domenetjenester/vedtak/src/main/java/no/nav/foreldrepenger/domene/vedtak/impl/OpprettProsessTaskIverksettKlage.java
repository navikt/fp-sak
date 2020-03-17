package no.nav.foreldrepenger.domene.vedtak.impl;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.vedtak.OpprettProsessTaskIverksett;
import no.nav.foreldrepenger.domene.vedtak.intern.AvsluttBehandlingTask;
import no.nav.foreldrepenger.domene.vedtak.intern.SendVedtaksbrevTask;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveVurderKonsekvensTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
public class OpprettProsessTaskIverksettKlage implements OpprettProsessTaskIverksett {

    private static final String BESKRIVELSESTEKST = "Vedtaket er opphevet eller omgjort. Opprett en ny behandling.";

    private ProsessTaskRepository prosessTaskRepository;
    private OppgaveTjeneste oppgaveTjeneste;
    private BehandlingRepository behandlingRepository;
    private KlageRepository klageRepository;

    OpprettProsessTaskIverksettKlage() {
        // for CDI proxy
    }

    @Inject
    public OpprettProsessTaskIverksettKlage(BehandlingRepository behandlingRepository,
                                            KlageRepository klageRepository,
                                            ProsessTaskRepository prosessTaskRepository,
                                            OppgaveTjeneste oppgaveTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.klageRepository = klageRepository;
        this.prosessTaskRepository = prosessTaskRepository;
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    @Override
    public void opprettIverksettingstasker(Behandling behandling, @SuppressWarnings("unused") List<String> inititellTaskNavn) {
        ProsessTaskData avsluttBehandling = new ProsessTaskData(AvsluttBehandlingTask.TASKTYPE);
        Optional<ProsessTaskData> avsluttOppgave = oppgaveTjeneste.opprettTaskAvsluttOppgave(behandling, behandling.erRevurdering() ? OppgaveÅrsak.REVURDER : OppgaveÅrsak.BEHANDLE_SAK, false);
        ProsessTaskData sendVedtaksbrev = new ProsessTaskData(SendVedtaksbrevTask.TASKTYPE);

        ProsessTaskGruppe taskData = opprettTaskDataForKlage(behandling, avsluttBehandling, sendVedtaksbrev, avsluttOppgave);

        taskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());

        taskData.setCallIdFraEksisterende();

        prosessTaskRepository.lagre(taskData);

    }

    private ProsessTaskGruppe opprettTaskDataForKlage(Behandling behandling, ProsessTaskData avsluttBehandling,
                                                      ProsessTaskData sendVedtaksbrev, Optional<ProsessTaskData> avsluttOppgave) {
        ProsessTaskGruppe taskData = new ProsessTaskGruppe();
        if (avsluttOppgave.isPresent()) {
            taskData.addNesteParallell(sendVedtaksbrev, avsluttOppgave.get());
        } else {
            taskData.addNesteSekvensiell(sendVedtaksbrev);
        }
        taskData.addNesteSekvensiell(avsluttBehandling);
        Optional<KlageVurderingResultat> vurderingsresultat = klageRepository.hentGjeldendeKlageVurderingResultat(behandling);
        if (vurderingsresultat.isPresent()) {
            KlageVurdering vurdering = vurderingsresultat.get().getKlageVurdering();
            if (KlageVurdering.MEDHOLD_I_KLAGE.equals(vurdering) || KlageVurdering.OPPHEVE_YTELSESVEDTAK.equals(vurdering)
                || KlageVurdering.HJEMSENDE_UTEN_Å_OPPHEVE.equals(vurdering)) {
                Behandling sisteFørstegangsbehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(behandling.getFagsakId()).orElse(behandling);
                ProsessTaskData opprettOppgave = new ProsessTaskData(OpprettOppgaveVurderKonsekvensTask.TASKTYPE);
                opprettOppgave.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_BEHANDLENDE_ENHET, sisteFørstegangsbehandling.getBehandlendeEnhet());
                opprettOppgave.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_BESKRIVELSE, BESKRIVELSESTEKST);
                opprettOppgave.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_PRIORITET, OpprettOppgaveVurderKonsekvensTask.PRIORITET_HØY);
                taskData.addNesteSekvensiell(opprettOppgave);
            }
        }
        return taskData;
    }
}

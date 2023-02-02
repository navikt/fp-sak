package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.observer;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingskontrollEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKobling;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKoblingRepository;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveForBehandlingTask;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveGodkjennVedtakTask;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveRegistrerSøknadTask;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.Totrinnsvurdering;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;
import no.nav.vedtak.log.mdc.MDCOperations;

/**
 * Observerer behandlinger med åpne aksjonspunkter og oppretter deretter oppgave i Gsak.
 */
@ApplicationScoped
public class OpprettOppgaveEventObserver {

    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository;
    private OppgaveTjeneste oppgaveTjeneste;
    private ProsessTaskTjeneste taskTjeneste;
    private TotrinnTjeneste totrinnTjeneste;
    private BehandlingRepository behandlingRepository;

    @Inject
    public OpprettOppgaveEventObserver(OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository
                                       , OppgaveTjeneste oppgaveTjeneste
                                       , ProsessTaskTjeneste taskTjeneste
                                       , BehandlingRepository behandlingRepository
                                       , TotrinnTjeneste totrinnTjeneste) {
        this.oppgaveBehandlingKoblingRepository = oppgaveBehandlingKoblingRepository;
        this.oppgaveTjeneste = oppgaveTjeneste;
        this.taskTjeneste = taskTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.totrinnTjeneste = totrinnTjeneste;
    }

    /**
     * Håndterer oppgave etter at behandlingskontroll er kjørt ferdig.
     */
    public void opprettOppgaveDersomDetErÅpneAksjonspunktForAktivtBehandlingSteg(@Observes BehandlingskontrollEvent.StoppetEvent event) {
        var behandling = behandlingRepository.hentBehandling(event.getBehandlingId());
        if (behandling.isBehandlingPåVent()) {
            oppgaveTjeneste.opprettTaskAvsluttOppgave(behandling);
            return;
        }
        var åpneAksjonspunkt = filterAksjonspunkt(behandling.getÅpneAksjonspunkter(AksjonspunktType.MANUELL), event);

        var totrinnsvurderings = totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(behandling.getId());
        //TODO(OJR) kunne informasjonen om hvilken oppgaveårsak som skal opprettes i GSAK være knyttet til AksjonspunktDef?
        if (!åpneAksjonspunkt.isEmpty()) {
            if (MDCOperations.getCallId() == null || MDCOperations.getCallId().isBlank()) {
                MDCOperations.putCallId(MDCOperations.generateCallId());
            }
            if (harAksjonspunkt(åpneAksjonspunkt, AksjonspunktDefinisjon.REGISTRER_PAPIRSØKNAD_ENGANGSSTØNAD) ||
                harAksjonspunkt(åpneAksjonspunkt, AksjonspunktDefinisjon.REGISTRER_PAPIRSØKNAD_FORELDREPENGER) ||
                harAksjonspunkt(åpneAksjonspunkt, AksjonspunktDefinisjon.REGISTRER_PAPIR_ENDRINGSØKNAD_FORELDREPENGER) ||
                harAksjonspunkt(åpneAksjonspunkt, AksjonspunktDefinisjon.REGISTRER_PAPIRSØKNAD_SVANGERSKAPSPENGER)) {

                var enkeltTask = opprettProsessTaskData(behandling, TaskType.forProsessTask(OpprettOppgaveRegistrerSøknadTask.class));
                enkeltTask.setCallIdFraEksisterende();
                taskTjeneste.lagre(enkeltTask);
            } else if (harAksjonspunkt(åpneAksjonspunkt, AksjonspunktDefinisjon.FATTER_VEDTAK)) {
                oppgaveTjeneste.avsluttOppgaveOgStartTask(behandling, behandling.erRevurdering() ? OppgaveÅrsak.REVURDER : OppgaveÅrsak.BEHANDLE_SAK, TaskType.forProsessTask(OpprettOppgaveGodkjennVedtakTask.class));
            } else if (erSendtTilbakeFraBeslutter(totrinnsvurderings)) {
                opprettOppgaveVedBehov(behandling);
            } else {
                opprettOppgaveVedBehov(behandling);
            }
        }
    }

    private void opprettOppgaveVedBehov(Behandling behandling) {
        var oppgaveBehandlingKoblinger = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandling.getId());
        var aktivOppgave = OppgaveBehandlingKobling.getAktivOppgaveMedÅrsak(behandling.erRevurdering() ? OppgaveÅrsak.REVURDER : OppgaveÅrsak.BEHANDLE_SAK, oppgaveBehandlingKoblinger);
        if (!aktivOppgave.isPresent()) {
            var enkeltTask = opprettProsessTaskData(behandling, TaskType.forProsessTask(OpprettOppgaveForBehandlingTask.class));
            enkeltTask.setCallIdFraEksisterende();
            taskTjeneste.lagre(enkeltTask);
        }
    }

    private boolean erSendtTilbakeFraBeslutter(Collection<Totrinnsvurdering> åpneTotrinnVurderinger) {
        return åpneTotrinnVurderinger.stream().anyMatch(ap -> !ap.getVurderPåNyttÅrsaker().isEmpty());
    }

    private boolean harAksjonspunkt(List<Aksjonspunkt> åpneAksjonspunkt, AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return åpneAksjonspunkt.stream().anyMatch(apListe -> apListe.getAksjonspunktDefinisjon().equals(aksjonspunktDefinisjon));
    }

    private ProsessTaskData opprettProsessTaskData(Behandling behandling, TaskType prosesstaskType) {
        var prosessTaskData = ProsessTaskData.forTaskType(prosesstaskType);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        return prosessTaskData;
    }

    private List<Aksjonspunkt> filterAksjonspunkt(List<Aksjonspunkt> åpneAksjonspunkter, BehandlingskontrollEvent event) {
        var aksjonspunktForSteg = event.getBehandlingModell().finnAksjonspunktDefinisjoner(event.getStegType());
        return åpneAksjonspunkter.stream()
                .filter(ad -> aksjonspunktForSteg.contains(ad.getAksjonspunktDefinisjon()))
                .collect(Collectors.toList());
    }
}

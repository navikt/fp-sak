package no.nav.foreldrepenger.behandling.impl;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingskontrollEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.vedtak.felles.integrasjon.kafka.EventHendelse;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
public class BehandlingskontrollEventObserver {

    private static final Logger LOG = LoggerFactory.getLogger(BehandlingskontrollEventObserver.class);

    private ProsessTaskTjeneste taskTjeneste;
    private BehandlingRepository behandlingRepository;

    public BehandlingskontrollEventObserver() {
    }

    @Inject
    public BehandlingskontrollEventObserver(ProsessTaskTjeneste taskTjeneste, BehandlingRepository behandlingRepository) {
        this.taskTjeneste = taskTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    public void observerStoppetEvent(@Observes BehandlingskontrollEvent.StoppetEvent event) {
        try {
            var prosessTaskData = opprettProsessTask(event.getBehandlingId(), EventHendelse.BEHANDLINGSKONTROLL_EVENT);
            taskTjeneste.lagre(prosessTaskData);
        } catch (Exception ex) {
            LOG.warn("Publisering av StoppetEvent feilet", ex);
        }
    }

    // Lytter på AksjonspunkterFunnetEvent, filtrer ut når behandling er satt
    // manuelt på vent og legger melding på kafka
    public void observerAksjonspunkterFunnetEvent(@Observes AksjonspunktStatusEvent event) {
        if (event.getAksjonspunkter().stream().anyMatch(e -> e.erOpprettet() && AUTO_MANUELT_SATT_PÅ_VENT.equals(e.getAksjonspunktDefinisjon()))) {
            try {
                var prosessTaskData = opprettProsessTask(event.getBehandlingId(), EventHendelse.AKSJONSPUNKT_OPPRETTET);
                taskTjeneste.lagre(prosessTaskData);
            } catch (Exception ex) {
                LOG.warn("Publisering av AksjonspunkterFunnetEvent feilet", ex);
            }
        }
    }

    public void observerBehandlingAvsluttetEvent(@Observes BehandlingStatusEvent.BehandlingAvsluttetEvent event) {
        try {
            var prosessTaskData = opprettProsessTask(event.getBehandlingId(), EventHendelse.AKSJONSPUNKT_AVBRUTT);
            taskTjeneste.lagre(prosessTaskData);
        } catch (Exception ex) {
            LOG.warn("Publisering av BehandlingAvsluttetEvent feilet", ex);
        }
    }

    public void observerAksjonspunktHarEndretBehandlendeEnhetEvent(@Observes BehandlingEnhetEvent event) {
        try {
            var prosessTaskData = opprettProsessTask(event.getBehandlingId(), EventHendelse.AKSJONSPUNKT_HAR_ENDRET_BEHANDLENDE_ENHET);
            taskTjeneste.lagre(prosessTaskData);
        } catch (Exception ex) {
            LOG.warn("Publisering av AksjonspunktHarEndretBehandlendeEnhetEvent feilet", ex);
        }
    }

    private ProsessTaskData opprettProsessTask(Long behandlingId, EventHendelse eventHendelse) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var hendelseBehandling = EventHendelse.AKSJONSPUNKT_HAR_ENDRET_BEHANDLENDE_ENHET.equals(eventHendelse) ?
            HendelseForBehandling.ENHET : ( EventHendelse.BEHANDLINGSKONTROLL_EVENT.equals(eventHendelse) ? HendelseForBehandling.ANNET : HendelseForBehandling.AKSJONSPUNKT) ;
        var prosessTaskData = ProsessTaskData.forProsessTask(PubliserBehandlingHendelseTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setProperty(PubliserBehandlingHendelseTask.HENDELSE_TYPE, hendelseBehandling.name());
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskData.setPrioritet(90);
        return prosessTaskData;
    }

}

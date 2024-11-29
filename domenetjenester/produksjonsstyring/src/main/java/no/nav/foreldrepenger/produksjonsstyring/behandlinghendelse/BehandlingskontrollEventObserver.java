package no.nav.foreldrepenger.produksjonsstyring.behandlinghendelse;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingskontrollEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.events.BehandlingEnhetEvent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
public class BehandlingskontrollEventObserver {

    private static final Logger LOG = LoggerFactory.getLogger(BehandlingskontrollEventObserver.class);

    private ProsessTaskTjeneste taskTjeneste;

    public BehandlingskontrollEventObserver() {
    }

    @Inject
    public BehandlingskontrollEventObserver(ProsessTaskTjeneste taskTjeneste) {
        this.taskTjeneste = taskTjeneste;
    }

    public void observerStoppetEvent(@Observes BehandlingskontrollEvent.StoppetEvent event) {
        try {
            opprettProsessTask(event, HendelseForBehandling.ANNET);
        } catch (Exception ex) {
            LOG.warn("Publisering av StoppetEvent feilet", ex);
        }
    }

    // Lytter på AksjonspunkterFunnetEvent, filtrer ut når behandling er satt
    // manuelt på vent og legger melding på kafka
    public void observerAksjonspunkterFunnetEvent(@Observes AksjonspunktStatusEvent event) {
        if (event.getAksjonspunkter().stream().anyMatch(e -> e.erOpprettet() && AUTO_MANUELT_SATT_PÅ_VENT.equals(e.getAksjonspunktDefinisjon()))) {
            try {
                opprettProsessTask(event, HendelseForBehandling.AKSJONSPUNKT);
            } catch (Exception ex) {
                LOG.warn("Publisering av AksjonspunkterFunnetEvent feilet", ex);
            }
        }
    }

    public void observerBehandlingAvsluttetEvent(@Observes BehandlingStatusEvent.BehandlingOpprettetEvent event) {
        try {
            opprettProsessTask(event, HendelseForBehandling.OPPRETTET);
        } catch (Exception ex) {
            LOG.warn("Publisering av BehandlingAvsluttetEvent feilet", ex);
        }
    }

    public void observerBehandlingAvsluttetEvent(@Observes BehandlingStatusEvent.BehandlingAvsluttetEvent event) {
        try {
            opprettProsessTask(event, HendelseForBehandling.AVSLUTTET);
        } catch (Exception ex) {
            LOG.warn("Publisering av BehandlingAvsluttetEvent feilet", ex);
        }
    }

    public void observerAksjonspunktHarEndretBehandlendeEnhetEvent(@Observes BehandlingEnhetEvent event) {
        try {
            opprettProsessTask(event, HendelseForBehandling.ENHET);
        } catch (Exception ex) {
            LOG.warn("Publisering av AksjonspunktHarEndretBehandlendeEnhetEvent feilet", ex);
        }
    }

    private void opprettProsessTask(BehandlingEvent behandlingEvent, HendelseForBehandling hendelse) {
        opprettProsessTask(behandlingEvent.getSaksnummer(), behandlingEvent.getFagsakId(), behandlingEvent.getBehandlingId(), hendelse);
    }

    private void opprettProsessTask(Saksnummer saksnummer, Long fagsakId, Long behandlingsId, HendelseForBehandling hendelse) {
        var prosessTaskData = PubliserBehandlingHendelseTask.prosessTask(saksnummer, fagsakId, behandlingsId, hendelse);
        taskTjeneste.lagre(prosessTaskData);
    }
}

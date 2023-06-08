package no.nav.foreldrepenger.behandling.impl;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.FagsakStatusEvent;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;

import no.nav.foreldrepenger.domene.typer.AktørId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingskontrollEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingEvent;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
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

    public void observerFagsakAvsluttetEvent(@Observes FagsakStatusEvent event) {
        if (event.getBehandlingId() != null && FagsakStatus.AVSLUTTET.equals(event.getNyStatus())) {
            try {
                // Bruker AVSLUTTET her for behandling som allerede er avsluttet. Dette er for å trigge ny opphenting av sak i fpoversikt.
                // Det vil i realiteten være to AVSLUTTET behandling hendelser på siste behandling i avsluttet fagsak.
                opprettProsessTask(event.getFagsakId(), event.getBehandlingId(), event.getAktørId(), HendelseForBehandling.AVSLUTTET);
            } catch (Exception ex) {
                LOG.warn("Publisering av BehandlingAvsluttetEvent feilet ved fagsak avsluttet", ex);
            }
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
        opprettProsessTask(behandlingEvent.getFagsakId(), behandlingEvent.getBehandlingId(), behandlingEvent.getAktørId(), hendelse);
    }

    private void opprettProsessTask(Long fagsakId, Long behandlingsId, AktørId aktørId, HendelseForBehandling hendelse) {
        var prosessTaskData = ProsessTaskData.forProsessTask(PubliserBehandlingHendelseTask.class);
        prosessTaskData.setBehandling(fagsakId, behandlingsId, aktørId.getId());
        prosessTaskData.setProperty(PubliserBehandlingHendelseTask.HENDELSE_TYPE, hendelse.name());
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskData.setPrioritet(90);
        taskTjeneste.lagre(prosessTaskData);
    }
}

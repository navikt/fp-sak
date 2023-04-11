package no.nav.foreldrepenger.dokumentbestiller.observers;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.dokumentbestiller.autopunkt.SendBrevForAutopunkt;

/**
 * Observerer Aksjonspunkt og sender brev ved behov
 */
@ApplicationScoped
public class SendBrevForAutopunktEventObserver {

    BehandlingRepository behandlingRepository;
    SendBrevForAutopunkt sendBrevForAutopunkt;

    public SendBrevForAutopunktEventObserver() {
        //CDI
    }

    @Inject
    public SendBrevForAutopunktEventObserver(BehandlingRepository behandlingRepository,
                                             SendBrevForAutopunkt sendBrevForAutopunkt) {
        this.behandlingRepository = behandlingRepository;
        this.sendBrevForAutopunkt = sendBrevForAutopunkt;
    }

    public void sendBrevForAutopunkt(@Observes AksjonspunktStatusEvent event) {
        var kontekst = event.getKontekst();
        var aksjonspunkter = event.getAksjonspunkter();
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        finnAksjonspunkerMedDef(aksjonspunkter, AksjonspunktDefinisjon.VENT_PÅ_SØKNAD)
            .ifPresent(ap -> sendBrevForAutopunkt.sendBrevForSøknadIkkeMottatt(behandling));
        finnAksjonspunkerMedDef(aksjonspunkter, AksjonspunktDefinisjon.VENT_PGA_FOR_TIDLIG_SØKNAD)
            .ifPresent(ap -> sendBrevForAutopunkt.sendBrevForTidligSøknad(behandling));
        finnAksjonspunkerMedDef(aksjonspunkter, AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_REVURDERING)
            .ifPresent(ap -> sendBrevForAutopunkt.sendBrevForEtterkontroll(behandling));
    }

    private Optional<Aksjonspunkt> finnAksjonspunkerMedDef(List<Aksjonspunkt> aksjonspunkter, AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return aksjonspunkter
            .stream()
            .filter(Aksjonspunkt::erOpprettet)
            .filter(ap -> aksjonspunktDefinisjon.equals(ap.getAksjonspunktDefinisjon()))
            .findFirst();
    }

}

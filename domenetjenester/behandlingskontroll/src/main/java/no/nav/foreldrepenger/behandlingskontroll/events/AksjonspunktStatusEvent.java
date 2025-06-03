package no.nav.foreldrepenger.behandlingskontroll.events;

import java.util.Collections;
import java.util.List;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public class AksjonspunktStatusEvent implements BehandlingEvent {

    private final Long behandlingId;
    private final Saksnummer saksnummer;
    private final Long fagsakId;
    private final List<Aksjonspunkt> aksjonspunkter;

    public AksjonspunktStatusEvent(BehandlingskontrollKontekst kontekst, List<Aksjonspunkt> aksjonspunkter) {
        this(kontekst.getBehandlingId(), kontekst.getSaksnummer(), kontekst.getFagsakId(), aksjonspunkter);
    }

    public AksjonspunktStatusEvent(Behandling behandling, List<Aksjonspunkt> aksjonspunkter) {
        this(behandling.getId(), behandling.getSaksnummer(), behandling.getFagsakId(), aksjonspunkter);
    }

    public AksjonspunktStatusEvent(Long behandlingId, Saksnummer saksnummer, Long fagsakId, List<Aksjonspunkt> aksjonspunkter) {
        this.behandlingId = behandlingId;
        this.saksnummer = saksnummer;
        this.fagsakId = fagsakId;
        this.aksjonspunkter = Collections.unmodifiableList(aksjonspunkter);
    }

    @Override
    public Long getFagsakId() {
        return fagsakId;
    }

    @Override
    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    @Override
    public Long getBehandlingId() {
        return behandlingId;
    }

    public List<Aksjonspunkt> getAksjonspunkter() {
        return aksjonspunkter;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + aksjonspunkter + ", behandlingId=" + getBehandlingId() + ">";
    }

}

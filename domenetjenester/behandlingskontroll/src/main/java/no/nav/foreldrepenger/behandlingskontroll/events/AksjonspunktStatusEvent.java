package no.nav.foreldrepenger.behandlingskontroll.events;

import java.util.Collections;
import java.util.List;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public class AksjonspunktStatusEvent implements BehandlingEvent {

    private final Long behandlingId;
    private final Saksnummer saksnummer;
    private final Long fagsakId;
    private final BehandlingStegType behandlingStegType;
    private final List<Aksjonspunkt> aksjonspunkter;

    public AksjonspunktStatusEvent(BehandlingskontrollKontekst kontekst, List<Aksjonspunkt> aksjonspunkter,
            BehandlingStegType behandlingStegType) {
        this.behandlingId = kontekst.getBehandlingId();
        this.saksnummer = kontekst.getSaksnummer();
        this.fagsakId = kontekst.getFagsakId();
        this.behandlingStegType = behandlingStegType;
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

    public BehandlingStegType getBehandlingStegType() {
        return behandlingStegType;
    }

    public List<Aksjonspunkt> getAksjonspunkter() {
        return aksjonspunkter;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + aksjonspunkter + ", behandlingId="
                + getBehandlingId() + ", steg=" + getBehandlingStegType() + ">";
    }

}

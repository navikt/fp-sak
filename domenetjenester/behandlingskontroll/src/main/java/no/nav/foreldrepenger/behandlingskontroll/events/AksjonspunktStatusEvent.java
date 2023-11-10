package no.nav.foreldrepenger.behandlingskontroll.events;

import java.util.Collections;
import java.util.List;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class AksjonspunktStatusEvent implements BehandlingEvent {
    private final BehandlingskontrollKontekst kontekst;
    private final BehandlingStegType behandlingStegType;
    private List<Aksjonspunkt> aksjonspunkter;

    public AksjonspunktStatusEvent(BehandlingskontrollKontekst kontekst, List<Aksjonspunkt> aksjonspunkter,
            BehandlingStegType behandlingStegType) {
        super();
        this.kontekst = kontekst;
        this.behandlingStegType = behandlingStegType;
        this.aksjonspunkter = Collections.unmodifiableList(aksjonspunkter);
    }

    @Override
    public Long getFagsakId() {
        return kontekst.getFagsakId();
    }

    @Override
    public AktørId getAktørId() {
        return kontekst.getAktørId();
    }

    @Override
    public Long getBehandlingId() {
        return kontekst.getBehandlingId();
    }

    public BehandlingskontrollKontekst getKontekst() {
        return kontekst;
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
                + getKontekst().getBehandlingId() + ", steg=" + getBehandlingStegType() + ">";
    }

}

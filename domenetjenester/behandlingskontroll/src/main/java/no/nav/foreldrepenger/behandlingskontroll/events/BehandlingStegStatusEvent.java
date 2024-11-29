package no.nav.foreldrepenger.behandlingskontroll.events;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

/**
 * Event publiseres av {@link BehandlingskontrollTjeneste} når en
 * {@link Behandling} endrer steg. Kan brukes til å lytte på flyt i en
 * Behandling og utføre logikk når det skjer.
 */
public class BehandlingStegStatusEvent implements BehandlingEvent {

    private BehandlingskontrollKontekst kontekst;

    private BehandlingStegStatus nyStatus;

    private BehandlingStegType stegType;

    private BehandlingStegStatus forrigeStatus;

    public BehandlingStegStatusEvent(BehandlingskontrollKontekst kontekst, BehandlingStegType stegType, BehandlingStegStatus forrigeStatus,
            BehandlingStegStatus nyStatus) {
        super();
        this.kontekst = kontekst;
        this.stegType = stegType;
        this.forrigeStatus = forrigeStatus;
        this.nyStatus = nyStatus;
    }

    @Override
    public Saksnummer getSaksnummer() {
        return kontekst.getSaksnummer();
    }

    @Override
    public Long getFagsakId() {
        return kontekst.getFagsakId();
    }

    @Override
    public Long getBehandlingId() {
        return kontekst.getBehandlingId();
    }

    public BehandlingStegType getStegType() {
        return stegType;
    }

    public BehandlingStegStatus getForrigeStatus() {
        return forrigeStatus;
    }

    public BehandlingStegStatus getNyStatus() {
        return nyStatus;
    }

    public BehandlingskontrollKontekst getKontekst() {
        return kontekst;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + kontekst +
                ", steg=" + stegType +
                ", forrigeStatus=" + forrigeStatus +
                ", nyStatus=" + nyStatus +
                ">";
    }
}

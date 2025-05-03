package no.nav.foreldrepenger.behandlingskontroll.events;

import java.util.Objects;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

/**
 * Event publiseres av {@link BehandlingskontrollTjeneste} når en
 * {@link Behandling} endrer steg. Kan brukes til å lytte på flyt i en
 * Behandling og utføre logikk når det skjer.
 */
public class BehandlingStatusEvent implements BehandlingEvent {

    private final Long behandlingId;
    private final Saksnummer saksnummer;
    private final Long fagsakId;
    private final BehandlingStatus nyStatus;

    BehandlingStatusEvent(BehandlingskontrollKontekst kontekst, BehandlingStatus nyStatus) {
        this.behandlingId = kontekst.getBehandlingId();
        this.saksnummer = kontekst.getSaksnummer();
        this.fagsakId = kontekst.getFagsakId();
        this.nyStatus = nyStatus;
    }

    @Override
    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    @Override
    public Long getBehandlingId() {
        return behandlingId;
    }

    @Override
    public Long getFagsakId() {
        return fagsakId;
    }

    public BehandlingStatus getNyStatus() {
        return nyStatus;
    }

    static void validerRiktigStatus(BehandlingStatus nyStatus, BehandlingStatus expected) {
        if (!Objects.equals(expected, nyStatus)) {
            throw new IllegalArgumentException("Kan bare være " + expected + ", fikk: " + nyStatus);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<behandlingId=" + getBehandlingId() +
                ", nyStatus=" + nyStatus +
                ">";
    }

    public static class BehandlingAvsluttetEvent extends BehandlingStatusEvent {
        BehandlingAvsluttetEvent(BehandlingskontrollKontekst kontekst, BehandlingStatus nyStatus) {
            super(kontekst, nyStatus);
            validerRiktigStatus(nyStatus, BehandlingStatus.AVSLUTTET);
        }
    }

    public static class BehandlingOpprettetEvent extends BehandlingStatusEvent {
        BehandlingOpprettetEvent(BehandlingskontrollKontekst kontekst, BehandlingStatus nyStatus) {
            super(kontekst, nyStatus);
            validerRiktigStatus(nyStatus, BehandlingStatus.OPPRETTET);
        }
    }

    @SuppressWarnings("unchecked")
    public static <V extends BehandlingStatusEvent> V nyEvent(BehandlingskontrollKontekst kontekst, BehandlingStatus nyStatus) {
        if (BehandlingStatus.AVSLUTTET.equals(nyStatus)) {
            return (V) new BehandlingAvsluttetEvent(kontekst, nyStatus);
        }
        if (BehandlingStatus.OPPRETTET.equals(nyStatus)) {
            return (V) new BehandlingOpprettetEvent(kontekst, nyStatus);
        }
        return (V) new BehandlingStatusEvent(kontekst, nyStatus);
    }
}

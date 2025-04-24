package no.nav.foreldrepenger.behandlingskontroll.events;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegTilstandSnapshot;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

/**
 * Event som fyres når Behandlingskontroll STARTER å prosessere en behandling,
 * STOPPER (eks. fordi den er avsluttet, eller stopper i et vurderingspunkt).
 * Eventuelt også dersom en Exception mottas.
 */
public abstract class BehandlingskontrollEvent implements BehandlingEvent {

    private final BehandlingskontrollKontekst kontekst;
    private final BehandlingStegTilstandSnapshot snapshot;

    protected BehandlingskontrollEvent(BehandlingskontrollKontekst kontekst, BehandlingStegTilstandSnapshot snapshot) {
        this.kontekst = kontekst;
        this.snapshot = snapshot;
    }

    @Override
    public Long getFagsakId() {
        return kontekst.getFagsakId();
    }

    @Override
    public Saksnummer getSaksnummer() {
        return kontekst.getSaksnummer();
    }

    @Override
    public Long getBehandlingId() {
        return kontekst.getBehandlingId();
    }

    public BehandlingStegType getStegType() {
        return Optional.ofNullable(snapshot).map(BehandlingStegTilstandSnapshot::steg).orElse(null);
    }

    public BehandlingStegStatus getStegStatus() {
        return Optional.ofNullable(snapshot).map(BehandlingStegTilstandSnapshot::status).orElse(null);
    }

    /**
     * Fyres når
     * {@link BehandlingskontrollTjeneste#prosesserBehandling(BehandlingskontrollKontekst)}
     * starter å kjøre.
     */
    public static class StartetEvent extends BehandlingskontrollEvent {

        public StartetEvent(BehandlingskontrollKontekst kontekst, BehandlingStegTilstandSnapshot snapshot) {
            super(kontekst, snapshot);
        }

    }

    /**
     * Fyres når
     * {@link BehandlingskontrollTjeneste#prosesserBehandling(BehandlingskontrollKontekst)}
     * stopper. Stoppet fordi aksjonspunkter er funnet..
     */
    public static class StoppetEvent extends BehandlingskontrollEvent {

        public StoppetEvent(BehandlingskontrollKontekst kontekst, BehandlingStegTilstandSnapshot snapshot) {
            super(kontekst, snapshot);
        }

    }

    /**
     * Fyres når
     * {@link BehandlingskontrollTjeneste#prosesserBehandling(BehandlingskontrollKontekst)}
     * stopper. Stoppet fordi prosessen er avsluttet.
     *
     * @see StoppetEvent
     */
    public static class AvsluttetEvent extends BehandlingskontrollEvent {

        public AvsluttetEvent(BehandlingskontrollKontekst kontekst, BehandlingStegTilstandSnapshot snapshot) {
            super(kontekst, snapshot);
        }

    }

    /**
     * Fyres når
     * {@link BehandlingskontrollTjeneste#prosesserBehandling(BehandlingskontrollKontekst)}
     * får en Exception
     */
    public static class ExceptionEvent extends BehandlingskontrollEvent {

        private final RuntimeException exception;

        public ExceptionEvent(BehandlingskontrollKontekst kontekst, BehandlingStegTilstandSnapshot snapshot, RuntimeException exception) {
            super(kontekst, snapshot);
            this.exception = exception;
        }

        public RuntimeException getException() {
            return exception;
        }

    }

}

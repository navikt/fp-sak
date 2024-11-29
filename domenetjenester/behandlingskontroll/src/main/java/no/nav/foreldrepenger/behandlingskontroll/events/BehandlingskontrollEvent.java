package no.nav.foreldrepenger.behandlingskontroll.events;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingModell;
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

    private BehandlingModell behandlingModell;
    private BehandlingStegType stegType;
    private BehandlingStegStatus stegStatus;
    private BehandlingskontrollKontekst kontekst;

    public BehandlingskontrollEvent(BehandlingskontrollKontekst kontekst, BehandlingModell behandlingModell, BehandlingStegType stegType,
            BehandlingStegStatus stegStatus) {
        this.kontekst = kontekst;
        this.behandlingModell = behandlingModell;
        this.stegType = stegType;
        this.stegStatus = stegStatus;

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

    public BehandlingModell getBehandlingModell() {
        return behandlingModell;
    }

    public BehandlingStegType getStegType() {
        return stegType;
    }

    public BehandlingStegStatus getStegStatus() {
        return stegStatus;
    }

    /**
     * Fyres når
     * {@link BehandlingskontrollTjeneste#prosesserBehandling(BehandlingskontrollKontekst)}
     * starter å kjøre.
     */
    public static class StartetEvent extends BehandlingskontrollEvent {

        public StartetEvent(BehandlingskontrollKontekst kontekst, BehandlingModell behandlingModell,
                BehandlingStegType stegType, BehandlingStegStatus stegStatus) {
            super(kontekst, behandlingModell, stegType, stegStatus);
        }

    }

    /**
     * Fyres når
     * {@link BehandlingskontrollTjeneste#prosesserBehandling(BehandlingskontrollKontekst)}
     * stopper. Stoppet fordi aksjonspunkter er funnet..
     */
    public static class StoppetEvent extends BehandlingskontrollEvent {

        public StoppetEvent(BehandlingskontrollKontekst kontekst, BehandlingModell behandlingModell,
                BehandlingStegType stegType, BehandlingStegStatus stegStatus) {
            super(kontekst, behandlingModell, stegType, stegStatus);
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

        public AvsluttetEvent(BehandlingskontrollKontekst kontekst, BehandlingModell behandlingModell, BehandlingStegType stegType,
                BehandlingStegStatus stegStatus) {
            super(kontekst, behandlingModell, stegType, stegStatus);
        }

    }

    /**
     * Fyres når
     * {@link BehandlingskontrollTjeneste#prosesserBehandling(BehandlingskontrollKontekst)}
     * får en Exception
     */
    public static class ExceptionEvent extends BehandlingskontrollEvent {

        private RuntimeException exception;

        public ExceptionEvent(BehandlingskontrollKontekst kontekst, BehandlingModell behandlingModell, BehandlingStegType stegType,
                BehandlingStegStatus stegStatus, RuntimeException exception) {
            super(kontekst, behandlingModell, stegType, stegStatus);
            this.exception = exception;
        }

        public RuntimeException getException() {
            return exception;
        }

    }

}

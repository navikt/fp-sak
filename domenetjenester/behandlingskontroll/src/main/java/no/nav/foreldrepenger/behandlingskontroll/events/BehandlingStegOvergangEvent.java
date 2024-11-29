package no.nav.foreldrepenger.behandlingskontroll.events;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegTilstandSnapshot;
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
public class BehandlingStegOvergangEvent implements BehandlingEvent {

    private BehandlingskontrollKontekst kontekst;
    private BehandlingStegTilstandSnapshot fraTilstand;
    private BehandlingStegTilstandSnapshot tilTilstand;

    public BehandlingStegOvergangEvent(BehandlingskontrollKontekst kontekst, BehandlingStegTilstandSnapshot forrigeTilstand,
            BehandlingStegTilstandSnapshot nyTilstand) {
        super();
        this.kontekst = kontekst;
        this.fraTilstand = forrigeTilstand;
        this.tilTilstand = nyTilstand;
    }

    public static BehandlingStegOvergangEvent nyEvent(BehandlingskontrollKontekst kontekst,
            BehandlingStegTilstandSnapshot forrigeTilstand, BehandlingStegTilstandSnapshot nyTilstand, int relativForflytning) {
        if (relativForflytning == 1) {
            // normal forover
            return new BehandlingStegOvergangEvent(kontekst, forrigeTilstand, nyTilstand);
        }
        if (relativForflytning < 1) {
            // tilbakeføring
            return new BehandlingStegTilbakeføringEvent(kontekst, forrigeTilstand, nyTilstand);
        }
        // > 1
        // framføring
        return new BehandlingStegOvergangEvent.BehandlingStegOverhoppEvent(kontekst, forrigeTilstand, nyTilstand);
    }

    public BehandlingskontrollKontekst getKontekst() {
        return kontekst;
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

    public Optional<BehandlingStegTilstandSnapshot> getFraTilstand() {
        return Optional.ofNullable(fraTilstand);
    }

    public Optional<BehandlingStegTilstandSnapshot> getTilTilstand() {
        return Optional.ofNullable(tilTilstand);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + kontekst +
                ", fraTilstand=" + fraTilstand +
                ", tilTilstand=" + tilTilstand +
                ">";
    }

    public BehandlingStegType getTilStegType() {
        return getTilTilstand().map(BehandlingStegTilstandSnapshot::getSteg).orElse(null);
    }

    public BehandlingStegType getFraStegType() {
        return getFraTilstand().map(BehandlingStegTilstandSnapshot::getSteg).orElse(null);
    }

    public BehandlingStegType getFørsteSteg() {
        // siden hopper framover blir dette fraSteg
        return getFraStegType();
    }

    public BehandlingStegType getSisteSteg() {
        // siden hopper framover blir dette tilSteg
        return getTilStegType();
    }

    public Optional<BehandlingStegStatus> getFørsteStegStatus() {
        return getFraTilstand().map(BehandlingStegTilstandSnapshot::getStatus);
    }

    public Optional<BehandlingStegStatus> getSisteStegStatus() {
        return getTilTilstand().map(BehandlingStegTilstandSnapshot::getStatus);
    }

    /**
     * Event som fyres dersom vi tilbakefører (går bakover i behandlingstegene)
     */
    public static class BehandlingStegTilbakeføringEvent extends BehandlingStegOvergangEvent {

        public BehandlingStegTilbakeføringEvent(BehandlingskontrollKontekst kontekst, BehandlingStegTilstandSnapshot forrigeTilstand,
                BehandlingStegTilstandSnapshot nyTilstand) {
            super(kontekst, forrigeTilstand, nyTilstand);

        }

        @Override
        public BehandlingStegType getFørsteSteg() {
            // siden hopper bakover blir dette tilSteg
            return getTilStegType();
        }

        @Override
        public BehandlingStegType getSisteSteg() {
            // siden hopper bakover blir dette fraSteg
            return getFraStegType();
        }

        @Override
        public Optional<BehandlingStegStatus> getSisteStegStatus() {
            // siden hopper bakover blir dette fraSteg
            return getFraTilstand().map(BehandlingStegTilstandSnapshot::getStatus);
        }

        @Override
        public Optional<BehandlingStegStatus> getFørsteStegStatus() {
            // siden hopper bakover blir dette tilSteg
            return getTilTilstand().map(BehandlingStegTilstandSnapshot::getStatus);
        }
    }

    /**
     * Event som fyres dersom vi gjør overhopp (hopper framover i stegene)
     */
    public static class BehandlingStegOverhoppEvent extends BehandlingStegOvergangEvent {

        public BehandlingStegOverhoppEvent(BehandlingskontrollKontekst kontekst, BehandlingStegTilstandSnapshot forrigeTilstand,
                BehandlingStegTilstandSnapshot nyTilstand) {
            super(kontekst, forrigeTilstand, nyTilstand);
        }
    }
}

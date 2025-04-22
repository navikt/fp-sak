package no.nav.foreldrepenger.behandlingskontroll.events;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegTilstandSnapshot;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.StegTransisjon;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.Transisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public class BehandlingTransisjonEvent implements BehandlingEvent {

    private final BehandlingskontrollKontekst kontekst;
    private final Transisjon transisjon;
    private final BehandlingStegTilstandSnapshot fraTilstand;

    public BehandlingTransisjonEvent(BehandlingskontrollKontekst kontekst, Transisjon transisjon, BehandlingStegTilstandSnapshot fraTilstand) {
        this.kontekst = kontekst;
        this.transisjon = transisjon;
        this.fraTilstand = fraTilstand;
    }

    @Override
    public Long getBehandlingId() {
        return kontekst.getBehandlingId();
    }

    @Override
    public Long getFagsakId() {
        return kontekst.getFagsakId();
    }

    @Override
    public Saksnummer getSaksnummer() {
        return kontekst.getSaksnummer();
    }

    public BehandlingskontrollKontekst getKontekst() {
        return kontekst;
    }

    public StegTransisjon getStegTransisjon() {
        return transisjon.stegTransisjon();
    }

    public Optional<BehandlingStegStatus> getFørsteStegStatus() {
        return Optional.ofNullable(fraTilstand).map(BehandlingStegTilstandSnapshot::status);
    }

    public BehandlingStegType getFørsteSteg() {
        // siden hopper framover blir dette fraSteg
        return Optional.ofNullable(fraTilstand).map(BehandlingStegTilstandSnapshot::steg).orElse(null);
    }

    public BehandlingStegType getSisteSteg() {
        // siden hopper framover blir dette tilSteg
        return transisjon.målSteg();
    }

}

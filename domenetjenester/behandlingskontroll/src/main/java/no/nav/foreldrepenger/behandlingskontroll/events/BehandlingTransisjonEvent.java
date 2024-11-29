package no.nav.foreldrepenger.behandlingskontroll.events;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.TransisjonIdentifikator;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegTilstand;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public class BehandlingTransisjonEvent implements BehandlingEvent {

    private final BehandlingskontrollKontekst kontekst;
    private TransisjonIdentifikator transisjonIdentifikator;
    private BehandlingStegTilstand fraTilstand;
    private BehandlingStegType tilStegType;
    private boolean erOverhopp;

    public BehandlingTransisjonEvent(BehandlingskontrollKontekst kontekst, TransisjonIdentifikator transisjonIdentifikator,
            BehandlingStegTilstand fraTilstand, BehandlingStegType tilStegType, boolean erOverhopp) {
        this.kontekst = kontekst;
        this.transisjonIdentifikator = transisjonIdentifikator;
        this.fraTilstand = fraTilstand;
        this.tilStegType = tilStegType;
        this.erOverhopp = erOverhopp;
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

    public TransisjonIdentifikator getTransisjonIdentifikator() {
        return transisjonIdentifikator;
    }

    public Optional<BehandlingStegStatus> getFørsteStegStatus() {
        return Optional.ofNullable(fraTilstand).map(BehandlingStegTilstand::getBehandlingStegStatus);
    }

    public BehandlingStegType getFørsteSteg() {
        // siden hopper framover blir dette fraSteg
        return fraTilstand != null ? fraTilstand.getBehandlingSteg() : null;
    }

    public BehandlingStegType getSisteSteg() {
        // siden hopper framover blir dette tilSteg
        return tilStegType;
    }

    public boolean erOverhopp() {
        return erOverhopp;
    }
}

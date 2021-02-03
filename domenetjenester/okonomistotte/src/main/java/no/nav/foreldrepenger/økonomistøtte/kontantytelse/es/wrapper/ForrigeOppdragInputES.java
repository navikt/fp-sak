package no.nav.foreldrepenger.økonomistøtte.kontantytelse.es.wrapper;

import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;

public class ForrigeOppdragInputES {

    private final Oppdrag110 forrigeOppdragForSak;
    private final BehandlingVedtak tidligereVedtak;
    private final long sats;

    public ForrigeOppdragInputES(Oppdrag110 forrigeOppdragForSak, BehandlingVedtak tidligereVedtak, long sats) {
        this.forrigeOppdragForSak = forrigeOppdragForSak;
        this.tidligereVedtak = tidligereVedtak;
        this.sats = sats;
    }

    public Oppdrag110 getForrigeOppddragForSak() {
        return forrigeOppdragForSak;
    }

    public BehandlingVedtak getTidligereVedtak() {
        return tidligereVedtak;
    }

    public long getSats() {
        return sats;
    }
}

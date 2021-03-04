package no.nav.foreldrepenger.økonomistøtte.kontantytelse.es.wrapper;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;

public class ForrigeOppdragInputES {

    private final Oppdrag110 forrigeOppdragForSak;
    private final long sats;

    public ForrigeOppdragInputES(Oppdrag110 forrigeOppdragForSak, long sats) {
        this.forrigeOppdragForSak = forrigeOppdragForSak;
        this.sats = sats;
    }

    public Oppdrag110 getForrigeOppddragForSak() {
        return forrigeOppdragForSak;
    }

    public long getSats() {
        return sats;
    }
}

package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

public class KontoUtvidelser {

    private final int prematurdager;
    private final int flerbarnsdager;

    public KontoUtvidelser(int prematurdager, int flerbarnsdager) {
        this.prematurdager = prematurdager;
        this.flerbarnsdager = flerbarnsdager;
    }

    public int getPrematurdager() {
        return prematurdager;
    }

    public int getFlerbarnsdager() {
        return flerbarnsdager;
    }
}

package no.nav.foreldrepenger.behandling;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;

public class FagsakRelasjonEvent {

    private FagsakRelasjon fagsakRelasjon;

    public FagsakRelasjonEvent(FagsakRelasjon fagsakRelasjon) {
        this.fagsakRelasjon = fagsakRelasjon;
    }

    public FagsakRelasjon getFagsakRelasjon() {
        return fagsakRelasjon;
    }
}

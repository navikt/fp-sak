package no.nav.foreldrepenger.behandlingskontroll.impl;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;

class DummyVenterSteg extends DummySteg {

    private int andreGang = 0;

    public DummyVenterSteg() {
        super();
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        BehandleStegResultat settPåVent = andreGang == 1 ? BehandleStegResultat.settPåVent() : BehandleStegResultat.startet();
        if (andreGang == 0) andreGang = 1;
        sisteUtførStegResultat.set(settPåVent);
        return settPåVent;
    }

    @Override
    public BehandleStegResultat gjenopptaSteg(BehandlingskontrollKontekst kontekst) {
        BehandleStegResultat resultat = BehandleStegResultat.utførtUtenAksjonspunkter();
        sisteUtførStegResultat.set(resultat);
        return resultat;
    }

}

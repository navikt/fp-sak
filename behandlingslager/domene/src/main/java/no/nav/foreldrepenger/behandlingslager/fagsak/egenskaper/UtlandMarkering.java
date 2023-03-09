package no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper;

import no.nav.foreldrepenger.behandlingslager.fagsak.EgenskapNøkkel;
import no.nav.foreldrepenger.behandlingslager.fagsak.EgenskapVerdi;

public enum UtlandMarkering implements EgenskapVerdi {

    NASJONAL,
    EØS_BOSATT_NORGE,
    BOSATT_UTLAND;

    public static final EgenskapNøkkel NØKKEL =  EgenskapNøkkel.UTLAND_MARKERING;

    @Override
    public EgenskapNøkkel getNøkkel() {
        return NØKKEL;
    }

}

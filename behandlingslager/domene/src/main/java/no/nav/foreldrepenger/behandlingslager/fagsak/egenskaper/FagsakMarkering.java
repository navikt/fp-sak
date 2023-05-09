package no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper;

import no.nav.foreldrepenger.behandlingslager.fagsak.EgenskapNøkkel;
import no.nav.foreldrepenger.behandlingslager.fagsak.EgenskapVerdi;

public enum FagsakMarkering implements EgenskapVerdi {

    NASJONAL,
    EØS_BOSATT_NORGE,
    BOSATT_UTLAND,
    SAMMENSATT_KONTROLL,
    DØD_DØDFØDSEL,
    SELVSTENDIG_NÆRING;

    @Override
    public String getVerdi() {
        return name();
    }

    @Override
    public EgenskapNøkkel getNøkkel() {
        return EgenskapNøkkel.FAGSAK_MARKERING;
    }

}

package no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper;

import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.fagsak.EgenskapNøkkel;
import no.nav.foreldrepenger.behandlingslager.fagsak.EgenskapVerdi;

public enum FagsakMarkering implements EgenskapVerdi {

    NASJONAL,
    EØS_BOSATT_NORGE,
    BOSATT_UTLAND,
    SAMMENSATT_KONTROLL,
    DØD_DØDFØDSEL,
    PRAKSIS_UTSETTELSE,
    SELVSTENDIG_NÆRING;

    @Override
    public String getVerdi() {
        return name();
    }

    @Override
    public EgenskapNøkkel getNøkkel() {
        return EgenskapNøkkel.FAGSAK_MARKERING;
    }

    private static final Set<FagsakMarkering> PRIORITERT = Set.of(FagsakMarkering.BOSATT_UTLAND, FagsakMarkering.SAMMENSATT_KONTROLL,
        FagsakMarkering.DØD_DØDFØDSEL);

    public static boolean erPrioritert(FagsakMarkering fagsakMarkering) {
        return fagsakMarkering != null && PRIORITERT.contains(fagsakMarkering);
    }

}

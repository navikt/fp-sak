package no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper;

import no.nav.foreldrepenger.behandlingslager.fagsak.EgenskapNøkkel;
import no.nav.foreldrepenger.behandlingslager.fagsak.EgenskapVerdi;

public enum UtlandDokumentasjonStatus implements EgenskapVerdi {

    DOKUMENTASJON_ER_INNHENTET,
    DOKUMENTASJON_VIL_BLI_INNHENTET,
    DOKUMENTASJON_VIL_IKKE_BLI_INNHENTET;

    public static final EgenskapNøkkel NØKKEL =  EgenskapNøkkel.UTLAND_DOKUMENTASJON;

    @Override
    public EgenskapNøkkel getNøkkel() {
        return NØKKEL;
    }

}

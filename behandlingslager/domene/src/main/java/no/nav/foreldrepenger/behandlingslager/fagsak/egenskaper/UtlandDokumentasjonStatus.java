package no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper;

import no.nav.foreldrepenger.behandlingslager.fagsak.EgenskapNøkkel;
import no.nav.foreldrepenger.behandlingslager.fagsak.EgenskapVerdi;

public enum UtlandDokumentasjonStatus implements EgenskapVerdi {

    DOKUMENTASJON_ER_INNHENTET,
    DOKUMENTASJON_VIL_BLI_INNHENTET,
    DOKUMENTASJON_VIL_IKKE_BLI_INNHENTET;

    @Override
    public String getVerdi() {
        return name();
    }

    @Override
    public EgenskapNøkkel getNøkkel() {
        return EgenskapNøkkel.UTLAND_DOKUMENTASJON;
    }

}

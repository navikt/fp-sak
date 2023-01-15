package no.nav.foreldrepenger.behandling.aksjonspunkt;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;

public interface OverstyringAksjonspunkt {
    String getAvslagskode();

    boolean getErVilkarOk();

    String getBegrunnelse();

    // TODO: hva skal til for å kunne bruke FAKTA_ENDRET
    default HistorikkinnslagType historikkmalForOverstyring() {
        return HistorikkinnslagType.OVERSTYRT;
    }
}

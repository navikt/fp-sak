package no.nav.foreldrepenger.behandling.aksjonspunkt;

public interface OverstyringAksjonspunkt {
    String getAvslagskode();

    boolean getErVilkarOk();

    String getBegrunnelse();
}

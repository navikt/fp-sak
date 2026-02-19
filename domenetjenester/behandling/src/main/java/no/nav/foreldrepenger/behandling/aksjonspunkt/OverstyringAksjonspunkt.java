package no.nav.foreldrepenger.behandling.aksjonspunkt;

public interface OverstyringAksjonspunkt {
    String getAvslagskode();

    boolean getErVilkårOk();

    String getBegrunnelse();
}

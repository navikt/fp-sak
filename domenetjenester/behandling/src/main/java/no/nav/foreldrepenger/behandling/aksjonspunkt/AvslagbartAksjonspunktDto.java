package no.nav.foreldrepenger.behandling.aksjonspunkt;

/**
 * Aksjonspunkt Dto som lar seg avslå (der vilkår kan settes Ok/Ikke OK)
 */
public interface AvslagbartAksjonspunktDto extends AksjonspunktKode {

    Boolean getErVilkarOk();

    String getAvslagskode();

    String getBegrunnelse();
}

package no.nav.foreldrepenger.behandling.aksjonspunkt;

/** Interface for å oppdatere aksjonspunkter. */
public interface AksjonspunktOppdaterer<T> {

    OppdateringResultat oppdater(T dto, AksjonspunktOppdaterParameter param);

}

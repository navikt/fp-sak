package no.nav.foreldrepenger.behandlingskontroll;

import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;

public interface AksjonspunktkontrollTjeneste {

    /**
     * Oppretter og håndterer nye aksjonspunkt
     */
    List<Aksjonspunkt> lagreAksjonspunkterFunnet(Behandling behandling, BehandlingLås skriveLås, BehandlingStegType behandlingStegType,
                                                 List<AksjonspunktDefinisjon> aksjonspunkter);

    /**
     * Oppretter og håndterer nye overstyringsaksjonspunkt
     */
    List<Aksjonspunkt> lagreAksjonspunkterFunnet(Behandling behandling, BehandlingLås skriveLås, List<AksjonspunktDefinisjon> aksjonspunkter);

    /**
     * Lagrer og håndterer utførte aksjonspunkt uten begrunnelse. Dersom man skal
     * lagre begrunnelse - bruk apRepository + aksjonspunkterUtført
     */
    void lagreAksjonspunkterUtført(Behandling behandling, BehandlingLås skriveLås, Aksjonspunkt aksjonspunkt, String begrunnelse);

    /**
     * Lagrer og håndterer avbrutte aksjonspunkt
     */
    void lagreAksjonspunkterAvbrutt(Behandling behandling, BehandlingLås skriveLås, List<Aksjonspunkt> aksjonspunkter);

    /**
     * Lagrer og håndterer reåpning av aksjonspunkt
     */
    void lagreAksjonspunkterReåpnet(Behandling behandling, BehandlingLås skriveLås, List<Aksjonspunkt> aksjonspunkter);

    /**
     * Lagrer og håndterer aksjonspunktresultater fra utledning utenom steg
     */
    void lagreAksjonspunktResultat(Behandling behandling, BehandlingLås skriveLås, BehandlingStegType behandlingStegType,
            List<AksjonspunktResultat> aksjonspunktResultater);

    /**
     * Lagrer og publiserer totrinns-setting for aksjonspunkt
     */
    void setAksjonspunkterToTrinn(Behandling behandling, BehandlingLås skriveLås, List<Aksjonspunkt> aksjonspunkter, boolean totrinn);

}

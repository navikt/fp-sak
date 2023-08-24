package no.nav.foreldrepenger.domene.registerinnhenting;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;

import java.util.List;

public interface KontrollerFaktaInngangsVilkårUtleder {

    List<AksjonspunktResultat> utledAksjonspunkter(BehandlingReferanse ref);

    List<AksjonspunktResultat> utledAksjonspunkterTilHøyreForStartpunkt(BehandlingReferanse ref, StartpunktType startpunktType);

    List<AksjonspunktResultat> utledAksjonspunkterFomSteg(BehandlingReferanse ref, BehandlingStegType steg);

    boolean skalOverstyringLøsesTilHøyreForStartpunkt(BehandlingReferanse ref, StartpunktType startpunktType, AksjonspunktDefinisjon aksjonspunktDefinisjon);

}

package no.nav.foreldrepenger.domene.registerinnhenting;

import java.util.List;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;

public interface KontrollerFaktaInngangsVilkårUtleder {

    List<AksjonspunktResultat> utledAksjonspunkter(BehandlingReferanse ref, Skjæringstidspunkt stp);

    List<AksjonspunktResultat> utledAksjonspunkterTilHøyreForStartpunkt(BehandlingReferanse ref, Skjæringstidspunkt stp, StartpunktType startpunktType);

    List<AksjonspunktResultat> utledAksjonspunkterFomSteg(BehandlingReferanse ref, Skjæringstidspunkt stp, BehandlingStegType steg);

    boolean skalOverstyringLøsesTilHøyreForStartpunkt(BehandlingReferanse ref, StartpunktType startpunktType, AksjonspunktDefinisjon aksjonspunktDefinisjon);

}

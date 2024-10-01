package no.nav.foreldrepenger.inngangsvilkaar.svp;

import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;

@ApplicationScoped
public class AksjonspunktutlederForSvangerskapspengerTilrettelegging  implements AksjonspunktUtleder {
    @Override
    public List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {
        return Collections.singletonList(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.VURDER_SVP_TILRETTELEGGING));
    }
}

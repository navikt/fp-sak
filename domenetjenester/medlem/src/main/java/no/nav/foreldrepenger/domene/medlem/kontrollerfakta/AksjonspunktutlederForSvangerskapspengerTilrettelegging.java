package no.nav.foreldrepenger.domene.medlem.kontrollerfakta;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;

import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class AksjonspunktutlederForSvangerskapspengerTilrettelegging  implements AksjonspunktUtleder {
    @Override
    public List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {
        return Collections.singletonList(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.VURDER_SVP_TILRETTELEGGING));
    }
}

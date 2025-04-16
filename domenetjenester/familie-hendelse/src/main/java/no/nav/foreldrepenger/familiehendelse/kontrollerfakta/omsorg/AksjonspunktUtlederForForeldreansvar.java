package no.nav.foreldrepenger.familiehendelse.kontrollerfakta.omsorg;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;



@ApplicationScoped
public class AksjonspunktUtlederForForeldreansvar implements AksjonspunktUtleder {

    AksjonspunktUtlederForForeldreansvar() {
    }

    @Override
    public List<AksjonspunktUtlederResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {
        return AksjonspunktUtlederResultat.opprettListeForAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_VILKÅR_FOR_FORELDREANSVAR);
    }

}

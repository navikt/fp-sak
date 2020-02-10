package no.nav.foreldrepenger.behandling.steg.avklarfakta;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtleder;

@ApplicationScoped
public class KontrollerArbeidsforholdUtledere {

    private AksjonspunktUtlederForVurderArbeidsforhold utleder;
    
    KontrollerArbeidsforholdUtledere() {
        // CDI
    }

    @Inject
    KontrollerArbeidsforholdUtledere(AksjonspunktUtlederForVurderArbeidsforhold utleder){
        this.utleder = utleder;
    }
    
    public List<AksjonspunktUtleder> utledUtledereFor() {
        return List.of(utleder);
    }
}

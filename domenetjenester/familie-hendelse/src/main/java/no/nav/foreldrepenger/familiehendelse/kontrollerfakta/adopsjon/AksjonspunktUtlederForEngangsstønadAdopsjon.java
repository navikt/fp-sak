package no.nav.foreldrepenger.familiehendelse.kontrollerfakta.adopsjon;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;

@ApplicationScoped
public class AksjonspunktUtlederForEngangsstønadAdopsjon implements AksjonspunktUtleder {

    @Override
    public List<AksjonspunktUtlederResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {
        List<AksjonspunktUtlederResultat> aksjonspunktResultater = new ArrayList<>();

        // felles for MOR og FAR
        aksjonspunktResultater.add(AksjonspunktUtlederResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_ADOPSJONSDOKUMENTAJON));
        aksjonspunktResultater.add(AksjonspunktUtlederResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_OM_ADOPSJON_GJELDER_EKTEFELLES_BARN));

        // gjelder bare FAR
        if (RelasjonsRolleType.erFar(param.getRelasjonsRolleType())) {
            aksjonspunktResultater.add(AksjonspunktUtlederResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_OM_SØKER_ER_MANN_SOM_ADOPTERER_ALENE));
        }
        return aksjonspunktResultater;
    }
}

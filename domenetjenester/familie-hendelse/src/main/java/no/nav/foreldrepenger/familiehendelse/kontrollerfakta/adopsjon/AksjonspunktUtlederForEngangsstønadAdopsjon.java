package no.nav.foreldrepenger.familiehendelse.kontrollerfakta.adopsjon;

import static no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat.opprettForAksjonspunkt;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;

@ApplicationScoped
public class AksjonspunktUtlederForEngangsstønadAdopsjon implements AksjonspunktUtleder {

    @Override
    public List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {
        List<AksjonspunktResultat> aksjonspunktResultater = new ArrayList<>();

        // felles for MOR og FAR
        aksjonspunktResultater.add(opprettForAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_ADOPSJONSDOKUMENTAJON));
        aksjonspunktResultater.add(opprettForAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_OM_ADOPSJON_GJELDER_EKTEFELLES_BARN));

        // gjelder bare FAR
        if (RelasjonsRolleType.erFar(param.getRelasjonsRolleType())) {
            aksjonspunktResultater.add(opprettForAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_OM_SØKER_ER_MANN_SOM_ADOPTERER_ALENE));
        }
        return aksjonspunktResultater;
    }
}

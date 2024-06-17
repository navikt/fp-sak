package no.nav.foreldrepenger.familiehendelse.kontrollerfakta.omsorg;

import static no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat.opprettListeForAksjonspunkt;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;


@ApplicationScoped
public class AksjonspunktUtlederForForeldreansvar implements AksjonspunktUtleder {

    AksjonspunktUtlederForForeldreansvar() {
    }

    @Override
    public List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {
        return opprettListeForAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_VILKÅR_FOR_FORELDREANSVAR);
    }

}

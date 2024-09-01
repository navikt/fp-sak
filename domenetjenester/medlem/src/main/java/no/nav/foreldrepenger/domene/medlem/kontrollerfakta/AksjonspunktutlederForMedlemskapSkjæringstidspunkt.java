package no.nav.foreldrepenger.domene.medlem.kontrollerfakta;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.domene.medlem.VurderMedlemskapTjeneste;

@ApplicationScoped
public class AksjonspunktutlederForMedlemskapSkjæringstidspunkt implements AksjonspunktUtleder {

    private VurderMedlemskapTjeneste tjeneste;

    AksjonspunktutlederForMedlemskapSkjæringstidspunkt() {
        //CDI
    }

    @Inject
    public AksjonspunktutlederForMedlemskapSkjæringstidspunkt(VurderMedlemskapTjeneste tjeneste) {
        this.tjeneste = tjeneste;
    }

    @Override
    public List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {
        var skjæringstidspunkt = param.getSkjæringstidspunkt().getUtledetSkjæringstidspunkt();
        var resultat = tjeneste.vurderMedlemskap(param.getRef(), param.getSkjæringstidspunkt(), skjæringstidspunkt);
        return resultat.stream().map(mr -> AksjonspunktResultat.opprettForAksjonspunkt(mr.getAksjonspunktDefinisjon())).toList();
    }

}

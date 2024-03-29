package no.nav.foreldrepenger.behandling.steg.avklarfakta.svp;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederHolder;
import no.nav.foreldrepenger.behandling.steg.avklarfakta.KontrollerFaktaUtledere;
import no.nav.foreldrepenger.domene.medlem.kontrollerfakta.AksjonspunktutlederForMedlemskapSkjæringstidspunkt;

@ApplicationScoped
class KontrollerFaktaUtledereTjenesteImpl implements KontrollerFaktaUtledere {

    protected KontrollerFaktaUtledereTjenesteImpl() {
    }

    @Override
    public List<AksjonspunktUtleder> utledUtledereFor(BehandlingReferanse ref) {
        var utlederHolder = new AksjonspunktUtlederHolder();

        // Legger til utledere som alltid skal kjøres
        leggTilStandardUtledere(utlederHolder);

        return utlederHolder.getUtledere();
    }

    private void leggTilStandardUtledere(AksjonspunktUtlederHolder utlederHolder) {
        utlederHolder.leggTil(AksjonspunktutlederForMedlemskapSkjæringstidspunkt.class);
    }
}

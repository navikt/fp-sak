package no.nav.foreldrepenger.behandling.steg.avklarfakta;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtleder;

import java.util.List;

public interface KontrollerFaktaUtledere {

    List<AksjonspunktUtleder> utledUtledereFor(BehandlingReferanse ref);
}

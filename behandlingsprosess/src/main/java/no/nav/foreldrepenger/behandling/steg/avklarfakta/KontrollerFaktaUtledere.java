package no.nav.foreldrepenger.behandling.steg.avklarfakta;

import java.util.List;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtleder;

public interface KontrollerFaktaUtledere {

    List<AksjonspunktUtleder> utledUtledereFor(BehandlingReferanse ref);
}

package no.nav.foreldrepenger.domene.medlem.api;

import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;

public record VurderMedlemskap(Set<AksjonspunktDefinisjon> aksjonspunkter, Set<VurderingsÅrsak> årsaker) {

}

package no.nav.foreldrepenger.domene.medlem.api;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;

import java.util.Set;

public record VurderMedlemskap(Set<AksjonspunktDefinisjon> aksjonspunkter, Set<VurderingsÅrsak> årsaker) {

}

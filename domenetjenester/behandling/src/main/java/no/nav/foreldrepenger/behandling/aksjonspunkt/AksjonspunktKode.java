package no.nav.foreldrepenger.behandling.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonIgnore;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;

public interface AksjonspunktKode {

    @JsonIgnore
    AksjonspunktDefinisjon getAksjonspunktDefinisjon();
}

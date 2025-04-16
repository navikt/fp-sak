package no.nav.foreldrepenger.behandling.aksjonspunkt;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;

public record OppdateringAksjonspunktResultat(AksjonspunktDefinisjon aksjonspunktDefinisjon, AksjonspunktStatus aksjonspunktStatus) {
}

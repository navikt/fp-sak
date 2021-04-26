package no.nav.foreldrepenger.behandling.aksjonspunkt;

import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;

public record AksjonspunktResultatMedStatus(AksjonspunktResultat aksjonspunktResultat, AksjonspunktStatus aksjonspunktStatus) {}

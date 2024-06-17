package no.nav.foreldrepenger.domene.prosess;

import java.util.List;

import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;

public record KalkulusRespons(List<AksjonspunktResultat> aksjonspunkter, Boolean erVilkårOppfylt) {
}

package no.nav.foreldrepenger.domene.prosess;

import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;

import java.util.List;

public record KalkulusRespons (List<AksjonspunktResultat> aksjonspunkter, VilkårRespons vilkårdata) {
    protected record VilkårRespons (Boolean erVilkårOppfylt, String regelEvalueringSporing, String regelInputSporing, String regelVersjon) {}
}

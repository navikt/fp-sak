package no.nav.foreldrepenger.domene.prosess;

import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;

import java.util.List;

public record KalkulusRespons (List<AksjonspunktResultat> aksjonspunkter, Boolean erVilkårOppfylt) {}

package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import no.nav.folketrygdloven.kalkulator.output.BeregningAksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;

class BeregningResultatMapper {

    static AksjonspunktResultat map(BeregningAksjonspunktResultat beregningResultat) {
        if (beregningResultat.harFrist()) {
            return AksjonspunktResultat.opprettForAksjonspunktMedFrist(
                AksjonspunktDefinisjon.fraKode(beregningResultat.getBeregningAksjonspunktDefinisjon().getKode()),
                Venteårsak.fraKode(beregningResultat.getVenteårsak().getKode()),
                beregningResultat.getVentefrist());
        }
        return AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.fraKode(beregningResultat.getBeregningAksjonspunktDefinisjon().getKode()));
    }
}

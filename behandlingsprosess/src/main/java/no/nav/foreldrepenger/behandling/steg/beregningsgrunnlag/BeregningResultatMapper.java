package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import no.nav.folketrygdloven.kalkulator.output.BeregningAvklaringsbehovResultat;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;

class BeregningResultatMapper {

    static AksjonspunktResultat map(BeregningAvklaringsbehovResultat beregningResultat) {
        if (beregningResultat.harFrist()) {
            return AksjonspunktResultat.opprettForAksjonspunktMedFrist(
                    AksjonspunktDefinisjon.fraKode(beregningResultat.getBeregningAvklaringsbehovDefinisjon().getKode()),
                    Venteårsak.fraKode(beregningResultat.getVenteårsak().getKode()),
                    beregningResultat.getVentefrist());
        }
        return AksjonspunktResultat
                .opprettForAksjonspunkt(AksjonspunktDefinisjon.fraKode(beregningResultat.getBeregningAvklaringsbehovDefinisjon().getKode()));
    }
}

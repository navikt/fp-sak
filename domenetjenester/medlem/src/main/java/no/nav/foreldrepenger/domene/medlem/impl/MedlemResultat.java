
package no.nav.foreldrepenger.domene.medlem.impl;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;

/**Mappes til riktig aksjonspunktDef
 * enten i en inngangsvilkårkontekts
 * eller i en revuderingskontekts
 */
public enum MedlemResultat {
    AVKLAR_OM_ER_BOSATT(AksjonspunktDefinisjon.AVKLAR_OM_ER_BOSATT),
    AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE(AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE),
    AVKLAR_OPPHOLDSRETT(AksjonspunktDefinisjon.AVKLAR_OPPHOLDSRETT),
    AVKLAR_LOVLIG_OPPHOLD(AksjonspunktDefinisjon.AVKLAR_LOVLIG_OPPHOLD),
    ;

    private final AksjonspunktDefinisjon aksjonspunktDefinisjon;

    MedlemResultat(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        this.aksjonspunktDefinisjon = aksjonspunktDefinisjon;
    }

    public AksjonspunktDefinisjon getAksjonspunktDefinisjon() {
        return aksjonspunktDefinisjon;
    }
}

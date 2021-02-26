package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;

public class AvklarOmsorgOgForeldreansvarAksjonspunktData {
    private String vilkarTypeKode;
    private LocalDate omsorgsovertakelseDato;
    private AksjonspunktDefinisjon aksjonspunktDefinisjon;

    public AvklarOmsorgOgForeldreansvarAksjonspunktData(String vilkarTypeKode, AksjonspunktDefinisjon aksjonspunktDefinisjon, LocalDate omsorgsovertakelseDato) {
        this.vilkarTypeKode = vilkarTypeKode;
        this.aksjonspunktDefinisjon = aksjonspunktDefinisjon;
        this.omsorgsovertakelseDato = omsorgsovertakelseDato;
    }

    public AksjonspunktDefinisjon getAksjonspunktDefinisjon() {
        return aksjonspunktDefinisjon;
    }

    public String getVilkarTypeKode() {
        return vilkarTypeKode;
    }

    public LocalDate getOmsorgsovertakelseDato() {
        return omsorgsovertakelseDato;
    }

}

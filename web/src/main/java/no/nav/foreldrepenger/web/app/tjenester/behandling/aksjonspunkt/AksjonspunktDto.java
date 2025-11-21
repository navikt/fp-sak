package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;

public class AksjonspunktDto {
    @NotNull private AksjonspunktDefinisjon definisjon;
    @NotNull private AksjonspunktStatus status;
    private String begrunnelse;
    private VilkårType vilkarType;
    @NotNull private Boolean toTrinnsBehandling;
    private Boolean toTrinnsBehandlingGodkjent;
    @NotNull private AksjonspunktType aksjonspunktType;
    @NotNull private Boolean kanLoses;

    public void setDefinisjon(AksjonspunktDefinisjon definisjon) {
        this.definisjon = definisjon;
    }

    public void setStatus(AksjonspunktStatus status) {
        this.status = status;
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public void setVilkarType(VilkårType vilkarType) {
        this.vilkarType = vilkarType;
    }

    public void setToTrinnsBehandling(Boolean toTrinnsBehandling) {
        this.toTrinnsBehandling = toTrinnsBehandling;
    }

    public void setToTrinnsBehandlingGodkjent(Boolean toTrinnsBehandlingGodkjent) {
        this.toTrinnsBehandlingGodkjent = toTrinnsBehandlingGodkjent;
    }


    public void setAksjonspunktType(AksjonspunktType aksjonspunktType) {
        this.aksjonspunktType = aksjonspunktType;
    }

    public void setKanLoses(Boolean kanLoses) {
        this.kanLoses = kanLoses;
    }


    public AksjonspunktDefinisjon getDefinisjon() {
        return definisjon;
    }

    public VilkårType getVilkarType() {
        return vilkarType;
    }

    public AksjonspunktStatus getStatus() {
        return status;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public Boolean getToTrinnsBehandling() {
        return toTrinnsBehandling;
    }


    public Boolean getToTrinnsBehandlingGodkjent() {
        return toTrinnsBehandlingGodkjent;
    }

    public AksjonspunktType getAksjonspunktType() {
        return aksjonspunktType;
    }

    public Boolean getKanLoses() {
        return kanLoses;
    }



}

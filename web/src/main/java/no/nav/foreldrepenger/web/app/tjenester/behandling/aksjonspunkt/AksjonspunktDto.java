package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import java.time.LocalDateTime;
import java.util.Set;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;

public class AksjonspunktDto {
    @NotNull private AksjonspunktDefinisjon definisjon;
    @NotNull private AksjonspunktStatus status;
    private String begrunnelse;
    private VilkårType vilkarType;
    @NotNull private Boolean toTrinnsBehandling;
    private Boolean toTrinnsBehandlingGodkjent;
    private Set<VurderÅrsak> vurderPaNyttArsaker;
    private String besluttersBegrunnelse;
    @NotNull private AksjonspunktType aksjonspunktType;
    @NotNull private Boolean kanLoses;
    @NotNull private Boolean erAktivt;

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

    public void setVurderPaNyttArsaker(Set<VurderÅrsak> vurderPaNyttArsaker) {
        this.vurderPaNyttArsaker = vurderPaNyttArsaker;
    }

    public void setBesluttersBegrunnelse(String besluttersBegrunnelse) {
        this.besluttersBegrunnelse = besluttersBegrunnelse;
    }

    public void setAksjonspunktType(AksjonspunktType aksjonspunktType) {
        this.aksjonspunktType = aksjonspunktType;
    }

    public void setKanLoses(Boolean kanLoses) {
        this.kanLoses = kanLoses;
    }

    public void setErAktivt(Boolean erAktivt) {
        this.erAktivt = erAktivt;
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

    public Set<VurderÅrsak> getVurderPaNyttArsaker() {
        return vurderPaNyttArsaker;
    }

    public String getBesluttersBegrunnelse() {
        return besluttersBegrunnelse;
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

    public Boolean getErAktivt() {
        return erAktivt;
    }


}

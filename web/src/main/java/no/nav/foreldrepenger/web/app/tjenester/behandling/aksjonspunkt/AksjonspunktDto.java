package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import java.time.LocalDateTime;
import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;

public class AksjonspunktDto {
    @NotNull private AksjonspunktDefinisjon definisjon;
    @NotNull private AksjonspunktStatus status;
    @Schema(nullable = true, requiredMode = Schema.RequiredMode.REQUIRED) private String begrunnelse;
    @Schema(nullable = true, requiredMode = Schema.RequiredMode.REQUIRED) private VilkårType vilkarType;
    @NotNull private Boolean toTrinnsBehandling;
    @Schema(nullable = true, requiredMode = Schema.RequiredMode.REQUIRED) private Boolean toTrinnsBehandlingGodkjent;
    @Schema(nullable = true, requiredMode = Schema.RequiredMode.REQUIRED) private Set<VurderÅrsak> vurderPaNyttArsaker;
    @Schema(nullable = true, requiredMode = Schema.RequiredMode.REQUIRED) private String besluttersBegrunnelse;
    @Schema(nullable = true, requiredMode = Schema.RequiredMode.REQUIRED) private AksjonspunktType aksjonspunktType;
    @NotNull private Boolean kanLoses;
    @NotNull private Boolean erAktivt;
    @Schema(nullable = true, requiredMode = Schema.RequiredMode.REQUIRED) private LocalDateTime fristTid;
    @Schema(nullable = true, requiredMode = Schema.RequiredMode.REQUIRED) private LocalDateTime endretTidspunkt;
    @Schema(nullable = true, requiredMode = Schema.RequiredMode.REQUIRED) private String endretAv;

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

    public LocalDateTime getFristTid() {
        return fristTid;
    }

    public void setFristTid(LocalDateTime fristTid) {
        this.fristTid = fristTid;
    }

    public LocalDateTime getEndretTidspunkt() {
        return endretTidspunkt;
    }

    public void setEndretTidspunkt(LocalDateTime endretTidspunkt) {
        this.endretTidspunkt = endretTidspunkt;
    }

    public String getEndretAv() {
        return endretAv;
    }

    public void setEndretAv(String endretAv) {
        this.endretAv = endretAv;
    }

}

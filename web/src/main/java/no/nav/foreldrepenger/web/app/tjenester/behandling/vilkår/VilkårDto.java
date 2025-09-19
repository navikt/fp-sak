package no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonRawValue;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;

public class VilkårDto {

    @NotNull private VilkårType vilkarType;
    @NotNull private VilkårUtfallType vilkarStatus;
    private String avslagKode;
    private String lovReferanse;
    @NotNull private Boolean overstyrbar;

    @JsonRawValue
    @JsonInclude(Include.NON_NULL)
    private String evaluering;

    @JsonRawValue
    @JsonInclude(Include.NON_NULL)
    private String input;

    public VilkårDto(VilkårType vilkårType,
                     VilkårUtfallType vilkårUtfallType,
                     String avslagKode,
                     String lovReferanse) {
        this.vilkarType = vilkårType;
        this.vilkarStatus = vilkårUtfallType;
        this.avslagKode = avslagKode;
        this.lovReferanse = lovReferanse;
    }

    public VilkårDto() {
    }

    public VilkårType getVilkarType() {
        return vilkarType;
    }

    public VilkårUtfallType getVilkarStatus() {
        return vilkarStatus;
    }

    public String getAvslagKode() {
        return avslagKode;
    }

    public String getLovReferanse() {
        return lovReferanse;
    }

    public String getEvaluering() {
        return evaluering;
    }

    public String getInput() {
        return input;
    }

    public void setVilkarType(VilkårType vilkarType) {
        this.vilkarType = vilkarType;
    }

    public void setVilkarStatus(VilkårUtfallType vilkarStatus) {
        this.vilkarStatus = vilkarStatus;
    }

    public void setAvslagKode(String avslagKode) {
        this.avslagKode = avslagKode;
    }

    public void setEvaluering(String evaluering) {
        this.evaluering = evaluering;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public void setLovReferanse(String lovReferanse) {
        this.lovReferanse = lovReferanse;
    }

    public void setOverstyrbar(boolean overstyrbar) {
        this.overstyrbar = overstyrbar;
    }

    public boolean isOverstyrbar() {
        return overstyrbar;
    }
}

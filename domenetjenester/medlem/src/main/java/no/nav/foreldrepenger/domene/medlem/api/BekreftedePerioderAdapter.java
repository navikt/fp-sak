package no.nav.foreldrepenger.domene.medlem.api;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapManuellVurderingType;

public class BekreftedePerioderAdapter {

    private LocalDate vurderingsdato;
    private List<String> aksjonspunkter = new ArrayList<>();
    private Boolean bosattVurdering;
    private Boolean erEosBorger;
    private Boolean oppholdsrettVurdering;
    private Boolean lovligOppholdVurdering;
    private LocalDate fodselsdato;
    private MedlemskapManuellVurderingType medlemskapManuellVurderingType;
    private String omsorgsovertakelseDato;
    private String begrunnelse;

    public BekreftedePerioderAdapter() {
    }

    public LocalDate getVurderingsdato() {
        return vurderingsdato;
    }

    public void setVurderingsdato(LocalDate vurderingsdato) {
        this.vurderingsdato = vurderingsdato;
    }

    public List<String> getAksjonspunkter() {
        return aksjonspunkter;
    }

    public void setAksjonspunkter(List<String> aksjonspunkter) {
        this.aksjonspunkter = aksjonspunkter;
    }

    public Boolean getBosattVurdering() {
        return bosattVurdering;
    }

    public void setBosattVurdering(Boolean bosattVurdering) {
        this.bosattVurdering = bosattVurdering;
    }

    public Boolean getErEosBorger() {
        return erEosBorger;
    }

    public void setErEosBorger(Boolean erEosBorger) {
        this.erEosBorger = erEosBorger;
    }

    public Boolean getOppholdsrettVurdering() {
        return oppholdsrettVurdering;
    }

    public void setOppholdsrettVurdering(Boolean oppholdsrettVurdering) {
        this.oppholdsrettVurdering = oppholdsrettVurdering;
    }

    public Boolean getLovligOppholdVurdering() {
        return lovligOppholdVurdering;
    }

    public void setLovligOppholdVurdering(Boolean lovligOppholdVurdering) {
        this.lovligOppholdVurdering = lovligOppholdVurdering;
    }

    public LocalDate getFodselsdato() {
        return fodselsdato;
    }

    public void setFodselsdato(LocalDate fodselsdato) {
        this.fodselsdato = fodselsdato;
    }

    public MedlemskapManuellVurderingType getMedlemskapManuellVurderingType() {
        return medlemskapManuellVurderingType;
    }

    public void setMedlemskapManuellVurderingType(MedlemskapManuellVurderingType medlemskapManuellVurderingType) {
        this.medlemskapManuellVurderingType = medlemskapManuellVurderingType;
    }

    public String getOmsorgsovertakelseDato() {
        return omsorgsovertakelseDato;
    }

    public void setOmsorgsovertakelseDato(String omsorgsovertakelseDato) {
        this.omsorgsovertakelseDato = omsorgsovertakelseDato;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }
}

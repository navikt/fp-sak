package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt;

import jakarta.validation.constraints.NotNull;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapManuellVurderingType;
import no.nav.foreldrepenger.validering.ValidKodeverk;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BekreftedePerioderDto {

    @NotNull
    private LocalDate vurderingsdato;
    private List<String> aksjonspunkter = new ArrayList<>();
    private Boolean bosattVurdering;
    private Boolean erEosBorger;
    private Boolean oppholdsrettVurdering;
    private Boolean lovligOppholdVurdering;
    @ValidKodeverk
    private MedlemskapManuellVurderingType medlemskapManuellVurderingType;
    private String omsorgsovertakelseDato;
    private String begrunnelse;

    public BekreftedePerioderDto() {
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
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
}

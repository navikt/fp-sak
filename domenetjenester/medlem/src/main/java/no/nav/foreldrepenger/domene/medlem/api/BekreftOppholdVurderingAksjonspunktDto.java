package no.nav.foreldrepenger.domene.medlem.api;

public class BekreftOppholdVurderingAksjonspunktDto {
    private Boolean oppholdsrettVurdering;
    private Boolean lovligOppholdVurdering;
    private Boolean erEosBorger;
    private String begrunnelse;

    public BekreftOppholdVurderingAksjonspunktDto(Boolean oppholdsrettVurdering, Boolean lovligOppholdVurdering, Boolean erEosBorger, String begrunnelse) {
        this.oppholdsrettVurdering = oppholdsrettVurdering;
        this.lovligOppholdVurdering = lovligOppholdVurdering;
        this.erEosBorger = erEosBorger;
        this.begrunnelse = begrunnelse;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public Boolean getOppholdsrettVurdering() {
        return oppholdsrettVurdering;
    }

    public Boolean getLovligOppholdVurdering() {
        return lovligOppholdVurdering;
    }

    public Boolean getErEosBorger() {
        return erEosBorger;
    }
}

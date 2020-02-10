package no.nav.foreldrepenger.domene.medlem.api;

public class BekreftBosattVurderingAksjonspunktDto {
    private Boolean bosattVurdering;
    private String begrunnelse;


    public BekreftBosattVurderingAksjonspunktDto(Boolean bosattVurdering, String begrunnelse) {
        this.bosattVurdering = bosattVurdering;
        this.begrunnelse = begrunnelse;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }
    public Boolean getBosattVurdering() {
        return bosattVurdering;
    }
}

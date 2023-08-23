package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;


import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;
import no.nav.foreldrepenger.validering.ValidKodeverk;

import java.math.BigDecimal;
import java.time.LocalDate;

public class VelferdspermisjonDto {

    @JsonProperty("permisjonFom")
    private LocalDate permisjonFom;
    @JsonProperty("permisjonTom")
    private LocalDate permisjonTom;
    @JsonProperty("permisjonsprosent")
    private BigDecimal permisjonsprosent;
    @ValidKodeverk
    @JsonProperty("type")
    private PermisjonsbeskrivelseType type;
    @JsonProperty("erGyldig")
    private Boolean erGyldig;

    public VelferdspermisjonDto(){
        // Jackson
    }

    public VelferdspermisjonDto(LocalDate permisjonFom,
                                LocalDate permisjonTom,
                                BigDecimal permisjonsprosent,
                                PermisjonsbeskrivelseType type,
                                Boolean erGyldig) {
        this.permisjonFom = permisjonFom;
        this.permisjonTom = permisjonTom;
        this.permisjonsprosent = permisjonsprosent;
        this.type = type;
        this.erGyldig = erGyldig;
    }

    public LocalDate getPermisjonFom() {
        return permisjonFom;
    }

    public void setPermisjonFom(LocalDate permisjonFom) {
        this.permisjonFom = permisjonFom;
    }

    public LocalDate getPermisjonTom() {
        return permisjonTom;
    }

    public void setPermisjonTom(LocalDate permisjonTom) {
        this.permisjonTom = permisjonTom;
    }

    public BigDecimal getPermisjonsprosent() {
        return permisjonsprosent;
    }

    public void setPermisjonsprosent(BigDecimal permisjonsprosent) {
        this.permisjonsprosent = permisjonsprosent;
    }

    public PermisjonsbeskrivelseType getType() {
        return type;
    }

    public void setType(PermisjonsbeskrivelseType type) {
        this.type = type;
    }

    public Boolean getErGyldig() {
        return erGyldig;
    }

    public void setErGyldig(Boolean erGyldig) {
        this.erGyldig = erGyldig;
    }
}

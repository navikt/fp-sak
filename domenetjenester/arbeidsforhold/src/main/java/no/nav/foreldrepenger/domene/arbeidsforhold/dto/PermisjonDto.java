package no.nav.foreldrepenger.domene.arbeidsforhold.dto;


import java.math.BigDecimal;
import java.time.LocalDate;

import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;

public class PermisjonDto {

    private LocalDate permisjonFom;
    private LocalDate permisjonTom;
    private BigDecimal permisjonsprosent;
    private PermisjonsbeskrivelseType type;

    PermisjonDto(){
        // Skjul private constructor
    }

    public PermisjonDto(LocalDate permisjonFom, LocalDate permisjonTom, BigDecimal permisjonsprosent, PermisjonsbeskrivelseType type) {
        this.permisjonFom = permisjonFom;
        this.permisjonTom = permisjonTom;
        this.permisjonsprosent = permisjonsprosent;
        this.type = type;
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

}

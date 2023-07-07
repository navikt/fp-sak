package no.nav.foreldrepenger.web.app.tjenester.behandling.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import no.nav.vedtak.util.InputValideringRegex;

public class ByttBehandlendeEnhetDto extends DtoMedBehandlingId {

    @Size(min = 1, max = 256)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String enhetNavn;

    @Size(max = 10)
    @Pattern(regexp = InputValideringRegex.KODEVERK)
    private String enhetId;

    @Size(max = 4000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String begrunnelse;

    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long behandlingVersjon;

    public String getEnhetId() {
        return enhetId;
    }

    public void setEnhetId(String enhetId) {
        this.enhetId = enhetId;
    }

    public String getEnhetNavn() {
        return enhetNavn;
    }

    public void setEnhetNavn(String enhetNavn) {
        this.enhetNavn = enhetNavn;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public Long getBehandlingVersjon() {
        return behandlingVersjon;
    }

}

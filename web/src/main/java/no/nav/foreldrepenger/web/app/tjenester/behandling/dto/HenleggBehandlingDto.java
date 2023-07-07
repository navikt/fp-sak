package no.nav.foreldrepenger.web.app.tjenester.behandling.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import no.nav.vedtak.util.InputValideringRegex;

public class HenleggBehandlingDto extends DtoMedBehandlingId  {


    @NotNull
    @Size(min = 1, max = 100)
    @Pattern(regexp = InputValideringRegex.KODEVERK)
    private String årsakKode;

    @Size(max = 4000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String begrunnelse;

    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long behandlingVersjon;

    public String getÅrsakKode() {
        return årsakKode;
    }

    public void setÅrsakKode(String årsakKode) {
        this.årsakKode = årsakKode;
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

    public void setBehandlingVersjon(Long behandlingVersjon) {
        this.behandlingVersjon = behandlingVersjon;
    }


}

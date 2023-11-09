package no.nav.foreldrepenger.web.app.tjenester.behandling.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.validering.ValidKodeverk;

public class SettBehandlingPaVentDto extends DtoMedBehandlingId  {

    @NotNull
    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long behandlingVersjon;

    private LocalDate frist;

    @ValidKodeverk
    private Venteårsak ventearsak;


    public Long getBehandlingVersjon() {
        return behandlingVersjon;
    }

    public void setBehandlingVersjon(Long behandlingVersjon) {
        this.behandlingVersjon = behandlingVersjon;
    }

    public LocalDate getFrist() {
        return frist;
    }

    public void setFrist(LocalDate frist) {
        this.frist = frist;
    }

    public Venteårsak getVentearsak() {
        return ventearsak;
    }

    public void setVentearsak(Venteårsak ventearsak) {
        this.ventearsak = ventearsak;
    }


}

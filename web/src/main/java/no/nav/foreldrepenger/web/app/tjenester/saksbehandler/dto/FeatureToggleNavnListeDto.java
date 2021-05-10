package no.nav.foreldrepenger.web.app.tjenester.saksbehandler.dto;

import java.util.Collection;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class FeatureToggleNavnListeDto {

    @Valid
    @NotNull
    @Size(min = 1, max = 10)
    private Collection<FeatureToggleNavnDto> toggles;

    public FeatureToggleNavnListeDto() {
        //trengs for jackson
    }

    public FeatureToggleNavnListeDto(Collection<FeatureToggleNavnDto> toggles) {
        this.toggles = toggles;
    }

    public Collection<FeatureToggleNavnDto> getToggles() {
        return toggles;
    }

}

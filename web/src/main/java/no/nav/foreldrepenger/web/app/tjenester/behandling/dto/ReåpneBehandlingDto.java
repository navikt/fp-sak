package no.nav.foreldrepenger.web.app.tjenester.behandling.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ReåpneBehandlingDto extends DtoMedBehandlingId {

    @NotNull
    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long behandlingVersjon;


    public Long getBehandlingVersjon() {
        return behandlingVersjon;
    }

    public void setBehandlingVersjon(Long behandlingVersjon) {
        this.behandlingVersjon = behandlingVersjon;
    }

    @Override
    public String toString() {
        return "BehandlingIdDto{" + "behandlingId=" + getBehandlingUuid() + '}';
    }
}

package no.nav.foreldrepenger.web.app.tjenester.behandling.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class BehandlingIdVersjonDto extends BehandlingIdDto {

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
        return getClass().getSimpleName() + '<' +
                (getSaksnummer() == null ? "" : "saksnummer=" + getSaksnummer() + ", ") +
                (getBehandlingVersjon() != null ? "behandlingVersjon=" + getBehandlingVersjon() + ", " : "") +
                (getBehandlingId() != null ? "behandlingId=" + getBehandlingId() : "") +
                ((getBehandlingId() != null) && (getBehandlingUuid() != null) ? ", " : "") +
                (getBehandlingUuid() != null ? "behandlingUuid=" + getBehandlingUuid() : "") +
                '>';
    }
}

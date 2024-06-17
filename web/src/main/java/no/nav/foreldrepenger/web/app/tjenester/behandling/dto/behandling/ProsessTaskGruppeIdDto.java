package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ProsessTaskGruppeIdDto {

    @Size(min = 1, max = 250)
    @Pattern(regexp = "[a-zA-Z0-9-.]+")
    private String gruppe;

    public ProsessTaskGruppeIdDto() {
        gruppe = null;
    }

    public ProsessTaskGruppeIdDto(String gruppe) {
        this.gruppe = gruppe;
    }

    public String getGruppe() {
        return gruppe;
    }

    @Override
    public String toString() {
        return "BehandlingIdDto{" + "behandlingId=" + gruppe + '}';
    }
}

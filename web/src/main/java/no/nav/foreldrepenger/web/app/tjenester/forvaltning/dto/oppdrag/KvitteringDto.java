package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.oppdrag;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.DefaultValue;

public class KvitteringDto extends K27PatchDto {

    @NotNull
    @DefaultValue("true")
    private Boolean oppdaterProsessTask;

    public Boolean getOppdaterProsessTask() {
        return oppdaterProsessTask;
    }

    public void setOppdaterProsessTask(Boolean oppdaterProsessTask) {
        this.oppdaterProsessTask = oppdaterProsessTask;
    }
}

package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.oppdrag;

import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;

import no.nav.vedtak.sikkerhet.abac.AbacDto;

public class KvitteringDto extends K27PatchDto implements AbacDto {

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

package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.oppdrag;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;

import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

public class KvitteringDto implements AbacDto {

    @Min(0)
    @Max(Long.MAX_VALUE)
    @NotNull
    private Long behandlingId;

    @Min(0)
    @Max(Long.MAX_VALUE)
    @NotNull
    private Long fagsystemId;

    @NotNull
    @DefaultValue("true")
    private Boolean oppdaterProsessTask;

    public Boolean getOppdaterProsessTask() {
        return oppdaterProsessTask;
    }

    public void setOppdaterProsessTask(Boolean oppdaterProsessTask) {
        this.oppdaterProsessTask = oppdaterProsessTask;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public void setBehandlingId(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    public Long getFagsystemId() {
        return fagsystemId;
    }

    public void setFagsystemId(Long fagsystemId) {
        this.fagsystemId = fagsystemId;
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett()
            .leggTil(AppAbacAttributtType.BEHANDLING_ID, behandlingId);
    }
}

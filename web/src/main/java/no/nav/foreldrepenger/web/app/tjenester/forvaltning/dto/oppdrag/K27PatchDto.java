package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.oppdrag;

import java.time.LocalDate;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import no.nav.foreldrepenger.web.server.abac.AbacAttributter;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

public class K27PatchDto implements AbacDto {

    @Min(0)
    @Max(Long.MAX_VALUE)
    @NotNull
    private Long behandlingId;

    @Min(0)
    @Max(Long.MAX_VALUE)
    @NotNull
    private Long fagsystemId;

    @NotNull
    private LocalDate maksDato;

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

    public LocalDate getMaksDato() {
        return maksDato;
    }

    public void setMaksDato(final LocalDate maksDato) {
        this.maksDato = maksDato;
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett()
            .leggTil(AppAbacAttributtType.BEHANDLING_ID, behandlingId);
    }
}

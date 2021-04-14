package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;

import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;
import no.nav.vedtak.util.InputValideringRegex;

public class HoppTilbakeDto implements AbacDto {

    @NotNull
    @QueryParam("behandlingId")
    @DefaultValue("0")
    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long behandlingId;

    @NotNull
    @QueryParam("behandlingStegType")
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String behandlingStegType;

    public HoppTilbakeDto(@NotNull Long behandlingId, @NotNull String behandlingStegType) {
        this.behandlingId = behandlingId;
        this.behandlingStegType = behandlingStegType;
    }

    public HoppTilbakeDto() {
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        var abac = AbacDataAttributter.opprett();
        if (behandlingId != null) {
            abac.leggTil(AppAbacAttributtType.BEHANDLING_ID, behandlingId);
        }
        return abac;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public String getBehandlingStegType() {
        return behandlingStegType;
    }
}

package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import java.util.Objects;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.QueryParam;

import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

/**
 * For at Swagger-brukerne skal slippe å bli forvirret av at det også kommer opp saksnummer og UUID,
 * har vi her en forenklet versjon av BehandlingIdDto til forvaltningstjenestene.
 */
public class ForvaltningBehandlingIdDto implements AbacDto {

    @NotNull
    @QueryParam("behandlingId")
    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long behandlingId;

    public ForvaltningBehandlingIdDto() {
    }

    public ForvaltningBehandlingIdDto(@NotNull String behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        this.behandlingId = Long.valueOf(behandlingId);
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        var abac = AbacDataAttributter.opprett();
        if (behandlingId != null) {
            abac.leggTil(AppAbacAttributtType.BEHANDLING_ID, behandlingId);
        }
        return abac;
    }
}

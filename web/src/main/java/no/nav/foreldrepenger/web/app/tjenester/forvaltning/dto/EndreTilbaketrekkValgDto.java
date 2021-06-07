package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.QueryParam;

public class EndreTilbaketrekkValgDto implements AbacDto {

    @QueryParam("behandlingId")
    @Min(0)
    @Max(Long.MAX_VALUE)
    @NotNull
    private Long behandlingId;

    @NotNull
    @QueryParam("skalHindreTilbaketrekk")
    private Boolean skalHindreTilbaketrekk;


    public EndreTilbaketrekkValgDto(@Min(0) @Max(Long.MAX_VALUE) @NotNull Long behandlingId,
                                    @NotNull Boolean skalHindreTilbaketrekk) {
        this.behandlingId = behandlingId;
        this.skalHindreTilbaketrekk = skalHindreTilbaketrekk;
    }

    public EndreTilbaketrekkValgDto() {
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett();
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public Boolean getSkalHindreTilbaketrekk() {
        return skalHindreTilbaketrekk;
    }
}

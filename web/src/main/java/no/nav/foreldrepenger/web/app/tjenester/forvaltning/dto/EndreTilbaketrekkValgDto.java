package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.QueryParam;

public class EndreTilbaketrekkValgDto extends ForvaltningBehandlingIdDto {

    @NotNull
    @QueryParam("skalHindreTilbaketrekk")
    private Boolean skalHindreTilbaketrekk;

    public Boolean getSkalHindreTilbaketrekk() {
        return skalHindreTilbaketrekk;
    }
}

package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import javax.validation.constraints.NotNull;
import javax.ws.rs.QueryParam;

public class EndreTilbaketrekkValgDto extends ForvaltningBehandlingIdDto {

    @NotNull
    @QueryParam("skalHindreTilbaketrekk")
    private Boolean skalHindreTilbaketrekk;

    public Boolean getSkalHindreTilbaketrekk() {
        return skalHindreTilbaketrekk;
    }
}

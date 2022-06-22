package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.ws.rs.QueryParam;

import no.nav.vedtak.util.InputValideringRegex;

public class HoppTilbakeDto extends ForvaltningBehandlingIdDto {

    @NotNull
    @QueryParam("behandlingStegType")
    private String behandlingStegType;

    public String getBehandlingStegType() {
        return behandlingStegType;
    }
}

package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import javax.validation.constraints.NotNull;
import javax.ws.rs.QueryParam;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;

public class HoppTilbakeDto extends ForvaltningBehandlingIdDto {

    @NotNull
    @QueryParam("behandlingStegType")
    private BehandlingStegType behandlingStegType;

    public BehandlingStegType getBehandlingStegType() {
        return behandlingStegType;
    }
}

package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.QueryParam;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.validering.ValidKodeverk;

public class HoppTilbakeDto extends ForvaltningBehandlingIdDto {

    @NotNull
    @QueryParam("målSteg")
    @ValidKodeverk
    private BehandlingStegType behandlingStegType;

    public BehandlingStegType getBehandlingStegType() {
        return behandlingStegType;
    }
}

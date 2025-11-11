package no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingVidereBehandling;

public class TilbakekrevingValgDto {
    private final Boolean erTilbakekrevingVilkårOppfylt;

    @NotNull private final TilbakekrevingVidereBehandling videreBehandling;  //null når erTilbakekrevingVilkårOppfylt

    private final String varseltekst;

    public TilbakekrevingValgDto(Boolean erTilbakekrevingVilkårOppfylt, TilbakekrevingVidereBehandling videreBehandling, String varseltekst) {
        this.erTilbakekrevingVilkårOppfylt = erTilbakekrevingVilkårOppfylt;
        this.videreBehandling = videreBehandling;
        this.varseltekst = varseltekst;
    }

    public String getVarseltekst() {
        return varseltekst;
    }

    public Boolean erTilbakekrevingVilkårOppfylt() {
        return erTilbakekrevingVilkårOppfylt;
    }


    public TilbakekrevingVidereBehandling getVidereBehandling() {
        return videreBehandling;
    }
}


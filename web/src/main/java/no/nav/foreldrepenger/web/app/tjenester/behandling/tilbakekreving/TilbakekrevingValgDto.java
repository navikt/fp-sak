package no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingVidereBehandling;

public class TilbakekrevingValgDto {
    private Boolean erTilbakekrevingVilkårOppfylt;

    private Boolean grunnerTilReduksjon; //null når !erTilbakekrevingVilkårOppfylt

    @NotNull private TilbakekrevingVidereBehandling videreBehandling;  //null når erTilbakekrevingVilkårOppfylt

    private String varseltekst;

    public TilbakekrevingValgDto(Boolean erTilbakekrevingVilkårOppfylt, Boolean grunnerTilReduksjon, TilbakekrevingVidereBehandling videreBehandling, String varseltekst) {
        this.erTilbakekrevingVilkårOppfylt = erTilbakekrevingVilkårOppfylt;
        this.grunnerTilReduksjon = grunnerTilReduksjon;
        this.videreBehandling = videreBehandling;
        this.varseltekst = varseltekst;
    }

    public String getVarseltekst() {
        return varseltekst;
    }

    public Boolean erTilbakekrevingVilkårOppfylt() {
        return erTilbakekrevingVilkårOppfylt;
    }

    public Boolean getGrunnerTilReduksjon() {
        return grunnerTilReduksjon;
    }

    public TilbakekrevingVidereBehandling getVidereBehandling() {
        return videreBehandling;
    }
}


package no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving;

public class TilbakekrevingValg {

    private final Boolean erTilbakekrevingVilkårOppfylt;
    private final Boolean grunnerTilReduksjon;
    private final TilbakekrevingVidereBehandling videreBehandling;
    private final String varseltekst;

    public TilbakekrevingValg(Boolean erTilbakekrevingVilkårOppfylt, Boolean grunnerTilReduksjon, TilbakekrevingVidereBehandling videreBehandling, String varseltekst) {
        this.erTilbakekrevingVilkårOppfylt = erTilbakekrevingVilkårOppfylt;
        this.grunnerTilReduksjon = grunnerTilReduksjon;
        this.videreBehandling = videreBehandling;
        this.varseltekst = varseltekst;
    }

    public static TilbakekrevingValg utenMulighetForInntrekk(TilbakekrevingVidereBehandling videreBehandling, String varseltekst) {
        return new TilbakekrevingValg(null, null, videreBehandling, varseltekst);
    }

    public static TilbakekrevingValg medMulighetForInntrekk(Boolean erTilbakekrevingVilkårOppfylt, Boolean grunnerTilReduksjon, TilbakekrevingVidereBehandling videreBehandling) {
        return new TilbakekrevingValg(erTilbakekrevingVilkårOppfylt, grunnerTilReduksjon, videreBehandling, null);
    }

    public static TilbakekrevingValg medAutomatiskInntrekk() {
        return new TilbakekrevingValg(null, null, TilbakekrevingVidereBehandling.INNTREKK, null);
    }

    public static TilbakekrevingValg medOppdaterTilbakekrevingsbehandling() {
        return new TilbakekrevingValg(null, null, TilbakekrevingVidereBehandling.TILBAKEKR_OPPDATER, null);
    }

    public Boolean getErTilbakekrevingVilkårOppfylt() {
        return erTilbakekrevingVilkårOppfylt;
    }

    public Boolean getGrunnerTilReduksjon() {
        return grunnerTilReduksjon;
    }

    public TilbakekrevingVidereBehandling getVidereBehandling() {
        return videreBehandling;
    }

    public String getVarseltekst() {
        return varseltekst;
    }
}

package no.nav.foreldrepenger.domene.output;

import java.util.List;

import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;

public class BeregningsgrunnlagVilkårOgAkjonspunktResultat {
    private final List<AksjonspunktResultat> aksjonspunkter;
    private Boolean vilkårOppfylt;
    private String regelEvalueringVilkårVurdering;
    private String regelInputVilkårVurdering;
    private String regelVersjonVilkårVurdering;

    public BeregningsgrunnlagVilkårOgAkjonspunktResultat(List<AksjonspunktResultat> aksjonspunktResultatListe) {
        this.aksjonspunkter = aksjonspunktResultatListe;
    }

    public List<AksjonspunktResultat> getAksjonspunkter() {
        return aksjonspunkter;
    }

    public Boolean getVilkårOppfylt() {
        return vilkårOppfylt;
    }

    public void setVilkårOppfylt(Boolean vilkårOppfylt, String regelEvalueringVilkårVurdering,
                                 String regelInputVilkårVurdering, String regelVersjonVilkårVurdering) {
        this.regelEvalueringVilkårVurdering = regelEvalueringVilkårVurdering;
        this.regelInputVilkårVurdering = regelInputVilkårVurdering;
        this.regelVersjonVilkårVurdering = regelVersjonVilkårVurdering;
        this.vilkårOppfylt = vilkårOppfylt;
    }

    public String getRegelEvalueringVilkårVurdering() {
        return regelEvalueringVilkårVurdering;
    }

    public String getRegelInputVilkårVurdering() {
        return regelInputVilkårVurdering;
    }

    public String getRegelVersjonVilkårVurdering() {
        return regelVersjonVilkårVurdering;
    }
}

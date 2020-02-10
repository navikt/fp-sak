package no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingVidereBehandling;

@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_INNTREKK_KODE)
public class VurderFeilutbetalingOgInntrekkDto extends BekreftetAksjonspunktDto {


    private boolean erTilbakekrevingVilkårOppfylt;

    private Boolean grunnerTilReduksjon; //null når !erTilbakekrevingVilkårOppfylt

    private TilbakekrevingVidereBehandling videreBehandling;  //null når erTilbakekrevingVilkårOppfylt


    VurderFeilutbetalingOgInntrekkDto() {
        //for jackson/resteasy
    }

    public VurderFeilutbetalingOgInntrekkDto(String begrunnelse, boolean erTilbakekrevingVilkårOppfylt, Boolean grunnerTilReduksjon, TilbakekrevingVidereBehandling videreBehandling) {
        super(begrunnelse);
        this.erTilbakekrevingVilkårOppfylt = erTilbakekrevingVilkårOppfylt;
        this.grunnerTilReduksjon = grunnerTilReduksjon;
        this.videreBehandling = videreBehandling;
    }

    public boolean getErTilbakekrevingVilkårOppfylt() {
        return erTilbakekrevingVilkårOppfylt;
    }

    public Boolean getGrunnerTilReduksjon() {
        return grunnerTilReduksjon;
    }

    public TilbakekrevingVidereBehandling getVidereBehandling() {
        return videreBehandling;
    }

}

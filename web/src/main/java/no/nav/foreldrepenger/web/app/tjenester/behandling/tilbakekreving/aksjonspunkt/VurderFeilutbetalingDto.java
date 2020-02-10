package no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving.aksjonspunkt;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.vedtak.util.InputValideringRegex;

@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_FEILUTBETALING_KODE)
public class VurderFeilutbetalingDto extends BekreftetAksjonspunktDto {


    @NotNull
    private TilbakekrevingVidereBehandling videreBehandling;

    @Size(max = 3000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String varseltekst;

    VurderFeilutbetalingDto() {
        //for jackson/resteasy
    }

    public VurderFeilutbetalingDto(String begrunnelse, TilbakekrevingVidereBehandling videreBehandling, String varseltekst) {
        super(begrunnelse);
        this.videreBehandling = videreBehandling;
        this.varseltekst = varseltekst;
    }

    public TilbakekrevingVidereBehandling getVidereBehandling() {
        return videreBehandling;
    }


    public String getVarseltekst() {
        return varseltekst;
    }

}

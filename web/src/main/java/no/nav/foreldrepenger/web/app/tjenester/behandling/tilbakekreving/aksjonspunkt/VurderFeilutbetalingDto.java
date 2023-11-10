package no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving.aksjonspunkt;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.util.InputValideringRegex;

@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_FEILUTBETALING_KODE)
public class VurderFeilutbetalingDto extends BekreftetAksjonspunktDto {


    @NotNull
    @ValidKodeverk
    private TilbakekrevingVidereBehandling videreBehandling;

    @Size(max = 3000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String varseltekst;

    VurderFeilutbetalingDto() {
        //for CDI
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

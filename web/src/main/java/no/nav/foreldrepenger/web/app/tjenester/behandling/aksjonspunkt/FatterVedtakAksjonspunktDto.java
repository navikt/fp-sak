package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import java.util.Collection;

import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt.AksjonspunktGodkjenningDto;

@JsonTypeName(AksjonspunktKodeDefinisjon.FATTER_VEDTAK_KODE)
public class FatterVedtakAksjonspunktDto extends BekreftetAksjonspunktDto {

    private Collection<@Valid AksjonspunktGodkjenningDto> aksjonspunktGodkjenningDtos;

    FatterVedtakAksjonspunktDto() {
        // For Jackson
    }

    public FatterVedtakAksjonspunktDto(String begrunnelse, Collection<@Valid AksjonspunktGodkjenningDto> aksjonspunktGodkjenningDtos) {
        super(begrunnelse);
        this.aksjonspunktGodkjenningDtos = aksjonspunktGodkjenningDtos;
    }


    public Collection<AksjonspunktGodkjenningDto> getAksjonspunktGodkjenningDtos() {
        return aksjonspunktGodkjenningDtos;
    }
}

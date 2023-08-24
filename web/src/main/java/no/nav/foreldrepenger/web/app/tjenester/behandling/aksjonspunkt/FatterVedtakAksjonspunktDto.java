package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.Valid;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt.AksjonspunktGodkjenningDto;

import java.util.Collection;

@JsonTypeName(AksjonspunktKodeDefinisjon.FATTER_VEDTAK_KODE)
public class FatterVedtakAksjonspunktDto extends BekreftetAksjonspunktDto {

    @Valid
    private Collection<AksjonspunktGodkjenningDto> aksjonspunktGodkjenningDtos;

    FatterVedtakAksjonspunktDto() {
        // For Jackson
    }

    public FatterVedtakAksjonspunktDto(String begrunnelse, Collection<AksjonspunktGodkjenningDto> aksjonspunktGodkjenningDtos) {
        super(begrunnelse);
        this.aksjonspunktGodkjenningDtos = aksjonspunktGodkjenningDtos;
    }


    public Collection<AksjonspunktGodkjenningDto> getAksjonspunktGodkjenningDtos() {
        return aksjonspunktGodkjenningDtos;
    }
}

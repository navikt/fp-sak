package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.VURDERE_INNTEKTSMELDING_FØR_VEDTAK_KODE)
public class VurdereInntektsmeldingFørVedtakDto extends BekreftetAksjonspunktDto {


    VurdereInntektsmeldingFørVedtakDto() {
        // For Jackson
    }

    public VurdereInntektsmeldingFørVedtakDto(String begrunnelse) {
        super(begrunnelse);
    }

}

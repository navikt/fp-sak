package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_ARBEIDSFORHOLD_INNTEKTSMELDING_KODE)
public class BekreftArbeidInntektsmeldingAksjonspunktDto extends BekreftetAksjonspunktDto {

    @SuppressWarnings("unused")
    private BekreftArbeidInntektsmeldingAksjonspunktDto() {
        super();
        // For Jackson
    }
}

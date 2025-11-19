package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_PERMISJON_UTEN_SLUTTDATO_KODE)
public class BekreftArbeidMedPermisjonUtenSluttdatoDto extends BekreftetAksjonspunktDto {

    @NotNull
    @Size(min = 1, max = 10)
    private List<@Valid AvklarPermisjonUtenSluttdatoDto> arbeidsforhold;

    @SuppressWarnings("unused")
    private BekreftArbeidMedPermisjonUtenSluttdatoDto() {
        super();
        // For Jackson
    }

    public BekreftArbeidMedPermisjonUtenSluttdatoDto(String begrunnelse, List<AvklarPermisjonUtenSluttdatoDto> avklartePermisjonerUtenSluttdato) {
        super(begrunnelse);
        this.arbeidsforhold = avklartePermisjonerUtenSluttdato;
    }

    public List<AvklarPermisjonUtenSluttdatoDto> getArbeidsforhold() {
        return arbeidsforhold;
    }
}

package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

import java.util.List;

@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_PERMISJON_UTEN_SLUTTDATO_KODE)
public class BekreftArbeidMedPermisjonUtenSluttdatoDto extends BekreftetAksjonspunktDto {
    @Valid
    @Size(max = 1000)
    @NotNull
    @Size(min = 1, max = 10)
    private List<AvklarPermisjonUtenSluttdatoDto> arbeidsforhold;

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

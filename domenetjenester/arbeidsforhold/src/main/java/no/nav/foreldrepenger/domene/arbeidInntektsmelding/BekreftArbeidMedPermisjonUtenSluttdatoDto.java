package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_PERMISJON_UTEN_SLUTTDATO_KODE)
public class BekreftArbeidMedPermisjonUtenSluttdatoDto extends BekreftetAksjonspunktDto {
    @Valid
    @Size(max = 1000)
    @NotNull
    @Size(min = 1, max = 10)
    private List<AvklarPermisjonUtenSluttdatoDto> arbeidsforhold;

    @SuppressWarnings("unused") // NOSONAR
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

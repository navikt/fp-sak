package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD_KODE)
public class FastsettBGTidsbegrensetArbeidsforholdDto extends BekreftetAksjonspunktDto {


    @Valid
    @Size(max = 100)
    private List<FastsattePerioderTidsbegrensetDto> fastsatteTidsbegrensedePerioder;

    @Min(0)
    @Max(Long.MAX_VALUE)
    private Integer frilansInntekt;


    FastsettBGTidsbegrensetArbeidsforholdDto() {
        // For Jackson
    }

    public FastsettBGTidsbegrensetArbeidsforholdDto(String begrunnelse, List<FastsattePerioderTidsbegrensetDto> fastsatteTidsbegrensedePerioder, Integer frilansInntekt) { // NOSONAR
        super(begrunnelse);
        this.fastsatteTidsbegrensedePerioder = new ArrayList<>(fastsatteTidsbegrensedePerioder);
        this.frilansInntekt = frilansInntekt;
    }

    public List<FastsattePerioderTidsbegrensetDto> getFastsatteTidsbegrensedePerioder() {
        return fastsatteTidsbegrensedePerioder;
    }

    public Integer getFrilansInntekt() {
        return frilansInntekt;
    }
}

package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

@JsonTypeName(AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS_KODE)
public class FastsettBeregningsgrunnlagATFLDto extends BekreftetAksjonspunktDto {


    @Valid
    @Size(max = 100)
    private List<InntektPrAndelDto> inntektPrAndelList;

    @Min(0)
    @Max(100 * 1000 * 1000)
    private Integer inntektFrilanser;

    @Valid
    @Size(max = 100)
    private List<FastsattePerioderTidsbegrensetDto> fastsatteTidsbegrensedePerioder;


    FastsettBeregningsgrunnlagATFLDto() {
        // For Jackson
    }


    public FastsettBeregningsgrunnlagATFLDto(String begrunnelse, List<InntektPrAndelDto> inntektPrAndelList, Integer inntektFrilanser) { // NOSONAR
        super(begrunnelse);
        this.inntektPrAndelList = new ArrayList<>(inntektPrAndelList);
        this.inntektFrilanser = inntektFrilanser;
    }

    public FastsettBeregningsgrunnlagATFLDto(String begrunnelse, Integer inntektFrilanser) { // NOSONAR
        super(begrunnelse);
        this.inntektFrilanser = inntektFrilanser;
    }


    public Integer getInntektFrilanser() {
        return inntektFrilanser;
    }

    public List<InntektPrAndelDto> getInntektPrAndelList() {
        return inntektPrAndelList;
    }

    public List<FastsattePerioderTidsbegrensetDto> getFastsatteTidsbegrensedePerioder() {
        return fastsatteTidsbegrensedePerioder;
    }

    public boolean tidsbegrensetInntektErFastsatt() {
        return fastsatteTidsbegrensedePerioder != null && !fastsatteTidsbegrensedePerioder.isEmpty();
    }

}

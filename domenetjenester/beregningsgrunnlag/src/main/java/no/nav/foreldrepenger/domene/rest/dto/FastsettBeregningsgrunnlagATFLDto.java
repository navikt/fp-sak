package no.nav.foreldrepenger.domene.rest.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS_KODE)
public class FastsettBeregningsgrunnlagATFLDto extends BekreftetAksjonspunktDto {


    @Size(max = 100)
    private List<@Valid InntektPrAndelDto> inntektPrAndelList;

    @Min(0)
    @Max(100 * 1000 * 1000)
    private Integer inntektFrilanser;

    FastsettBeregningsgrunnlagATFLDto() {
        // For Jackson
    }


    public FastsettBeregningsgrunnlagATFLDto(String begrunnelse, List<InntektPrAndelDto> inntektPrAndelList, Integer inntektFrilanser) {
        super(begrunnelse);
        this.inntektPrAndelList = new ArrayList<>(inntektPrAndelList);
        this.inntektFrilanser = inntektFrilanser;
    }

    public FastsettBeregningsgrunnlagATFLDto(String begrunnelse, Integer inntektFrilanser) {
        super(begrunnelse);
        this.inntektFrilanser = inntektFrilanser;
    }


    public Integer getInntektFrilanser() {
        return inntektFrilanser;
    }

    public List<InntektPrAndelDto> getInntektPrAndelList() {
        return inntektPrAndelList;
    }
}

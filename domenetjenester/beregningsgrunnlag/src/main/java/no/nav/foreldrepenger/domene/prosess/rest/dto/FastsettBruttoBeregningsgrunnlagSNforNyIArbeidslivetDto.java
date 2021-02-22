package no.nav.foreldrepenger.domene.prosess.rest.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_FOR_SN_NY_I_ARBEIDSLIVET_KODE)
public class FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto extends BekreftetAksjonspunktDto {


    @Min(0)
    @Max(Integer.MAX_VALUE)
    @NotNull
    private Integer bruttoBeregningsgrunnlag;

    FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto() {
        // For Jackson
    }

    public FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto(String begrunnelse, Integer bruttoBeregningsgrunnlag) {
        super(begrunnelse);
        this.bruttoBeregningsgrunnlag = bruttoBeregningsgrunnlag;
    }


    public Integer getBruttoBeregningsgrunnlag() {
        return bruttoBeregningsgrunnlag;
    }
}

package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_SELVSTENDIG_NÆRINGSDRIVENDE_KODE)
public class FastsettBruttoBeregningsgrunnlagSNDto extends BekreftetAksjonspunktDto {


    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer bruttoBeregningsgrunnlag;

    FastsettBruttoBeregningsgrunnlagSNDto() {
        // For Jackson
    }

    public FastsettBruttoBeregningsgrunnlagSNDto(String begrunnelse, Integer bruttoBeregningsgrunnlag) {
        super(begrunnelse);
        this.bruttoBeregningsgrunnlag = bruttoBeregningsgrunnlag;
    }



    public Integer getBruttoBeregningsgrunnlag() {
        return bruttoBeregningsgrunnlag;
    }
}

package no.nav.foreldrepenger.domene.rest.dto.fordeling;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.FORDEL_BEREGNINGSGRUNNLAG_KODE)
public class FordelBeregningsgrunnlagDto extends BekreftetAksjonspunktDto {


    @Size(max = 100)
    private List<@Valid FordelBeregningsgrunnlagPeriodeDto> endretBeregningsgrunnlagPerioder;

    FordelBeregningsgrunnlagDto() {
        // Jackson
    }

    public FordelBeregningsgrunnlagDto(List<FordelBeregningsgrunnlagPeriodeDto> endretBeregningsgrunnlagPerioder, String begrunnelse) {
        super(begrunnelse);
        this.endretBeregningsgrunnlagPerioder = endretBeregningsgrunnlagPerioder;
    }


    public List<FordelBeregningsgrunnlagPeriodeDto> getEndretBeregningsgrunnlagPerioder() {
        return endretBeregningsgrunnlagPerioder;
    }
}

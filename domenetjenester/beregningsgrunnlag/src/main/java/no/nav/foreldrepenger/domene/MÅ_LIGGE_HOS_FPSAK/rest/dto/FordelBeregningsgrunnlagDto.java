package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.FORDEL_BEREGNINGSGRUNNLAG_KODE)
public class FordelBeregningsgrunnlagDto extends BekreftetAksjonspunktDto {


    @Valid
    @Size(max = 100)
    private List<FastsettBeregningsgrunnlagPeriodeDto> endretBeregningsgrunnlagPerioder;

    FordelBeregningsgrunnlagDto() { // NOSONAR
        // Jackson
    }

    public FordelBeregningsgrunnlagDto(List<FastsettBeregningsgrunnlagPeriodeDto> endretBeregningsgrunnlagPerioder, String begrunnelse) { // NOSONAR
        super(begrunnelse);
        this.endretBeregningsgrunnlagPerioder = endretBeregningsgrunnlagPerioder;
    }


    public List<FastsettBeregningsgrunnlagPeriodeDto> getEndretBeregningsgrunnlagPerioder() {
        return endretBeregningsgrunnlagPerioder;
    }
}

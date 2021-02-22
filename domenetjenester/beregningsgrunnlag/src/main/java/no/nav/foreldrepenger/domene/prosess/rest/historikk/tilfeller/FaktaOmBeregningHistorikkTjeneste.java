package no.nav.foreldrepenger.domene.prosess.rest.historikk.tilfeller;

import java.util.Optional;

import no.nav.foreldrepenger.domene.prosess.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

public abstract class FaktaOmBeregningHistorikkTjeneste {

    public abstract void lagHistorikk(Long behandlingId, FaktaBeregningLagreDto dto, HistorikkInnslagTekstBuilder tekstBuilder, BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag, InntektArbeidYtelseGrunnlag iayGrunnlag);

}

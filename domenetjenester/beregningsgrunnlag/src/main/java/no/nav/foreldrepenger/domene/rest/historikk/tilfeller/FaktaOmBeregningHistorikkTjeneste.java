package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;

import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;

public abstract class FaktaOmBeregningHistorikkTjeneste {

    public abstract List<HistorikkinnslagTekstlinjeBuilder> lagHistorikk(FaktaBeregningLagreDto dto,
                                                                         BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                                                                         Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag,
                                                                         InntektArbeidYtelseGrunnlag iayGrunnlag);

}

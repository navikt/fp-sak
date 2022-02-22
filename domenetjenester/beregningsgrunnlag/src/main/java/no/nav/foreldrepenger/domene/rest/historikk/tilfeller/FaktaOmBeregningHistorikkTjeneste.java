package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;

import no.nav.foreldrepenger.domene.oppdateringresultat.OppdaterBeregningsgrunnlagResultat;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

public abstract class FaktaOmBeregningHistorikkTjeneste {

    public abstract void lagHistorikk(Long behandlingId,
                                      OppdaterBeregningsgrunnlagResultat oppdaterResultat,
                                      FaktaBeregningLagreDto dto,
                                      HistorikkInnslagTekstBuilder tekstBuilder, InntektArbeidYtelseGrunnlag iayGrunnlag);

}

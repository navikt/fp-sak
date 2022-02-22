package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.oppdateringresultat.FaktaOmBeregningVurderinger;
import no.nav.foreldrepenger.domene.oppdateringresultat.OppdaterBeregningsgrunnlagResultat;
import no.nav.foreldrepenger.domene.oppdateringresultat.ToggleEndring;
import no.nav.foreldrepenger.domene.rest.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef("VURDER_MILITÆR_SIVILTJENESTE")
public class VurderMilitærHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    @Override
    public void lagHistorikk(Long behandlingId,
                             OppdaterBeregningsgrunnlagResultat oppdaterResultat,
                             FaktaBeregningLagreDto dto,
                             HistorikkInnslagTekstBuilder tekstBuilder,
                             InntektArbeidYtelseGrunnlag iayGrunnlag) {
        oppdaterResultat.getFaktaOmBeregningVurderinger()
            .flatMap(FaktaOmBeregningVurderinger::getHarMilitærSiviltjenesteEndring)
            .ifPresent(e -> lagHistorikkInnslag(e, tekstBuilder));
    }

    private void lagHistorikkInnslag(ToggleEndring verdiEndring, HistorikkInnslagTekstBuilder tekstBuilder) {
        if (verdiEndring.erEndring()) {
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.MILITÆR_ELLER_SIVIL, verdiEndring.getFraVerdi(), verdiEndring.getTilVerdi());
        }
    }

}

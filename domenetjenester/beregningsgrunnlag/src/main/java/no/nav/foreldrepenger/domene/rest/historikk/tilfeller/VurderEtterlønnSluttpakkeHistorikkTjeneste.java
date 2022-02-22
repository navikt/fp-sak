package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.oppdateringresultat.FaktaOmBeregningVurderinger;
import no.nav.foreldrepenger.domene.oppdateringresultat.OppdaterBeregningsgrunnlagResultat;
import no.nav.foreldrepenger.domene.oppdateringresultat.ToggleEndring;
import no.nav.foreldrepenger.domene.rest.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef("VURDER_ETTERLØNN_SLUTTPAKKE")
public class VurderEtterlønnSluttpakkeHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    @Override
    public void lagHistorikk(Long behandlingId,
                             OppdaterBeregningsgrunnlagResultat oppdaterResultat,
                             FaktaBeregningLagreDto dto,
                             HistorikkInnslagTekstBuilder tekstBuilder,
                             InntektArbeidYtelseGrunnlag iayGrunnlag) {
        if (dto.getFaktaOmBeregningTilfeller().contains(FaktaOmBeregningTilfelle.VURDER_ETTERLØNN_SLUTTPAKKE)) {
            lagHistorikkForVurdering(tekstBuilder, oppdaterResultat);
        }
    }

    private void lagHistorikkForVurdering(HistorikkInnslagTekstBuilder tekstBuilder, OppdaterBeregningsgrunnlagResultat oppdaterResultat) {
        var etterlønnSluttpakkeEndring = oppdaterResultat.getFaktaOmBeregningVurderinger()
            .flatMap(FaktaOmBeregningVurderinger::getHarEtterlønnSluttpakkeEndring);
        etterlønnSluttpakkeEndring.ifPresent(e -> lagHistorikkinnslagVurderEtterlønnSluttpakke(e, tekstBuilder));
    }

    private void lagHistorikkinnslagVurderEtterlønnSluttpakke(ToggleEndring verdiEndring, HistorikkInnslagTekstBuilder tekstBuilder) {
        if (verdiEndring.erEndring()) {
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.VURDER_ETTERLØNN_SLUTTPAKKE, verdiEndring.getFraVerdi(), verdiEndring.getTilVerdi());
        }
    }


}

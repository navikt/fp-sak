package no.nav.foreldrepenger.domene.rest.historikk.kalkulus;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeregningsgrunnlagPeriodeEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.OppdaterBeregningsgrunnlagResultat;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

/**
 * Lager historikk for aksjonspunkter løst i fakta om beregning.
 */
@Dependent
public class FaktaBeregningHistorikkKalkulusTjeneste {

    private HistorikkTjenesteAdapter historikkAdapter;
    private FaktaOmBeregningVurderingHistorikkTjeneste vurderingHistorikkTjeneste;
    private BeregningsgrunnlagVerdierHistorikkTjeneste verdierHistorikkTjeneste;

    public FaktaBeregningHistorikkKalkulusTjeneste() {
        // CDI
    }

    @Inject
    public FaktaBeregningHistorikkKalkulusTjeneste(HistorikkTjenesteAdapter historikkAdapter,
                                                     FaktaOmBeregningVurderingHistorikkTjeneste vurderingHistorikkTjeneste,
                                                     BeregningsgrunnlagVerdierHistorikkTjeneste verdierHistorikkTjeneste) {
        this.historikkAdapter = historikkAdapter;
        this.vurderingHistorikkTjeneste = vurderingHistorikkTjeneste;
        this.verdierHistorikkTjeneste = verdierHistorikkTjeneste;
    }


    public void lagHistorikk(Long behandlingId, OppdaterBeregningsgrunnlagResultat oppdatering, String begrunnelse) {
        HistorikkInnslagTekstBuilder tekstBuilder = historikkAdapter.tekstBuilder();
        byggHistorikkForEndring(behandlingId, oppdatering, tekstBuilder);

        if (tekstBuilder.antallEndredeFelter() > 0) {
            tekstBuilder.medSkjermlenke(SkjermlenkeType.FAKTA_OM_BEREGNING).medBegrunnelse(begrunnelse);
            historikkAdapter.opprettHistorikkInnslag(behandlingId, HistorikkinnslagType.FAKTA_ENDRET);
        }
    }

    private void byggHistorikkForEndring(Long behandlingId, OppdaterBeregningsgrunnlagResultat oppdaterBeregningsgrunnlagResultat, HistorikkInnslagTekstBuilder tekstBuilder) {
        oppdaterBeregningsgrunnlagResultat.getFaktaOmBeregningVurderinger()
            .ifPresent(vurderinger -> vurderingHistorikkTjeneste.lagHistorikkForVurderinger(behandlingId, tekstBuilder, vurderinger));
        oppdaterBeregningsgrunnlagResultat.getBeregningsgrunnlagEndring()
            .ifPresent(endring -> {
                BeregningsgrunnlagPeriodeEndring førstePeriode = endring.getBeregningsgrunnlagPeriodeEndringer().get(0);
                verdierHistorikkTjeneste.lagHistorikkForBeregningsgrunnlagVerdier(behandlingId, førstePeriode, tekstBuilder);
            });
    }

}

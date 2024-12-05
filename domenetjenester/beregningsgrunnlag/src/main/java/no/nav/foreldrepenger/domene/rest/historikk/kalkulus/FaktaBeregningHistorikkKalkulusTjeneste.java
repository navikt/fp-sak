package no.nav.foreldrepenger.domene.rest.historikk.kalkulus;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.aksjonspunkt.OppdaterBeregningsgrunnlagResultat;

/**
 * Lager historikk for aksjonspunkter løst i fakta om beregning.
 */
@Dependent
public class FaktaBeregningHistorikkKalkulusTjeneste {

    private Historikkinnslag2Repository historikkinnslagRepository;
    private FaktaOmBeregningVurderingHistorikkTjeneste vurderingHistorikkTjeneste;
    private BeregningsgrunnlagVerdierHistorikkTjeneste verdierHistorikkTjeneste;

    public FaktaBeregningHistorikkKalkulusTjeneste() {
        // CDI
    }

    @Inject
    public FaktaBeregningHistorikkKalkulusTjeneste(Historikkinnslag2Repository historikkinnslagRepository,
                                                    FaktaOmBeregningVurderingHistorikkTjeneste vurderingHistorikkTjeneste,
                                                    BeregningsgrunnlagVerdierHistorikkTjeneste verdierHistorikkTjeneste) {
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.vurderingHistorikkTjeneste = vurderingHistorikkTjeneste;
        this.verdierHistorikkTjeneste = verdierHistorikkTjeneste;
    }


    public void lagHistorikk(BehandlingReferanse behandlingReferanse, OppdaterBeregningsgrunnlagResultat oppdatering, String begrunnelse) {
        var linjer = byggHistorikkForEndring(behandlingReferanse.behandlingId(), oppdatering);
        if (linjer.isEmpty()) {
            return;
        }

        var historikkinnslag = new Historikkinnslag2.Builder()
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(behandlingReferanse.fagsakId())
            .medBehandlingId(behandlingReferanse.behandlingId())
            .medTittel(SkjermlenkeType.FAKTA_OM_BEREGNING)
            .medLinjer(linjer)
            .addLinje(begrunnelse)
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

    private List<HistorikkinnslagLinjeBuilder> byggHistorikkForEndring(Long behandlingId, OppdaterBeregningsgrunnlagResultat oppdaterBeregningsgrunnlagResultat) {
        var linjer = new ArrayList<HistorikkinnslagLinjeBuilder>();
        oppdaterBeregningsgrunnlagResultat.getFaktaOmBeregningVurderinger().ifPresent(vurderinger ->
            linjer.addAll(vurderingHistorikkTjeneste.lagHistorikkForVurderinger(behandlingId, vurderinger)));
        oppdaterBeregningsgrunnlagResultat.getBeregningsgrunnlagEndring()
            .ifPresent(endring -> {
                var førstePeriode = endring.getBeregningsgrunnlagPeriodeEndringer().getFirst();
                linjer.addAll(verdierHistorikkTjeneste.lagHistorikkForBeregningsgrunnlagVerdier(behandlingId, førstePeriode));
            });
        return linjer;
    }

}

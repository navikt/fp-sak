package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AbstractOverstyringshåndterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.rest.dto.OverstyrBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.rest.historikk.FaktaBeregningHistorikkHåndterer;
import no.nav.foreldrepenger.domene.rest.historikk.kalkulus.FaktaBeregningHistorikkKalkulusTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyrBeregningsgrunnlagDto.class, adapter = Overstyringshåndterer.class)
public class BeregningsgrunnlagOverstyringshåndterer extends AbstractOverstyringshåndterer<OverstyrBeregningsgrunnlagDto> {

    private FaktaBeregningHistorikkHåndterer faktaBeregningHistorikkHåndterer;
    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private BeregningTjeneste beregningTjeneste;
    private FaktaBeregningHistorikkKalkulusTjeneste faktaBeregningHistorikkKalkulusTjeneste;

    BeregningsgrunnlagOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public BeregningsgrunnlagOverstyringshåndterer(HistorikkTjenesteAdapter historikkAdapter,
                                                   FaktaBeregningHistorikkHåndterer faktaBeregningHistorikkHåndterer,
                                                   FaktaBeregningHistorikkKalkulusTjeneste faktaBeregningHistorikkKalkulusTjeneste,
                                                   HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                                   BeregningTjeneste beregningTjeneste) {
        super(historikkAdapter, AksjonspunktDefinisjon.OVERSTYRING_AV_BEREGNINGSGRUNNLAG);
        this.faktaBeregningHistorikkHåndterer = faktaBeregningHistorikkHåndterer;
        this.faktaBeregningHistorikkKalkulusTjeneste = faktaBeregningHistorikkKalkulusTjeneste;
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.beregningTjeneste = beregningTjeneste;
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyrBeregningsgrunnlagDto dto,
                                                  Behandling behandling, BehandlingskontrollKontekst kontekst) {
        var ref = BehandlingReferanse.fra(behandling);
        var endringsaggregat = beregningTjeneste.overstyrBeregning(dto, ref);
        if (endringsaggregat.isPresent()) {
            faktaBeregningHistorikkKalkulusTjeneste.lagHistorikk(ref.behandlingId(), endringsaggregat.get(), dto.getBegrunnelse());
        } else {
            var forrigeGrunnlag = beregningsgrunnlagTjeneste.hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(behandling.getId(), behandling.getOriginalBehandlingId(),
                BeregningsgrunnlagTilstand.FORESLÅTT);
            var aktivtGrunnlag = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetAggregatForBehandling(behandling.getId());
            faktaBeregningHistorikkHåndterer.lagHistorikkOverstyringInntekt(behandling, dto, aktivtGrunnlag, forrigeGrunnlag);
        }
        var builder = OppdateringResultat.utenTransisjon();
        fjernOverstyrtAksjonspunkt(behandling)
            .ifPresent(ap -> builder.medEkstraAksjonspunktResultat(ap.getAksjonspunktDefinisjon(), AksjonspunktStatus.AVBRUTT));
        return builder.build();
    }

    private Optional<Aksjonspunkt> fjernOverstyrtAksjonspunkt(Behandling behandling) {
        return behandling.getÅpentAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VURDER_FAKTA_FOR_ATFL_SN);
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyrBeregningsgrunnlagDto dto) {
        // Håndteres sammen med selve overstyringen
    }
}

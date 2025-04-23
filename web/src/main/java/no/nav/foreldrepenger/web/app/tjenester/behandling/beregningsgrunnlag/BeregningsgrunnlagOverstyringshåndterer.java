package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.rest.dto.OverstyrBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.rest.historikk.FaktaBeregningHistorikkHåndterer;
import no.nav.foreldrepenger.domene.rest.historikk.kalkulus.FaktaBeregningHistorikkKalkulusTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyrBeregningsgrunnlagDto.class, adapter = Overstyringshåndterer.class)
public class BeregningsgrunnlagOverstyringshåndterer implements Overstyringshåndterer<OverstyrBeregningsgrunnlagDto> {

    private FaktaBeregningHistorikkHåndterer faktaBeregningHistorikkHåndterer;
    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private BeregningTjeneste beregningTjeneste;
    private FaktaBeregningHistorikkKalkulusTjeneste faktaBeregningHistorikkKalkulusTjeneste;
    private BehandlingRepository behandlingRepository;

    BeregningsgrunnlagOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public BeregningsgrunnlagOverstyringshåndterer(FaktaBeregningHistorikkHåndterer faktaBeregningHistorikkHåndterer,
                                                   FaktaBeregningHistorikkKalkulusTjeneste faktaBeregningHistorikkKalkulusTjeneste,
                                                   HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                                   BeregningTjeneste beregningTjeneste, BehandlingRepository behandlingRepository) {
        this.faktaBeregningHistorikkHåndterer = faktaBeregningHistorikkHåndterer;
        this.faktaBeregningHistorikkKalkulusTjeneste = faktaBeregningHistorikkKalkulusTjeneste;
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.beregningTjeneste = beregningTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyrBeregningsgrunnlagDto dto, BehandlingReferanse ref) {
        var endringsaggregat = beregningTjeneste.overstyrBeregning(dto, ref);
        if (endringsaggregat.isPresent()) {
            faktaBeregningHistorikkKalkulusTjeneste.lagHistorikk(ref, endringsaggregat.get(), dto.getBegrunnelse());
        } else {
            var forrigeGrunnlag = beregningsgrunnlagTjeneste.hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(ref.behandlingId(), ref.getOriginalBehandlingId(),
                BeregningsgrunnlagTilstand.FORESLÅTT);
            var aktivtGrunnlag = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetAggregatForBehandling(ref.behandlingId());
            faktaBeregningHistorikkHåndterer.lagHistorikkOverstyringInntekt(ref, dto, aktivtGrunnlag, forrigeGrunnlag);
        }
        var builder = OppdateringResultat.utenTransisjon();
        fjernOverstyrtAksjonspunkt(ref)
            .ifPresent(ap -> builder.medEkstraAksjonspunktResultat(ap.getAksjonspunktDefinisjon(), AksjonspunktStatus.AVBRUTT));
        return builder.build();
    }

    private Optional<Aksjonspunkt> fjernOverstyrtAksjonspunkt(BehandlingReferanse ref) {
        return behandlingRepository.hentBehandling(ref.behandlingId())
            .getÅpentAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VURDER_FAKTA_FOR_ATFL_SN);
    }
}

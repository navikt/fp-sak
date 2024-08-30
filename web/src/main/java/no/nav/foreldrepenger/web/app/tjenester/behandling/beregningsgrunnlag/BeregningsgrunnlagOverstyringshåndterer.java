package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AbstractOverstyringshåndterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.domene.mappers.til_kalkulator.BeregningsgrunnlagInputProvider;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.domene.mappers.til_kalkulator.OppdatererDtoMapper;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.rest.BeregningHåndterer;
import no.nav.foreldrepenger.domene.rest.dto.OverstyrBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.rest.historikk.FaktaBeregningHistorikkHåndterer;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyrBeregningsgrunnlagDto.class, adapter = Overstyringshåndterer.class)
public class BeregningsgrunnlagOverstyringshåndterer extends AbstractOverstyringshåndterer<OverstyrBeregningsgrunnlagDto> {

    private FaktaBeregningHistorikkHåndterer faktaBeregningHistorikkHåndterer;
    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputTjeneste;
    private BeregningHåndterer beregningHåndterer;

    BeregningsgrunnlagOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public BeregningsgrunnlagOverstyringshåndterer(HistorikkTjenesteAdapter historikkAdapter,
                                                   FaktaBeregningHistorikkHåndterer faktaBeregningHistorikkHåndterer,
                                                   HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                                   BeregningsgrunnlagInputProvider beregningsgrunnlagInputTjeneste,
                                                   BeregningHåndterer beregningHåndterer) {
        super(historikkAdapter, AksjonspunktDefinisjon.OVERSTYRING_AV_BEREGNINGSGRUNNLAG);
        this.faktaBeregningHistorikkHåndterer = faktaBeregningHistorikkHåndterer;
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.beregningsgrunnlagInputTjeneste = beregningsgrunnlagInputTjeneste;
        this.beregningHåndterer = beregningHåndterer;
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyrBeregningsgrunnlagDto dto,
                                                  Behandling behandling, BehandlingskontrollKontekst kontekst) {
        var tjeneste = beregningsgrunnlagInputTjeneste.getTjeneste(behandling.getFagsakYtelseType());
        var ref = BehandlingReferanse.fra(behandling);
        var input = tjeneste.lagInput(ref);
        beregningHåndterer.håndterBeregningsgrunnlagOverstyring(input, OppdatererDtoMapper.mapOverstyrBeregningsgrunnlagDto(dto));

        lagHistorikk(ref, dto);

        var builder = OppdateringResultat.utenTransisjon();
        fjernOverstyrtAksjonspunkt(behandling)
            .ifPresent(ap -> builder.medEkstraAksjonspunktResultat(ap.getAksjonspunktDefinisjon(), AksjonspunktStatus.AVBRUTT));
        return builder.build();
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyrBeregningsgrunnlagDto dto) {
        // Håndteres sammen med overstyringen
    }

    private Optional<Aksjonspunkt> fjernOverstyrtAksjonspunkt(Behandling behandling) {
        return behandling.getÅpentAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VURDER_FAKTA_FOR_ATFL_SN);
    }

    private void lagHistorikk(BehandlingReferanse referanse, OverstyrBeregningsgrunnlagDto dto) {
        var forrigeGrunnlag = beregningsgrunnlagTjeneste.hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(referanse.behandlingId(), referanse.getOriginalBehandlingId(),
            BeregningsgrunnlagTilstand.FORESLÅTT);
        var aktivtGrunnlag = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetAggregatForBehandling(referanse.behandlingId());
        faktaBeregningHistorikkHåndterer.lagHistorikkOverstyringInntekt(referanse, dto, aktivtGrunnlag, forrigeGrunnlag);
    }

}

package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.domene.mappers.til_kalkulator.BeregningsgrunnlagInputProvider;
import no.nav.foreldrepenger.domene.mappers.til_kalkulator.OppdatererDtoMapper;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.rest.BeregningHåndterer;
import no.nav.foreldrepenger.domene.rest.dto.VurderRefusjonBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.rest.historikk.VurderRefusjonBeregningsgrunnlagHistorikkTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderRefusjonBeregningsgrunnlagDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderRefusjonBeregningsgrunnlagOppdaterer implements AksjonspunktOppdaterer<VurderRefusjonBeregningsgrunnlagDto> {

    private VurderRefusjonBeregningsgrunnlagHistorikkTjeneste vurderRefusjonBeregningsgrunnlagHistorikkTjeneste;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputTjeneste;
    private BeregningHåndterer beregningHåndterer;
    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;

    VurderRefusjonBeregningsgrunnlagOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderRefusjonBeregningsgrunnlagOppdaterer(VurderRefusjonBeregningsgrunnlagHistorikkTjeneste vurderRefusjonBeregningsgrunnlagHistorikkTjeneste,
                                                      BeregningsgrunnlagInputProvider beregningsgrunnlagInputTjeneste,
                                                      BeregningHåndterer beregningHåndterer,
                                                      HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste) {
        this.vurderRefusjonBeregningsgrunnlagHistorikkTjeneste = vurderRefusjonBeregningsgrunnlagHistorikkTjeneste;
        this.beregningsgrunnlagInputTjeneste = beregningsgrunnlagInputTjeneste;
        this.beregningHåndterer = beregningHåndterer;
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(VurderRefusjonBeregningsgrunnlagDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingRef = param.getRef();
        var tjeneste = beregningsgrunnlagInputTjeneste.getTjeneste(behandlingRef.fagsakYtelseType());
        var input = tjeneste.lagInput(behandlingRef.behandlingId());
        var forrigeGrunnlag = beregningsgrunnlagTjeneste.hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(behandlingRef.behandlingId(),
            behandlingRef.getOriginalBehandlingId(), BeregningsgrunnlagTilstand.VURDERT_REFUSJON_UT);
        beregningHåndterer.håndterVurderRefusjonBeregningsgrunnlag(input, OppdatererDtoMapper.mapVurderRefusjonBeregningsgrunnlag(dto));
        vurderRefusjonBeregningsgrunnlagHistorikkTjeneste.lagHistorikk(dto, param, forrigeGrunnlag);
        return OppdateringResultat.utenOverhopp();
    }

}

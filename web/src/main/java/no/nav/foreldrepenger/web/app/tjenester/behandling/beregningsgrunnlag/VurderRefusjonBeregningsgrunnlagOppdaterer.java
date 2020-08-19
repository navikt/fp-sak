package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputFelles;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputProvider;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus.OppdatererDtoMapper;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.BeregningHåndterer;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.VurderRefusjonBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.historikk.VurderRefusjonBeregningsgrunnlagHistorikkTjeneste;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Optional;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderRefusjonBeregningsgrunnlagDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderRefusjonBeregningsgrunnlagOppdaterer implements AksjonspunktOppdaterer<VurderRefusjonBeregningsgrunnlagDto>  {

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
        BehandlingReferanse behandlingRef = param.getRef();
        BeregningsgrunnlagInputFelles tjeneste = beregningsgrunnlagInputTjeneste.getTjeneste(behandlingRef.getFagsakYtelseType());
        BeregningsgrunnlagInput input = tjeneste.lagInput(behandlingRef.getBehandlingId());
        Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag = beregningsgrunnlagTjeneste
            .hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(
                behandlingRef.getBehandlingId(),
                behandlingRef.getOriginalBehandlingId(),
                BeregningsgrunnlagTilstand.VURDERT_REFUSJON_UT);
        beregningHåndterer.håndterVurderRefusjonBeregningsgrunnlag(input, OppdatererDtoMapper.mapVurderRefusjonBeregningsgrunnlag(dto));
        vurderRefusjonBeregningsgrunnlagHistorikkTjeneste.lagHistorikk(dto, param, forrigeGrunnlag);
        return OppdateringResultat.utenOveropp();
    }

}

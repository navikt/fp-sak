package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.rest.dto.VurderRefusjonBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.rest.historikk.VurderRefusjonBeregningsgrunnlagHistorikkTjeneste;
import no.nav.foreldrepenger.domene.rest.historikk.kalkulus.VurderRefusjonBeregningsgrunnlagHistorikkKalkulusTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderRefusjonBeregningsgrunnlagDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderRefusjonBeregningsgrunnlagOppdaterer implements AksjonspunktOppdaterer<VurderRefusjonBeregningsgrunnlagDto>  {

    private VurderRefusjonBeregningsgrunnlagHistorikkTjeneste vurderRefusjonBeregningsgrunnlagHistorikkTjeneste;
    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private VurderRefusjonBeregningsgrunnlagHistorikkKalkulusTjeneste vurderRefusjonBeregningsgrunnlagHistorikkKalkulusTjeneste;
    private BeregningTjeneste beregningTjeneste;

    VurderRefusjonBeregningsgrunnlagOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderRefusjonBeregningsgrunnlagOppdaterer(VurderRefusjonBeregningsgrunnlagHistorikkTjeneste vurderRefusjonBeregningsgrunnlagHistorikkTjeneste,
                                                      HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                                      VurderRefusjonBeregningsgrunnlagHistorikkKalkulusTjeneste vurderRefusjonBeregningsgrunnlagHistorikkKalkulusTjeneste,
                                                      BeregningTjeneste beregningTjeneste) {
        this.vurderRefusjonBeregningsgrunnlagHistorikkTjeneste = vurderRefusjonBeregningsgrunnlagHistorikkTjeneste;
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.vurderRefusjonBeregningsgrunnlagHistorikkKalkulusTjeneste = vurderRefusjonBeregningsgrunnlagHistorikkKalkulusTjeneste;
        this.beregningTjeneste = beregningTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(VurderRefusjonBeregningsgrunnlagDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingRef = param.getRef();
        var forrigeGrunnlag = beregningsgrunnlagTjeneste.hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(
            param.getRef().behandlingId(), param.getRef().getOriginalBehandlingId(), BeregningsgrunnlagTilstand.VURDERT_REFUSJON_UT);
        var endringsaggregat = beregningTjeneste.oppdaterBeregning(dto, behandlingRef);
        if (endringsaggregat.isPresent()) {
            vurderRefusjonBeregningsgrunnlagHistorikkKalkulusTjeneste.lagHistorikk(dto, param, endringsaggregat.get());
        } else {
            vurderRefusjonBeregningsgrunnlagHistorikkTjeneste.lagHistorikk(dto, param, forrigeGrunnlag);
        }
        return OppdateringResultat.utenOverhopp();
    }

}

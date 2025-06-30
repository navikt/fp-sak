package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.rest.dto.VurderRefusjonBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.rest.historikk.VurderRefusjonBeregningsgrunnlagHistorikkKalkulusTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderRefusjonBeregningsgrunnlagDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderRefusjonBeregningsgrunnlagOppdaterer implements AksjonspunktOppdaterer<VurderRefusjonBeregningsgrunnlagDto>  {

    private VurderRefusjonBeregningsgrunnlagHistorikkKalkulusTjeneste vurderRefusjonBeregningsgrunnlagHistorikkKalkulusTjeneste;
    private BeregningTjeneste beregningTjeneste;

    VurderRefusjonBeregningsgrunnlagOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderRefusjonBeregningsgrunnlagOppdaterer(VurderRefusjonBeregningsgrunnlagHistorikkKalkulusTjeneste vurderRefusjonBeregningsgrunnlagHistorikkKalkulusTjeneste,
                                                      BeregningTjeneste beregningTjeneste) {
        this.vurderRefusjonBeregningsgrunnlagHistorikkKalkulusTjeneste = vurderRefusjonBeregningsgrunnlagHistorikkKalkulusTjeneste;
        this.beregningTjeneste = beregningTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(VurderRefusjonBeregningsgrunnlagDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingRef = param.getRef();
        var endringsaggregat = beregningTjeneste.oppdaterBeregning(dto, behandlingRef);
        endringsaggregat.ifPresent(
            oppdaterBeregningsgrunnlagResultat -> vurderRefusjonBeregningsgrunnlagHistorikkKalkulusTjeneste.lagHistorikk(dto, param,
                oppdaterBeregningsgrunnlagResultat));
        return OppdateringResultat.utenOverhopp();
    }

}

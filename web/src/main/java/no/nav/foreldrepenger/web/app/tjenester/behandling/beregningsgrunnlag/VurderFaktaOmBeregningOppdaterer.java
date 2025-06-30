package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.rest.dto.VurderFaktaOmBeregningDto;
import no.nav.foreldrepenger.domene.rest.historikk.FaktaBeregningHistorikkKalkulusTjeneste;


@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderFaktaOmBeregningDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderFaktaOmBeregningOppdaterer implements AksjonspunktOppdaterer<VurderFaktaOmBeregningDto> {

    private BeregningTjeneste beregningTjeneste;
    private FaktaBeregningHistorikkKalkulusTjeneste faktaOmBeregningHistorikkKalkulusTjeneste;

    VurderFaktaOmBeregningOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderFaktaOmBeregningOppdaterer(BeregningTjeneste beregningTjeneste,
                                            FaktaBeregningHistorikkKalkulusTjeneste faktaOmBeregningHistorikkKalkulusTjeneste)  {
        this.beregningTjeneste = beregningTjeneste;
        this.faktaOmBeregningHistorikkKalkulusTjeneste = faktaOmBeregningHistorikkKalkulusTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(VurderFaktaOmBeregningDto dto, AksjonspunktOppdaterParameter param) {
        var ref = param.getRef();
        var endringsaggregat = beregningTjeneste.oppdaterBeregning(dto, ref);
        endringsaggregat.ifPresent(
            oppdaterBeregningsgrunnlagResultat -> faktaOmBeregningHistorikkKalkulusTjeneste.lagHistorikk(ref, oppdaterBeregningsgrunnlagResultat,
                dto.getBegrunnelse()));
        return OppdateringResultat.utenOverhopp();
    }
}

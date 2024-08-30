package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.rest.dto.VurderFaktaOmBeregningDto;
import no.nav.foreldrepenger.domene.rest.historikk.FaktaBeregningHistorikkHåndterer;
import no.nav.foreldrepenger.domene.rest.historikk.kalkulus.FaktaBeregningHistorikkKalkulusTjeneste;


@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderFaktaOmBeregningDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderFaktaOmBeregningOppdaterer implements AksjonspunktOppdaterer<VurderFaktaOmBeregningDto> {

    private FaktaBeregningHistorikkHåndterer faktaBeregningHistorikkHåndterer;
    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private BeregningTjeneste beregningTjeneste;
    private FaktaBeregningHistorikkKalkulusTjeneste faktaOmBeregningHistorikkKalkulusTjeneste;

    VurderFaktaOmBeregningOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderFaktaOmBeregningOppdaterer(FaktaBeregningHistorikkHåndterer faktaBeregningHistorikkHåndterer,
                                            HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                            BeregningTjeneste beregningTjeneste,
                                            FaktaBeregningHistorikkKalkulusTjeneste faktaOmBeregningHistorikkKalkulusTjeneste)  {
        this.faktaBeregningHistorikkHåndterer = faktaBeregningHistorikkHåndterer;
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.beregningTjeneste = beregningTjeneste;
        this.faktaOmBeregningHistorikkKalkulusTjeneste = faktaOmBeregningHistorikkKalkulusTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(VurderFaktaOmBeregningDto dto, AksjonspunktOppdaterParameter param) {
        var forrigeGrunnlag = beregningsgrunnlagTjeneste
            .hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(
                param.getBehandlingId(),
                param.getRef().getOriginalBehandlingId(),
                BeregningsgrunnlagTilstand.KOFAKBER_UT);

        var endringsaggregat = beregningTjeneste.oppdaterBeregning(dto, param.getRef());
        if (endringsaggregat.isPresent()) {
            faktaOmBeregningHistorikkKalkulusTjeneste.lagHistorikk(param.getBehandlingId(), endringsaggregat.get(), dto.getBegrunnelse());
        } else {
            var nyttBeregningsgrunnlag = beregningsgrunnlagTjeneste
                .hentSisteBeregningsgrunnlagGrunnlagEntitet(param.getBehandlingId(), BeregningsgrunnlagTilstand.KOFAKBER_UT)
                .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag)
                .orElseThrow(() -> new IllegalStateException("Skal ha lagret beregningsgrunnlag fra KOFAKBER_UT."));
            faktaBeregningHistorikkHåndterer.lagHistorikk(param, dto, nyttBeregningsgrunnlag, forrigeGrunnlag);
        }
        return OppdateringResultat.utenOverhopp();
    }
}

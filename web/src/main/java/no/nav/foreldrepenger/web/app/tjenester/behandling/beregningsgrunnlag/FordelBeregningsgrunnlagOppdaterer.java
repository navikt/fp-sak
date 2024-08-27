package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.rest.dto.fordeling.FordelBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.rest.historikk.FordelBeregningsgrunnlagHistorikkTjeneste;
import no.nav.foreldrepenger.domene.rest.historikk.kalkulus.FordelBeregningsgrunnlagHistorikkKalkulusTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = FordelBeregningsgrunnlagDto.class, adapter = AksjonspunktOppdaterer.class)
public class FordelBeregningsgrunnlagOppdaterer implements AksjonspunktOppdaterer<FordelBeregningsgrunnlagDto>  {

    private FordelBeregningsgrunnlagHistorikkTjeneste fordelBeregningsgrunnlagHistorikkTjeneste;
    private FordelBeregningsgrunnlagHistorikkKalkulusTjeneste fordelBeregningsgrunnlagHistorikkKalkulusTjeneste;
    private BeregningTjeneste beregningTjeneste;

    FordelBeregningsgrunnlagOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public FordelBeregningsgrunnlagOppdaterer(FordelBeregningsgrunnlagHistorikkTjeneste fordelBeregningsgrunnlagHistorikkTjeneste,
                                              FordelBeregningsgrunnlagHistorikkKalkulusTjeneste fordelBeregningsgrunnlagHistorikkKalkulusTjeneste,
                                              BeregningTjeneste beregningTjeneste) {
        this.fordelBeregningsgrunnlagHistorikkTjeneste = fordelBeregningsgrunnlagHistorikkTjeneste;
        this.fordelBeregningsgrunnlagHistorikkKalkulusTjeneste = fordelBeregningsgrunnlagHistorikkKalkulusTjeneste;
        this.beregningTjeneste = beregningTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(FordelBeregningsgrunnlagDto dto, AksjonspunktOppdaterParameter param) {
        var endringsaggregat = beregningTjeneste.oppdaterBeregning(dto, param.getRef());
        if (endringsaggregat.isPresent()) {
            fordelBeregningsgrunnlagHistorikkKalkulusTjeneste.lagHistorikk(dto, endringsaggregat, param);
        } else {
            fordelBeregningsgrunnlagHistorikkTjeneste.lagHistorikk(dto, param);
        }
        return OppdateringResultat.utenOverhopp();
    }

}

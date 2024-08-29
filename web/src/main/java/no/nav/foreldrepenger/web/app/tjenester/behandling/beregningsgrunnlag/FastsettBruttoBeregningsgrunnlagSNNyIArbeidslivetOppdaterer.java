package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto;
import no.nav.foreldrepenger.domene.rest.historikk.FastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetHistorikkTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto.class, adapter = AksjonspunktOppdaterer.class)
public class FastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetOppdaterer implements AksjonspunktOppdaterer<FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto> {


    private FastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetHistorikkTjeneste fastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetHistorikkTjeneste;
    private BeregningTjeneste beregningTjeneste;

    FastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetOppdaterer() {
        // CDI
    }

    @Inject
    public FastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetOppdaterer(FastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetHistorikkTjeneste fastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetHistorikkTjeneste,
                                                                       BeregningTjeneste beregningTjeneste) {

        this.fastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetHistorikkTjeneste = fastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetHistorikkTjeneste;
        this.beregningTjeneste = beregningTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto dto, AksjonspunktOppdaterParameter param) {
        beregningTjeneste.oppdaterBeregning(dto, param.getRef());
        fastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetHistorikkTjeneste.lagHistorikk(dto, param);
        return OppdateringResultat.utenOverhopp();

    }
}

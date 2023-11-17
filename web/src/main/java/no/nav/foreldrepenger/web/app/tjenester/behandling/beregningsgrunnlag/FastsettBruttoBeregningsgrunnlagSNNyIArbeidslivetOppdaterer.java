package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputProvider;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.OppdatererDtoMapper;
import no.nav.foreldrepenger.domene.rest.BeregningHåndterer;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto;
import no.nav.foreldrepenger.domene.rest.historikk.FastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetHistorikkTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto.class, adapter = AksjonspunktOppdaterer.class)
public class FastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetOppdaterer implements AksjonspunktOppdaterer<FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto> {


    private FastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetHistorikkTjeneste fastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetHistorikkTjeneste;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputTjeneste;
    private BeregningHåndterer beregningHåndterer;

    FastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetOppdaterer() {
        // CDI
    }

    @Inject
    public FastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetOppdaterer(FastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetHistorikkTjeneste fastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetHistorikkTjeneste,
                                                                       BeregningsgrunnlagInputProvider beregningsgrunnlagInputTjeneste,
                                                                       BeregningHåndterer beregningHåndterer) {

        this.fastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetHistorikkTjeneste = fastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetHistorikkTjeneste;
        this.beregningsgrunnlagInputTjeneste = beregningsgrunnlagInputTjeneste;
        this.beregningHåndterer = beregningHåndterer;
    }

    @Override
    public OppdateringResultat oppdater(FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto dto, AksjonspunktOppdaterParameter param) {
        var tjeneste = beregningsgrunnlagInputTjeneste.getTjeneste(param.getRef().fagsakYtelseType());
        var input = tjeneste.lagInput(param.getRef().behandlingId());
        beregningHåndterer.håndterFastsettBruttoForSNNyIArbeidslivet(input, OppdatererDtoMapper.mapFastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto(dto));
        fastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetHistorikkTjeneste.lagHistorikk(dto, param);

        return OppdateringResultat.utenOverhopp();

    }
}

package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputFelles;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputProvider;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.OppdatererDtoMapper;
import no.nav.foreldrepenger.domene.rest.BeregningHåndterer;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBruttoBeregningsgrunnlagSNDto;
import no.nav.foreldrepenger.domene.rest.historikk.FastsettBruttoBeregningsgrunnlagSNHistorikkTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = FastsettBruttoBeregningsgrunnlagSNDto.class, adapter = AksjonspunktOppdaterer.class)
public class FastsettBruttoBeregningsgrunnlagSNOppdaterer implements AksjonspunktOppdaterer<FastsettBruttoBeregningsgrunnlagSNDto> {

    private FastsettBruttoBeregningsgrunnlagSNHistorikkTjeneste fastsettBruttoBeregningsgrunnlagSNHistorikkTjeneste;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputTjeneste;
    private BeregningHåndterer beregningHåndterer;

    FastsettBruttoBeregningsgrunnlagSNOppdaterer() {
        // CDI
    }

    @Inject
    public FastsettBruttoBeregningsgrunnlagSNOppdaterer(FastsettBruttoBeregningsgrunnlagSNHistorikkTjeneste fastsettBruttoBeregningsgrunnlagSNHistorikkTjeneste,
                                                        BeregningsgrunnlagInputProvider beregningsgrunnlagInputTjeneste,
                                                        BeregningHåndterer beregningHåndterer) {

        this.fastsettBruttoBeregningsgrunnlagSNHistorikkTjeneste = fastsettBruttoBeregningsgrunnlagSNHistorikkTjeneste;
        this.beregningsgrunnlagInputTjeneste = beregningsgrunnlagInputTjeneste;
        this.beregningHåndterer = beregningHåndterer;
    }

    @Override
    public OppdateringResultat oppdater(FastsettBruttoBeregningsgrunnlagSNDto dto, AksjonspunktOppdaterParameter param) {
        BeregningsgrunnlagInputFelles tjeneste = beregningsgrunnlagInputTjeneste.getTjeneste(param.getRef().getFagsakYtelseType());
        BeregningsgrunnlagInput input = tjeneste.lagInput(param.getRef().getBehandlingId());
        beregningHåndterer.håndterFastsettBeregningsgrunnlagSN(input, OppdatererDtoMapper.mapFastsettBruttoBeregningsgrunnlagSNDto(dto));
        fastsettBruttoBeregningsgrunnlagSNHistorikkTjeneste.lagHistorikk(param, dto);
        return OppdateringResultat.utenOveropp();
    }
}

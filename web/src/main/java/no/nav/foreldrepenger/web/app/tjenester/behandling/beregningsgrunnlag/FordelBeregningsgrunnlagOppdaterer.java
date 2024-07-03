package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.domene.mappers.til_kalkulator.BeregningsgrunnlagInputProvider;
import no.nav.foreldrepenger.domene.mappers.til_kalkulator.OppdatererDtoMapper;
import no.nav.foreldrepenger.domene.rest.BeregningHåndterer;
import no.nav.foreldrepenger.domene.rest.dto.fordeling.FordelBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.rest.historikk.FordelBeregningsgrunnlagHistorikkTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = FordelBeregningsgrunnlagDto.class, adapter = AksjonspunktOppdaterer.class)
public class FordelBeregningsgrunnlagOppdaterer implements AksjonspunktOppdaterer<FordelBeregningsgrunnlagDto>  {

    private FordelBeregningsgrunnlagHistorikkTjeneste fordelBeregningsgrunnlagHistorikkTjeneste;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputTjeneste;
    private BeregningHåndterer beregningHåndterer;

    FordelBeregningsgrunnlagOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public FordelBeregningsgrunnlagOppdaterer(FordelBeregningsgrunnlagHistorikkTjeneste fordelBeregningsgrunnlagHistorikkTjeneste,
                                              BeregningsgrunnlagInputProvider beregningsgrunnlagInputTjeneste,
                                              BeregningHåndterer beregningHåndterer) {
        this.fordelBeregningsgrunnlagHistorikkTjeneste = fordelBeregningsgrunnlagHistorikkTjeneste;
        this.beregningsgrunnlagInputTjeneste = beregningsgrunnlagInputTjeneste;
        this.beregningHåndterer = beregningHåndterer;
    }

    @Override
    public OppdateringResultat oppdater(FordelBeregningsgrunnlagDto dto, AksjonspunktOppdaterParameter param) {
        var tjeneste = beregningsgrunnlagInputTjeneste.getTjeneste(param.getRef().fagsakYtelseType());
        var input = tjeneste.lagInput(param.getRef());
        beregningHåndterer.håndterFordelBeregningsgrunnlag(input, OppdatererDtoMapper.mapFordelBeregningsgrunnlagDto(dto));
        fordelBeregningsgrunnlagHistorikkTjeneste.lagHistorikk(dto, param);
        return OppdateringResultat.utenOverhopp();
    }

}

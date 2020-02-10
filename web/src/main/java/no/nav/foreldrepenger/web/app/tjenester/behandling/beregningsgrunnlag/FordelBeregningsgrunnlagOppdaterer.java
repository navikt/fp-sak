package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputFelles;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputProvider;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus.OppdatererDtoMapper;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.BeregningHåndterer;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.FordelBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.historikk.FordelBeregningsgrunnlagHistorikkTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.FordelBeregningsgrunnlagDto.class, adapter = AksjonspunktOppdaterer.class)
public class FordelBeregningsgrunnlagOppdaterer implements AksjonspunktOppdaterer<no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.FordelBeregningsgrunnlagDto>  {

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
        BeregningsgrunnlagInputFelles tjeneste = beregningsgrunnlagInputTjeneste.getTjeneste(param.getRef().getFagsakYtelseType());
        BeregningsgrunnlagInput input = tjeneste.lagInput(param.getRef().getBehandlingId());
        beregningHåndterer.håndterFordelBeregningsgrunnlag(input, OppdatererDtoMapper.mapFordelBeregningsgrunnlagDto(dto));
        fordelBeregningsgrunnlagHistorikkTjeneste.lagHistorikk(dto, param);
        return OppdateringResultat.utenOveropp();
    }

}

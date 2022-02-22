package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningTjeneste;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputProvider;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.OppdatererDtoMapper;
import no.nav.foreldrepenger.domene.oppdateringresultat.OppdaterBeregningsgrunnlagResultat;
import no.nav.foreldrepenger.domene.rest.BeregningHåndterer;
import no.nav.foreldrepenger.domene.rest.dto.fordeling.FordelBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.rest.historikk.FordelBeregningsgrunnlagHistorikkTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = FordelBeregningsgrunnlagDto.class, adapter = AksjonspunktOppdaterer.class)
public class FordelBeregningsgrunnlagOppdaterer implements AksjonspunktOppdaterer<FordelBeregningsgrunnlagDto>  {

    private FordelBeregningsgrunnlagHistorikkTjeneste fordelBeregningsgrunnlagHistorikkTjeneste;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputTjeneste;
    private BeregningHåndterer beregningHåndterer;
    private BeregningTjeneste beregningTjeneste;

    FordelBeregningsgrunnlagOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public FordelBeregningsgrunnlagOppdaterer(FordelBeregningsgrunnlagHistorikkTjeneste fordelBeregningsgrunnlagHistorikkTjeneste,
                                              BeregningsgrunnlagInputProvider beregningsgrunnlagInputTjeneste,
                                              BeregningHåndterer beregningHåndterer, BeregningTjeneste beregningTjeneste) {
        this.fordelBeregningsgrunnlagHistorikkTjeneste = fordelBeregningsgrunnlagHistorikkTjeneste;
        this.beregningsgrunnlagInputTjeneste = beregningsgrunnlagInputTjeneste;
        this.beregningHåndterer = beregningHåndterer;
        this.beregningTjeneste = beregningTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(FordelBeregningsgrunnlagDto dto, AksjonspunktOppdaterParameter param) {
        var endringsresultat = beregningTjeneste.oppdater(param, dto);
        endringsresultat.getBeregningsgrunnlagEndring().ifPresent(e ->
                fordelBeregningsgrunnlagHistorikkTjeneste.lagHistorikk(dto, param, e)
            );
        return OppdateringResultat.utenOveropp();
    }

}

package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningTjeneste;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputProvider;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.rest.BeregningHåndterer;
import no.nav.foreldrepenger.domene.rest.dto.VurderFaktaOmBeregningDto;
import no.nav.foreldrepenger.domene.rest.historikk.FaktaBeregningHistorikkHåndterer;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;


@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderFaktaOmBeregningDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderFaktaOmBeregningOppdaterer implements AksjonspunktOppdaterer<VurderFaktaOmBeregningDto> {

    private FaktaBeregningHistorikkHåndterer faktaBeregningHistorikkHåndterer;
    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputTjeneste;
    private BeregningHåndterer beregningHåndterer;
    private BeregningTjeneste beregningTjeneste;

    VurderFaktaOmBeregningOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderFaktaOmBeregningOppdaterer(FaktaBeregningHistorikkHåndterer faktaBeregningHistorikkHåndterer,
                                            HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                            InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                            BeregningsgrunnlagInputProvider beregningsgrunnlagInputTjeneste,
                                            BeregningHåndterer beregningHåndterer, BeregningTjeneste beregningTjeneste)  {
        this.faktaBeregningHistorikkHåndterer = faktaBeregningHistorikkHåndterer;
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.beregningsgrunnlagInputTjeneste = beregningsgrunnlagInputTjeneste;
        this.beregningHåndterer = beregningHåndterer;
        this.beregningTjeneste = beregningTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(VurderFaktaOmBeregningDto dto, AksjonspunktOppdaterParameter param) {
        var behandling = param.getBehandling();
        var forrigeGrunnlag = beregningsgrunnlagTjeneste
            .hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(
                behandling.getId(),
                behandling.getOriginalBehandlingId(),
                BeregningsgrunnlagTilstand.KOFAKBER_UT);

        var oppdaterResultat = beregningTjeneste.oppdater(param, dto);

        var nyttBeregningsgrunnlag = beregningsgrunnlagTjeneste
            .hentSisteBeregningsgrunnlagGrunnlagEntitet(behandling.getId(), BeregningsgrunnlagTilstand.KOFAKBER_UT)
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag)
            .orElseThrow(() -> new IllegalStateException("Skal ha lagret beregningsgrunnlag fra KOFAKBER_UT."));
        var inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandling.getId());
        faktaBeregningHistorikkHåndterer.lagHistorikk(param,
            oppdaterResultat,
            dto, inntektArbeidYtelseGrunnlag);
        return OppdateringResultat.utenOveropp();
    }
}

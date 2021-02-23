package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputFelles;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.OppdatererDtoMapper;
import no.nav.foreldrepenger.domene.rest.BeregningHåndterer;
import no.nav.foreldrepenger.domene.rest.dto.VurderFaktaOmBeregningDto;
import no.nav.foreldrepenger.domene.rest.historikk.FaktaBeregningHistorikkHåndterer;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;


@ApplicationScoped
@DtoTilServiceAdapter(dto = no.nav.foreldrepenger.domene.rest.dto.VurderFaktaOmBeregningDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderFaktaOmBeregningOppdaterer implements AksjonspunktOppdaterer<no.nav.foreldrepenger.domene.rest.dto.VurderFaktaOmBeregningDto> {

    private FaktaBeregningHistorikkHåndterer faktaBeregningHistorikkHåndterer;
    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputTjeneste;
    private BeregningHåndterer beregningHåndterer;

    VurderFaktaOmBeregningOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderFaktaOmBeregningOppdaterer(FaktaBeregningHistorikkHåndterer faktaBeregningHistorikkHåndterer,
                                            HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                            InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                            BeregningsgrunnlagInputProvider beregningsgrunnlagInputTjeneste,
                                            BeregningHåndterer beregningHåndterer)  {
        this.faktaBeregningHistorikkHåndterer = faktaBeregningHistorikkHåndterer;
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.beregningsgrunnlagInputTjeneste = beregningsgrunnlagInputTjeneste;
        this.beregningHåndterer = beregningHåndterer;
    }

    @Override
    public OppdateringResultat oppdater(VurderFaktaOmBeregningDto dto, AksjonspunktOppdaterParameter param) {
        Behandling behandling = param.getBehandling();
        Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag = beregningsgrunnlagTjeneste
            .hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(
                behandling.getId(),
                behandling.getOriginalBehandlingId(),
                BeregningsgrunnlagTilstand.KOFAKBER_UT);

        BeregningsgrunnlagInputFelles tjeneste = beregningsgrunnlagInputTjeneste.getTjeneste(param.getRef().getFagsakYtelseType());

        BeregningsgrunnlagInput input = tjeneste.lagInput(param.getRef());

        beregningHåndterer.håndterVurderFaktaOmBeregning(input, OppdatererDtoMapper.mapTilFaktaOmBeregningLagreDto(dto.getFakta()));

        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = beregningsgrunnlagTjeneste
            .hentSisteBeregningsgrunnlagGrunnlagEntitet(behandling.getId(), BeregningsgrunnlagTilstand.KOFAKBER_UT)
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag)
            .orElseThrow(() -> new IllegalStateException("Skal ha lagret beregningsgrunnlag fra KOFAKBER_UT."));
        InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandling.getId());
        faktaBeregningHistorikkHåndterer.lagHistorikk(param, dto, nyttBeregningsgrunnlag, forrigeGrunnlag, inntektArbeidYtelseGrunnlag);
        return OppdateringResultat.utenOveropp();
    }
}

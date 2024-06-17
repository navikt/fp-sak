package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.domene.mappers.til_kalkulator.BeregningsgrunnlagInputProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.mappers.til_kalkulator.OppdatererDtoMapper;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.rest.BeregningHåndterer;
import no.nav.foreldrepenger.domene.rest.dto.VurderFaktaOmBeregningDto;
import no.nav.foreldrepenger.domene.rest.historikk.FaktaBeregningHistorikkHåndterer;


@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderFaktaOmBeregningDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderFaktaOmBeregningOppdaterer implements AksjonspunktOppdaterer<VurderFaktaOmBeregningDto> {

    private FaktaBeregningHistorikkHåndterer faktaBeregningHistorikkHåndterer;
    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputTjeneste;
    private BeregningHåndterer beregningHåndterer;
    private BehandlingRepository behandlingRepository;

    VurderFaktaOmBeregningOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderFaktaOmBeregningOppdaterer(FaktaBeregningHistorikkHåndterer faktaBeregningHistorikkHåndterer,
                                            HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                            InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                            BeregningsgrunnlagInputProvider beregningsgrunnlagInputTjeneste,
                                            BeregningHåndterer beregningHåndterer,
                                            BehandlingRepository behandlingRepository) {
        this.faktaBeregningHistorikkHåndterer = faktaBeregningHistorikkHåndterer;
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.beregningsgrunnlagInputTjeneste = beregningsgrunnlagInputTjeneste;
        this.beregningHåndterer = beregningHåndterer;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public OppdateringResultat oppdater(VurderFaktaOmBeregningDto dto, AksjonspunktOppdaterParameter param) {
        var behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        var forrigeGrunnlag = beregningsgrunnlagTjeneste.hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(behandling.getId(),
            behandling.getOriginalBehandlingId(), BeregningsgrunnlagTilstand.KOFAKBER_UT);

        var tjeneste = beregningsgrunnlagInputTjeneste.getTjeneste(param.getRef().fagsakYtelseType());

        var input = tjeneste.lagInput(param.getRef());

        beregningHåndterer.håndterVurderFaktaOmBeregning(input, OppdatererDtoMapper.mapTilFaktaOmBeregningLagreDto(dto.getFakta()));

        var nyttBeregningsgrunnlag = beregningsgrunnlagTjeneste.hentSisteBeregningsgrunnlagGrunnlagEntitet(behandling.getId(),
                BeregningsgrunnlagTilstand.KOFAKBER_UT)
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag)
            .orElseThrow(() -> new IllegalStateException("Skal ha lagret beregningsgrunnlag fra KOFAKBER_UT."));
        var inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandling.getId());
        faktaBeregningHistorikkHåndterer.lagHistorikk(param, dto, nyttBeregningsgrunnlag, forrigeGrunnlag, inntektArbeidYtelseGrunnlag);
        return OppdateringResultat.utenOverhopp();
    }
}

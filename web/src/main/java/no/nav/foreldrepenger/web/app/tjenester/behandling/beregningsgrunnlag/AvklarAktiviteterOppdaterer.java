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
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus.OppdatererDtoMapper;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.BeregningHåndterer;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.AvklarteAktiviteterDto;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.historikk.BeregningsaktivitetHistorikkTjeneste;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningAktivitetAggregatEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarteAktiviteterDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarAktiviteterOppdaterer implements AksjonspunktOppdaterer<AvklarteAktiviteterDto> {

    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private BeregningsaktivitetHistorikkTjeneste beregningsaktivitetHistorikkTjeneste;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputTjeneste;
    private BeregningHåndterer beregningHåndterer;

    AvklarAktiviteterOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AvklarAktiviteterOppdaterer(HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                       BeregningsaktivitetHistorikkTjeneste beregningsaktivitetHistorikkTjeneste,
                                       BeregningsgrunnlagInputProvider beregningsgrunnlagInputTjeneste,
                                       BeregningHåndterer beregningHåndterer) {
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.beregningsaktivitetHistorikkTjeneste = beregningsaktivitetHistorikkTjeneste;
        this.beregningsgrunnlagInputTjeneste = beregningsgrunnlagInputTjeneste;
        this.beregningHåndterer = beregningHåndterer;
    }

    @Override
    public OppdateringResultat oppdater(AvklarteAktiviteterDto dto, AksjonspunktOppdaterParameter param) {

        Optional<Long> originalBehandlingId = param.getRef().getOriginalBehandlingId();
        Long behandlingId = param.getBehandlingId();
        Optional<BeregningAktivitetAggregatEntitet> forrige = beregningsgrunnlagTjeneste.hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(behandlingId, originalBehandlingId,
            BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER)
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getSaksbehandletAktiviteter);

        BeregningsgrunnlagInputFelles tjeneste = beregningsgrunnlagInputTjeneste.getTjeneste(param.getRef().getFagsakYtelseType());
        BeregningsgrunnlagInput inputUtenBeregningsgrunnlag = tjeneste.lagInput(param.getRef());

        beregningHåndterer.håndterAvklarAktiviteter(inputUtenBeregningsgrunnlag, OppdatererDtoMapper.mapAvklarteAktiviteterDto(dto));

        BeregningsgrunnlagGrunnlagEntitet lagretGrunnlag = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagGrunnlagEntitet(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Har ikke et aktivt grunnlag"));
        BeregningAktivitetAggregatEntitet registerAktiviteter = lagretGrunnlag.getRegisterAktiviteter();
        BeregningAktivitetAggregatEntitet saksbehandledeAktiviteter = lagretGrunnlag.getSaksbehandletAktiviteter()
            .orElseThrow(() -> new IllegalStateException("Forventer å ha lagret ned saksbehandlet grunnlag"));
        beregningsaktivitetHistorikkTjeneste.lagHistorikk(behandlingId, registerAktiviteter, saksbehandledeAktiviteter, dto.getBegrunnelse(), forrige);

        return OppdateringResultat.utenOveropp();
    }

}

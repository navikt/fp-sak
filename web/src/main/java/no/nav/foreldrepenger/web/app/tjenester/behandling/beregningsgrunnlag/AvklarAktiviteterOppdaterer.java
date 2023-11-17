package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputProvider;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.OppdatererDtoMapper;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.rest.BeregningHåndterer;
import no.nav.foreldrepenger.domene.rest.dto.AvklarteAktiviteterDto;
import no.nav.foreldrepenger.domene.rest.historikk.BeregningsaktivitetHistorikkTjeneste;

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

        var originalBehandlingId = param.getRef().getOriginalBehandlingId();
        var behandlingId = param.getBehandlingId();
        var forrige = beregningsgrunnlagTjeneste.hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(behandlingId, originalBehandlingId,
            BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER)
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getSaksbehandletAktiviteter);

        var tjeneste = beregningsgrunnlagInputTjeneste.getTjeneste(param.getRef().fagsakYtelseType());
        var inputUtenBeregningsgrunnlag = tjeneste.lagInput(param.getRef());

        beregningHåndterer.håndterAvklarAktiviteter(inputUtenBeregningsgrunnlag, OppdatererDtoMapper.mapAvklarteAktiviteterDto(dto));

        var lagretGrunnlag = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagGrunnlagEntitet(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Har ikke et aktivt grunnlag"));
        var registerAktiviteter = lagretGrunnlag.getRegisterAktiviteter();
        var saksbehandledeAktiviteter = lagretGrunnlag.getSaksbehandletAktiviteter()
            .orElseThrow(() -> new IllegalStateException("Forventer å ha lagret ned saksbehandlet grunnlag"));
        beregningsaktivitetHistorikkTjeneste.lagHistorikk(behandlingId, registerAktiviteter, saksbehandledeAktiviteter, dto.getBegrunnelse(), forrige);

        return OppdateringResultat.utenOverhopp();
    }

}

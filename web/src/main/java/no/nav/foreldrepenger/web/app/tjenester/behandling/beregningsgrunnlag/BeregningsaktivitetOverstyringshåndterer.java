package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AbstractOverstyringshåndterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputFelles;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputProvider;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.OppdatererDtoMapper;
import no.nav.foreldrepenger.domene.rest.BeregningHåndterer;
import no.nav.foreldrepenger.domene.rest.dto.OverstyrBeregningsaktiviteterDto;
import no.nav.foreldrepenger.domene.rest.historikk.BeregningsaktivitetHistorikkTjeneste;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitetAggregatEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyrBeregningsaktiviteterDto.class, adapter = Overstyringshåndterer.class)
public class BeregningsaktivitetOverstyringshåndterer extends AbstractOverstyringshåndterer<OverstyrBeregningsaktiviteterDto> {

    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private BeregningsaktivitetHistorikkTjeneste beregningsaktivitetHistorikkTjeneste;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputTjeneste;
    private BeregningHåndterer beregningHåndterer;


    BeregningsaktivitetOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public BeregningsaktivitetOverstyringshåndterer(HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                                    HistorikkTjenesteAdapter historikkAdapter,
                                                    BeregningsaktivitetHistorikkTjeneste beregningsaktivitetHistorikkTjeneste,
                                                    BeregningsgrunnlagInputProvider beregningsgrunnlagInputTjeneste,
                                                    BeregningHåndterer beregningHåndterer) {
        super(historikkAdapter, AksjonspunktDefinisjon.OVERSTYRING_AV_BEREGNINGSAKTIVITETER);
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.beregningsaktivitetHistorikkTjeneste = beregningsaktivitetHistorikkTjeneste;
        this.beregningsgrunnlagInputTjeneste = beregningsgrunnlagInputTjeneste;
        this.beregningHåndterer = beregningHåndterer;
    }

    //TODO(OJR) endre til å benytte BehandlingReferanse?
    @Override
    public OppdateringResultat håndterOverstyring(OverstyrBeregningsaktiviteterDto dto, Behandling behandling,
                                                  BehandlingskontrollKontekst kontekst) {

        BeregningsgrunnlagInputFelles tjeneste = beregningsgrunnlagInputTjeneste.getTjeneste(behandling.getFagsakYtelseType());
        BeregningsgrunnlagInput input = tjeneste.lagInput(behandling.getId());
        beregningHåndterer.håndterBeregningAktivitetOverstyring(input, OppdatererDtoMapper.mapOverstyrBeregningsaktiviteterDto(dto.getBeregningsaktivitetLagreDtoList()));
        return OppdateringResultat.utenOveropp();
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyrBeregningsaktiviteterDto dto) {
        BeregningsgrunnlagGrunnlagEntitet grunnlag = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagGrunnlagEntitet(behandling.getId())
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Mangler BeregningsgrunnlagGrunnlagEntitet"));
        Optional<Long> originalBehandlingId = behandling.getOriginalBehandlingId();
        Optional<BeregningAktivitetAggregatEntitet> forrige = beregningsgrunnlagTjeneste.hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(behandling.getId(), originalBehandlingId,
            BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER)
            .map(BeregningsgrunnlagGrunnlagEntitet::getGjeldendeAktiviteter);
        BeregningAktivitetAggregatEntitet registerAktiviteter = grunnlag.getRegisterAktiviteter();
        BeregningAktivitetAggregatEntitet overstyrteAktiviteter = grunnlag.getGjeldendeAktiviteter();
        beregningsaktivitetHistorikkTjeneste.lagHistorikk(behandling.getId(),
            getHistorikkAdapter().tekstBuilder().medHendelse(HistorikkinnslagType.OVERSTYRT),
            registerAktiviteter,
            overstyrteAktiviteter,
            dto.getBegrunnelse(),
            forrige);
    }
}

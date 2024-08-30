package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AbstractOverstyringshåndterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.domene.mappers.til_kalkulator.BeregningsgrunnlagInputProvider;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.mappers.til_kalkulator.OppdatererDtoMapper;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.rest.BeregningHåndterer;
import no.nav.foreldrepenger.domene.rest.dto.OverstyrBeregningsaktiviteterDto;
import no.nav.foreldrepenger.domene.rest.historikk.BeregningsaktivitetHistorikkTjeneste;
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

        var tjeneste = beregningsgrunnlagInputTjeneste.getTjeneste(behandling.getFagsakYtelseType());
        var ref = BehandlingReferanse.fra(behandling);
        var input = tjeneste.lagInput(ref);
        beregningHåndterer.håndterBeregningAktivitetOverstyring(input, OppdatererDtoMapper.mapOverstyrBeregningsaktiviteterDto(dto.getBeregningsaktivitetLagreDtoList()));
        lagHistorikk(ref, dto);
        return OppdateringResultat.utenOverhopp();
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyrBeregningsaktiviteterDto dto) {
        // Håndteres sammen med overstyringen
    }

    protected void lagHistorikk(BehandlingReferanse referanse, OverstyrBeregningsaktiviteterDto dto) {
        var grunnlag = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagGrunnlagEntitet(referanse.behandlingId())
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Mangler BeregningsgrunnlagGrunnlagEntitet"));
        var originalBehandlingId = referanse.getOriginalBehandlingId();
        var forrige = beregningsgrunnlagTjeneste.hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(referanse.behandlingId(), originalBehandlingId,
                BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER)
            .map(BeregningsgrunnlagGrunnlagEntitet::getGjeldendeAktiviteter);
        var registerAktiviteter = grunnlag.getRegisterAktiviteter();
        var overstyrteAktiviteter = grunnlag.getGjeldendeAktiviteter();
        beregningsaktivitetHistorikkTjeneste.lagHistorikk(referanse.behandlingId(),
            getHistorikkAdapter().tekstBuilder().medHendelse(HistorikkinnslagType.OVERSTYRT),
            registerAktiviteter,
            overstyrteAktiviteter,
            dto.getBegrunnelse(),
            forrige);
    }

}

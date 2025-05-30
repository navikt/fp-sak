package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.rest.dto.OverstyrBeregningsaktiviteterDto;
import no.nav.foreldrepenger.domene.rest.historikk.BeregningsaktivitetHistorikkTjeneste;
import no.nav.foreldrepenger.domene.rest.historikk.kalkulus.BeregningsaktivitetHistorikkKalkulusTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyrBeregningsaktiviteterDto.class, adapter = Overstyringshåndterer.class)
public class BeregningsaktivitetOverstyringshåndterer implements Overstyringshåndterer<OverstyrBeregningsaktiviteterDto> {

    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private BeregningsaktivitetHistorikkTjeneste beregningsaktivitetHistorikkTjeneste;
    private BeregningTjeneste beregningTjeneste;
    private BeregningsaktivitetHistorikkKalkulusTjeneste beregningsaktivitetHistorikkKalkulusTjeneste;

    BeregningsaktivitetOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public BeregningsaktivitetOverstyringshåndterer(HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                                    BeregningsaktivitetHistorikkTjeneste beregningsaktivitetHistorikkTjeneste,
                                                    BeregningTjeneste beregningTjeneste,
                                                    BeregningsaktivitetHistorikkKalkulusTjeneste beregningsaktivitetHistorikkKalkulusTjeneste) {
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.beregningsaktivitetHistorikkTjeneste = beregningsaktivitetHistorikkTjeneste;
        this.beregningTjeneste = beregningTjeneste;
        this.beregningsaktivitetHistorikkKalkulusTjeneste = beregningsaktivitetHistorikkKalkulusTjeneste;
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyrBeregningsaktiviteterDto dto, BehandlingReferanse ref) {
        var endringsaggregat = beregningTjeneste.overstyrBeregning(dto, ref);
        if (endringsaggregat.isPresent()) {
            beregningsaktivitetHistorikkKalkulusTjeneste.lagHistorikk(ref, dto.getBegrunnelse(), endringsaggregat.get());

        } else {
            var grunnlag = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagGrunnlagEntitet(ref.behandlingId())
                .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Mangler BeregningsgrunnlagGrunnlagEntitet"));
            var originalBehandlingId = ref.getOriginalBehandlingId();
            var forrige = beregningsgrunnlagTjeneste.hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(ref.behandlingId(), originalBehandlingId,
                    BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER)
                .map(BeregningsgrunnlagGrunnlagEntitet::getGjeldendeAktiviteter);
            var registerAktiviteter = grunnlag.getRegisterAktiviteter();
            var overstyrteAktiviteter = grunnlag.getGjeldendeAktiviteter();
            beregningsaktivitetHistorikkTjeneste.lagHistorikk(ref,
                registerAktiviteter,
                overstyrteAktiviteter,
                dto.getBegrunnelse(),
                forrige);
        }
        return OppdateringResultat.utenOverhopp();
    }
}

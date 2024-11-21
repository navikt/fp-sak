package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AbstractOverstyringshåndterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.rest.dto.OverstyrBeregningsaktiviteterDto;
import no.nav.foreldrepenger.domene.rest.historikk.BeregningsaktivitetHistorikkTjeneste;
import no.nav.foreldrepenger.domene.rest.historikk.kalkulus.BeregningsaktivitetHistorikkKalkulusTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyrBeregningsaktiviteterDto.class, adapter = Overstyringshåndterer.class)
public class BeregningsaktivitetOverstyringshåndterer extends AbstractOverstyringshåndterer<OverstyrBeregningsaktiviteterDto> {

    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private BeregningsaktivitetHistorikkTjeneste beregningsaktivitetHistorikkTjeneste;
    private BeregningTjeneste beregningTjeneste;
    private BeregningsaktivitetHistorikkKalkulusTjeneste beregningsaktivitetHistorikkKalkulusTjeneste;

    BeregningsaktivitetOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public BeregningsaktivitetOverstyringshåndterer(HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                                    HistorikkTjenesteAdapter historikkAdapter,
                                                    BeregningsaktivitetHistorikkTjeneste beregningsaktivitetHistorikkTjeneste,
                                                    BeregningTjeneste beregningTjeneste,
                                                    BeregningsaktivitetHistorikkKalkulusTjeneste beregningsaktivitetHistorikkKalkulusTjeneste) {
        super(historikkAdapter, AksjonspunktDefinisjon.OVERSTYRING_AV_BEREGNINGSAKTIVITETER);
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.beregningsaktivitetHistorikkTjeneste = beregningsaktivitetHistorikkTjeneste;
        this.beregningTjeneste = beregningTjeneste;
        this.beregningsaktivitetHistorikkKalkulusTjeneste = beregningsaktivitetHistorikkKalkulusTjeneste;
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyrBeregningsaktiviteterDto dto, Behandling behandling,
                                                  BehandlingskontrollKontekst kontekst) {
        var ref = BehandlingReferanse.fra(behandling);
        var endringsaggregat = beregningTjeneste.overstyrBeregning(dto, ref);
        if (endringsaggregat.isPresent()) {
            beregningsaktivitetHistorikkKalkulusTjeneste.lagHistorikk(ref, dto.getBegrunnelse(), endringsaggregat.get());

        } else {
            var grunnlag = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagGrunnlagEntitet(behandling.getId())
                .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Mangler BeregningsgrunnlagGrunnlagEntitet"));
            var originalBehandlingId = behandling.getOriginalBehandlingId();
            var forrige = beregningsgrunnlagTjeneste.hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(behandling.getId(), originalBehandlingId,
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

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyrBeregningsaktiviteterDto dto) {
        // Håndteres sammen med selve overstyringen
    }
}

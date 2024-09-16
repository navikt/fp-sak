package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.app;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.totrinn.Totrinnsvurdering;
import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.dto.TotrinnsBeregningDto;

@ApplicationScoped
public class TotrinnsBeregningDtoTjeneste {
    private BeregningTjeneste beregningTjeneste;

    protected TotrinnsBeregningDtoTjeneste() {
        // for CDI proxy
    }

    @Inject
    public TotrinnsBeregningDtoTjeneste(BeregningTjeneste beregningTjeneste) {
        this.beregningTjeneste = beregningTjeneste;
    }

    TotrinnsBeregningDto hentBeregningDto(Totrinnsvurdering aksjonspunkt,
                                                  Behandling behandling) {
        var dto = new TotrinnsBeregningDto();
        var ref = BehandlingReferanse.fra(behandling);
        if (aksjonspunkt.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE)) {
            dto.setFastsattVarigEndringNaering(erVarigEndringFastsattForSelvstendingNæringsdrivendeGittBehandlingId(ref));
        }
        if (AksjonspunktDefinisjon.VURDER_FAKTA_FOR_ATFL_SN.equals(aksjonspunkt.getAksjonspunktDefinisjon())) {
            var bg = beregningTjeneste.hent(ref).flatMap(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag);
            var tilfeller = bg.map(Beregningsgrunnlag::getFaktaOmBeregningTilfeller).orElseGet(List::of);
            dto.setFaktaOmBeregningTilfeller(mapTilfelle(tilfeller));
        }
        return dto;
    }

    private List<FaktaOmBeregningTilfelle> mapTilfelle(List<FaktaOmBeregningTilfelle> tilfeller) {
        return tilfeller.stream().map(t -> FaktaOmBeregningTilfelle.fraKode(t.getKode())).toList();
    }

    private boolean erVarigEndringFastsattForSelvstendingNæringsdrivendeGittBehandlingId(BehandlingReferanse ref) {
        var beregningsgrunnlag = beregningTjeneste.hent(ref).flatMap(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag);

        return beregningsgrunnlag.map(Beregningsgrunnlag::getBeregningsgrunnlagPerioder).orElseGet(List::of).stream()
            .flatMap(bgps -> bgps.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .filter(andel -> AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE.equals(AktivitetStatus.fraKode(andel.getAktivitetStatus().getKode())))
            .anyMatch(andel -> andel.getOverstyrtPrÅr() != null);
    }
}

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

    TotrinnsBeregningDto hentBeregningDto(Totrinnsvurdering aksjonspunkt, Behandling behandling) {
        var ref = BehandlingReferanse.fra(behandling);
        var erVarigEndringNæring = aksjonspunkt.getAksjonspunktDefinisjon()
            .equals(AksjonspunktDefinisjon.VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE)
            && erVarigEndringFastsattForSelvstendingNæringsdrivendeGittBehandlingId(ref);
        var beregningTilfeller = AksjonspunktDefinisjon.VURDER_FAKTA_FOR_ATFL_SN.equals(
            aksjonspunkt.getAksjonspunktDefinisjon()) ? beregningTjeneste.hent(ref)
            .flatMap(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag)
            .map(Beregningsgrunnlag::getFaktaOmBeregningTilfeller)
            .orElseGet(List::of) : null;
        return new TotrinnsBeregningDto(erVarigEndringNæring, erVarigEndringNæring, beregningTilfeller);
    }

    private boolean erVarigEndringFastsattForSelvstendingNæringsdrivendeGittBehandlingId(BehandlingReferanse ref) {
        var beregningsgrunnlag = beregningTjeneste.hent(ref).flatMap(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag);

        return beregningsgrunnlag.map(Beregningsgrunnlag::getBeregningsgrunnlagPerioder)
            .orElseGet(List::of)
            .stream()
            .flatMap(bgps -> bgps.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .filter(andel -> AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE.equals(andel.getAktivitetStatus()))
            .anyMatch(andel -> andel.getOverstyrtPrÅr() != null);
    }
}

package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.app;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.totrinn.Totrinnsvurdering;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.dto.TotrinnsBeregningDto;

@ApplicationScoped
public class TotrinnsBeregningDtoTjeneste {
    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;

    protected TotrinnsBeregningDtoTjeneste() {
        // for CDI proxy
    }

    @Inject
    public TotrinnsBeregningDtoTjeneste(HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste) {
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
    }

    TotrinnsBeregningDto hentBeregningDto(Totrinnsvurdering aksjonspunkt,
                                                  Behandling behandling) {
        var dto = new TotrinnsBeregningDto();
        if (aksjonspunkt.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE)) {
            dto.setFastsattVarigEndringNaering(erVarigEndringFastsattForSelvstendingNæringsdrivendeGittBehandlingId(behandling.getId()));
        }
        if (AksjonspunktDefinisjon.VURDER_FAKTA_FOR_ATFL_SN.equals(aksjonspunkt.getAksjonspunktDefinisjon())) {
            var bg = hentBeregningsgrunnlag(behandling.getId());
            var tilfeller = bg.map(BeregningsgrunnlagEntitet::getFaktaOmBeregningTilfeller).orElseGet(List::of);
            dto.setFaktaOmBeregningTilfeller(mapTilfelle(tilfeller));
        }
        return dto;
    }

    private List<FaktaOmBeregningTilfelle> mapTilfelle(List<FaktaOmBeregningTilfelle> tilfeller) {
        return tilfeller.stream().map(t -> FaktaOmBeregningTilfelle.fraKode(t.getKode())).toList();
    }

    private Optional<BeregningsgrunnlagEntitet> hentBeregningsgrunnlag(Long behandlingId) {
        return beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetForBehandling(behandlingId);
    }

    private boolean erVarigEndringFastsattForSelvstendingNæringsdrivendeGittBehandlingId(Long behandlingId) {
        var beregningsgrunnlag = hentBeregningsgrunnlag(behandlingId);

        return beregningsgrunnlag.map(BeregningsgrunnlagEntitet::getBeregningsgrunnlagPerioder).orElseGet(List::of).stream()
            .flatMap(bgps -> bgps.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .filter(andel -> AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE.equals(AktivitetStatus.fraKode(andel.getAktivitetStatus().getKode())))
            .anyMatch(andel -> andel.getOverstyrtPrÅr() != null);
    }
}

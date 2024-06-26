package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.prosess.BeregningsgrunnlagKopierOgLagreTjeneste;

@FagsakYtelseTypeRef
@BehandlingStegRef(BehandlingStegType.FORDEL_BEREGNINGSGRUNNLAG)
@BehandlingTypeRef
@ApplicationScoped
public class FordelBeregningsgrunnlagSteg implements BeregningsgrunnlagSteg {

    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    private BehandlingRepository behandlingRepository;
    private BeregningTjeneste beregningTjeneste;

    protected FordelBeregningsgrunnlagSteg() {
        // CDI Proxy
    }

    @Inject
    public FordelBeregningsgrunnlagSteg(BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste,
                                        BehandlingRepository behandlingRepository, BeregningTjeneste beregningTjeneste) {
        this.beregningsgrunnlagKopierOgLagreTjeneste = beregningsgrunnlagKopierOgLagreTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.beregningTjeneste = beregningTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var beregningsgrunnlagResultat = beregningTjeneste.beregn(BehandlingReferanse.fra(behandling), BehandlingStegType.FORDEL_BEREGNINGSGRUNNLAG);
        return BehandleStegResultat.utførtMedAksjonspunktResultater(beregningsgrunnlagResultat.getAksjonspunkter());
    }


    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg,
            BehandlingStegType fraSteg) {
        if (tilSteg.equals(BehandlingStegType.FORDEL_BEREGNINGSGRUNNLAG)) {
            var aps = behandlingRepository.hentBehandling(kontekst.getBehandlingId()).getAksjonspunkter();
            var harAksjonspunktSomErUtførtIUtgang = tilSteg.getAksjonspunktDefinisjonerUtgang().stream()
                    .anyMatch(ap -> aps.stream().filter(a -> a.getAksjonspunktDefinisjon().equals(ap))
                            .anyMatch(a -> !a.erAvbrutt()));
            beregningsgrunnlagKopierOgLagreTjeneste.getRyddBeregningsgrunnlag(kontekst)
                    .ryddFordelBeregningsgrunnlagVedTilbakeføring(harAksjonspunktSomErUtførtIUtgang);
        }
    }
}

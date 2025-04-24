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

@BehandlingStegRef(BehandlingStegType.VURDER_VILKAR_BERGRUNN)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderBeregningsgrunnlagVilkårSteg implements BeregningsgrunnlagSteg {
    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private BeregningTjeneste beregningTjeneste;

    protected VurderBeregningsgrunnlagVilkårSteg() {
        // CDI Proxy
    }

    @Inject
    public VurderBeregningsgrunnlagVilkårSteg(BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste,
                                              BehandlingRepository behandlingRepository,
                                              BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                                              BeregningTjeneste beregningTjeneste) {
        this.beregningsgrunnlagKopierOgLagreTjeneste = beregningsgrunnlagKopierOgLagreTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.beregningTjeneste = beregningTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var resultat = beregningTjeneste.beregn(BehandlingReferanse.fra(behandling), BehandlingStegType.VURDER_VILKAR_BERGRUNN);
        beregningsgrunnlagVilkårTjeneste.lagreVilkårresultat(kontekst, resultat);
        if (Boolean.FALSE.equals(resultat.getVilkårOppfylt())) {
            return BehandleStegResultat.fremoverført(BehandlingStegType.FORESLÅ_BEHANDLINGSRESULTAT);
        }
        return BehandleStegResultat.utførtMedAksjonspunktResultater(resultat.getAksjonspunkter());
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg,
            BehandlingStegType fraSteg) {
        if (tilSteg.equals(BehandlingStegType.VURDER_VILKAR_BERGRUNN)) {
            beregningsgrunnlagKopierOgLagreTjeneste.getRyddBeregningsgrunnlag(kontekst).ryddVurderVilkårBeregningsgrunnlagVedTilbakeføring();
        } else {
            beregningsgrunnlagVilkårTjeneste.ryddVedtaksresultatOgVilkår(kontekst);
        }
    }

}

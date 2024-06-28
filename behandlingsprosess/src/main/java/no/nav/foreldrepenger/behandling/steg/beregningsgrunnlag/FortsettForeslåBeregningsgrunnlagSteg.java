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
@BehandlingStegRef(BehandlingStegType.FORTSETT_FORESLÅ_BEREGNINGSGRUNNLAG)
@BehandlingTypeRef
@ApplicationScoped
public class FortsettForeslåBeregningsgrunnlagSteg implements BeregningsgrunnlagSteg {
    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    private BeregningTjeneste beregningTjeneste;

    protected FortsettForeslåBeregningsgrunnlagSteg() {
        // for CDI proxy
    }

    @Inject
    public FortsettForeslåBeregningsgrunnlagSteg(BehandlingRepository behandlingRepository,
                                                 BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste,
                                                 BeregningTjeneste beregningTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.beregningsgrunnlagKopierOgLagreTjeneste = beregningsgrunnlagKopierOgLagreTjeneste;
        this.beregningTjeneste = beregningTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var ref = BehandlingReferanse.fra(behandling);
        var resultat = beregningTjeneste.beregn(ref, BehandlingStegType.FORTSETT_FORESLÅ_BEREGNINGSGRUNNLAG);
        return BehandleStegResultat.utførtMedAksjonspunktResultater(resultat.getAksjonspunkter());

    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg,
            BehandlingStegType fraSteg) {
        if (tilSteg.equals(BehandlingStegType.FORTSETT_FORESLÅ_BEREGNINGSGRUNNLAG)) {
            beregningsgrunnlagKopierOgLagreTjeneste.getRyddBeregningsgrunnlag(kontekst).ryddFortsettForeslåBeregningsgrunnlagVedTilbakeføring();
        }
    }
}

package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;

@BehandlingStegRef(kode = "VURDER_REF_BERGRUNN")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderRefusjonBeregningsgrunnlagSteg implements BeregningsgrunnlagSteg {
    private BehandlingRepository behandlingRepository;
    private BeregningTjeneste beregningTjeneste;

    protected VurderRefusjonBeregningsgrunnlagSteg() {
        // CDI Proxy
    }

    @Inject
    public VurderRefusjonBeregningsgrunnlagSteg(BehandlingRepository behandlingRepository, BeregningTjeneste beregningTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.beregningTjeneste = beregningTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var beregningsgrunnlagResultat = beregningTjeneste.beregn(BehandlingReferanse.fra(behandling), BehandlingStegType.VURDER_REF_BERGRUNN);
        var aksjonspunkter = beregningsgrunnlagResultat.getAksjonspunkter();
        return BehandleStegResultat.utførtMedAksjonspunktResultater(
            aksjonspunkter.stream().map(BeregningAksjonspunktResultatMapper::map).collect(Collectors.toList()));
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst,
                                   BehandlingStegModell modell,
                                   BehandlingStegType tilSteg,
                                   BehandlingStegType fraSteg) {
        beregningTjeneste.rydd(kontekst, BehandlingStegType.VURDER_REF_BERGRUNN, tilSteg);
    }

}

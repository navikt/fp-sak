package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import static no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner.FREMHOPP_TIL_FORESLÅ_BEHANDLINGSRESULTAT;

import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.prosess.BeregningsgrunnlagVilkårTjeneste;

@BehandlingStegRef(kode = "VURDER_VILKAR_BERGRUNN")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderBeregningsgrunnlagVilkårSteg implements BeregningsgrunnlagSteg {
    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private BeregningTjeneste beregningTjeneste;

    protected VurderBeregningsgrunnlagVilkårSteg() {
        // CDI Proxy
    }

    @Inject
    public VurderBeregningsgrunnlagVilkårSteg(BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                                              BeregningTjeneste beregningTjeneste) {
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.beregningTjeneste = beregningTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var beregningsgrunnlagResultat = beregningTjeneste.beregn(kontekst.getBehandlingId(), BehandlingStegType.VURDER_VILKAR_BERGRUNN);
        beregningsgrunnlagVilkårTjeneste.lagreVilkårresultat(kontekst, beregningsgrunnlagResultat);
        if (Boolean.FALSE.equals(beregningsgrunnlagResultat.getVilkårOppfylt())) {
            return BehandleStegResultat.fremoverført(FREMHOPP_TIL_FORESLÅ_BEHANDLINGSRESULTAT);
        }
        var aksjonspunkter = beregningsgrunnlagResultat.getAksjonspunkter();
        return BehandleStegResultat.utførtMedAksjonspunktResultater(
            aksjonspunkter.stream().map(BeregningAksjonspunktResultatMapper::map).collect(Collectors.toList()));
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst,
                                   BehandlingStegModell modell,
                                   BehandlingStegType tilSteg,
                                   BehandlingStegType fraSteg) {
        beregningTjeneste.rydd(kontekst, BehandlingStegType.VURDER_VILKAR_BERGRUNN, tilSteg);
    }

}

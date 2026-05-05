package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import java.util.ArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.fp.AAPKombinertMedATFLTjeneste;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@BehandlingStegRef(BehandlingStegType.VURDER_VILKAR_BERGRUNN)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderBeregningsgrunnlagVilkårSteg implements BeregningsgrunnlagSteg {
    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private BeregningTjeneste beregningTjeneste;
    private AAPKombinertMedATFLTjeneste aapKombinertMedATFLTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    protected VurderBeregningsgrunnlagVilkårSteg() {
        // CDI Proxy
    }

    @Inject
    public VurderBeregningsgrunnlagVilkårSteg(BehandlingRepository behandlingRepository,
                                              BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                                              BeregningTjeneste beregningTjeneste,
                                              AAPKombinertMedATFLTjeneste aapKombinertMedATFLTjeneste,
                                              SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.beregningTjeneste = beregningTjeneste;
        this.aapKombinertMedATFLTjeneste = aapKombinertMedATFLTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var ref = BehandlingReferanse.fra(behandling);
        var resultat = beregningTjeneste.beregn(ref, BehandlingStegType.VURDER_VILKAR_BERGRUNN);
        beregningsgrunnlagVilkårTjeneste.lagreVilkårresultat(kontekst, resultat);
        if (Boolean.FALSE.equals(resultat.getVilkårOppfylt())) {
            return BehandleStegResultat.fremoverført(BehandlingStegType.FORESLÅ_BEHANDLINGSRESULTAT);
        }
        var aksjonspunkter = new ArrayList<>(resultat.getAksjonspunkter());
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(kontekst.getBehandlingId());
        if (aapKombinertMedATFLTjeneste.harAAPKombinertMedATFL(ref, stp)) {
            aksjonspunkter.add(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.MANUELL_KONTROLL_AAP_KOMBINERT_ATFL));
        }
        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunkter);
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg,
            BehandlingStegType fraSteg) {
        if (!tilSteg.equals(BehandlingStegType.VURDER_VILKAR_BERGRUNN)) {
            beregningsgrunnlagVilkårTjeneste.ryddVedtaksresultatOgVilkår(kontekst);
        }
    }
}

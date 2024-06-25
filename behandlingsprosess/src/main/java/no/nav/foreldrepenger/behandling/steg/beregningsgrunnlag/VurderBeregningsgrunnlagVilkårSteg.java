package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import static no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner.FREMHOPP_TIL_FORESLÅ_BEHANDLINGSRESULTAT;

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.mappers.til_kalkulator.BeregningsgrunnlagInputFelles;
import no.nav.foreldrepenger.domene.mappers.til_kalkulator.BeregningsgrunnlagInputProvider;
import no.nav.foreldrepenger.domene.prosess.BeregningsgrunnlagKopierOgLagreTjeneste;

@BehandlingStegRef(BehandlingStegType.VURDER_VILKAR_BERGRUNN)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderBeregningsgrunnlagVilkårSteg implements BeregningsgrunnlagSteg {
    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputProvider;
    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;

    protected VurderBeregningsgrunnlagVilkårSteg() {
        // CDI Proxy
    }

    @Inject
    public VurderBeregningsgrunnlagVilkårSteg(BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste,
                                              BehandlingRepository behandlingRepository,
                                              BeregningsgrunnlagInputProvider inputTjenesteProvider,
                                              BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste) {
        this.beregningsgrunnlagInputProvider = Objects.requireNonNull(inputTjenesteProvider, "inputTjenesteProvider");
        this.beregningsgrunnlagKopierOgLagreTjeneste = beregningsgrunnlagKopierOgLagreTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var input = getInputTjeneste(behandling.getFagsakYtelseType()).lagInput(behandlingId);
        var resultat = beregningsgrunnlagKopierOgLagreTjeneste.vurderVilkårBeregningsgrunnlag(input);
        beregningsgrunnlagVilkårTjeneste.lagreVilkårresultat(kontekst, resultat);
        if (Boolean.FALSE.equals(resultat.getVilkårOppfylt())) {
            return BehandleStegResultat.fremoverført(FREMHOPP_TIL_FORESLÅ_BEHANDLINGSRESULTAT);
        }
        return BehandleStegResultat.utførtMedAksjonspunktResultater(resultat.getAksjonspunkter());
    }

    private BeregningsgrunnlagInputFelles getInputTjeneste(FagsakYtelseType ytelseType) {
        return beregningsgrunnlagInputProvider.getTjeneste(ytelseType);
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

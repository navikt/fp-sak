package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import java.util.Objects;
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
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.prosess.BeregningsgrunnlagKopierOgLagreTjeneste;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.prosess.SplittForeslåBgToggle;

@FagsakYtelseTypeRef
@BehandlingStegRef(BehandlingStegType.FORTSETT_FORESLÅ_BEREGNINGSGRUNNLAG)
@BehandlingTypeRef
@ApplicationScoped
public class ForeslåBeregningsgrunnlag2Steg implements BeregningsgrunnlagSteg {
    private static final String SPLITT_FORESLÅ_TOGGLE = "splitt-foreslå-toggle";

    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputProvider;
    private HentOgLagreBeregningsgrunnlagTjeneste hentOgLagreBeregningsgrunnlagTjeneste;

    protected ForeslåBeregningsgrunnlag2Steg() {
        // for CDI proxy
    }

    @Inject
    public ForeslåBeregningsgrunnlag2Steg(BehandlingRepository behandlingRepository,
                                          BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste,
                                          BeregningsgrunnlagInputProvider inputTjenesteProvider,
                                          HentOgLagreBeregningsgrunnlagTjeneste hentOgLagreBeregningsgrunnlagTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.beregningsgrunnlagKopierOgLagreTjeneste = beregningsgrunnlagKopierOgLagreTjeneste;
        this.beregningsgrunnlagInputProvider = Objects.requireNonNull(inputTjenesteProvider, "inputTjenesteProvider");
        this.hentOgLagreBeregningsgrunnlagTjeneste = hentOgLagreBeregningsgrunnlagTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var ref = BehandlingReferanse.fra(behandling);
        var input = getInputTjeneste(ref.fagsakYtelseType()).lagInput(ref.behandlingId());
        if (input.isEnabled(SPLITT_FORESLÅ_TOGGLE, false)) {
            var resultat = beregningsgrunnlagKopierOgLagreTjeneste.foreslåBeregningsgrunnlag2(input);
            var aksjonspunkter = resultat.getAksjonspunkter().stream().map(BeregningAksjonspunktResultatMapper::map)
                .collect(Collectors.toList());
            return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunkter);
        } else {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg,
            BehandlingStegType fraSteg) {
        if (tilSteg.equals(BehandlingStegType.FORTSETT_FORESLÅ_BEREGNINGSGRUNNLAG)) {
            // Midlertidig fiks til alle saker med aksjonspunkt som starter i foreslå 2 faktisk har et foreslå 2 grunnlag
            var finnesGrunnlagFraForeslå2 = hentOgLagreBeregningsgrunnlagTjeneste.hentSisteBeregningsgrunnlagGrunnlagEntitet(kontekst.getBehandlingId(),
                BeregningsgrunnlagTilstand.FORESLÅTT_2).isPresent();
            if (SplittForeslåBgToggle.erTogglePå() && finnesGrunnlagFraForeslå2) {
                beregningsgrunnlagKopierOgLagreTjeneste.getRyddBeregningsgrunnlag(kontekst).ryddForeslåBeregningsgrunnlag2VedTilbakeføring();
            } else {
                beregningsgrunnlagKopierOgLagreTjeneste.getRyddBeregningsgrunnlag(kontekst).ryddForeslåBeregningsgrunnlagVedTilbakeføring();
            }
        }
    }

    private BeregningsgrunnlagInputFelles getInputTjeneste(FagsakYtelseType ytelseType) {
        return beregningsgrunnlagInputProvider.getTjeneste(ytelseType);
    }
}

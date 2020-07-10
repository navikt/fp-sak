package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import no.finn.unleash.Unleash;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.BeregningsgrunnlagKopierOgLagreTjeneste;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Objects;
import java.util.stream.Collectors;

@BehandlingStegRef(kode = "VURDER_REF_BERGRUNN")
@BehandlingTypeRef("BT-004")
@FagsakYtelseTypeRef("*")
@ApplicationScoped
public class VurderRefusjonBeregningsgrunnlagSteg implements BeregningsgrunnlagSteg {
    private static final String SKAL_BRUKE_STEG = "fpsak.steg.vurder_ref_bergrunn";

    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputProvider;
    private Unleash unleash;

    protected VurderRefusjonBeregningsgrunnlagSteg() {
        // CDI Proxy
    }

    @Inject
    public VurderRefusjonBeregningsgrunnlagSteg(BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste,
                                                BehandlingRepository behandlingRepository,
                                                BeregningsgrunnlagInputProvider inputTjenesteProvider,
                                                Unleash unleash) {
        this.beregningsgrunnlagInputProvider = Objects.requireNonNull(inputTjenesteProvider, "inputTjenesteProvider");
        this.beregningsgrunnlagKopierOgLagreTjeneste = beregningsgrunnlagKopierOgLagreTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.unleash = unleash;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        if (!unleash.isEnabled(SKAL_BRUKE_STEG, false)) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var input = getInputTjeneste(behandling.getFagsakYtelseType()).lagInput(behandlingId);
        var aksjonspunkter = beregningsgrunnlagKopierOgLagreTjeneste.vurderRefusjonBeregningsgrunnlag(input);
        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunkter.stream().map(BeregningResultatMapper::map).collect(Collectors.toList()));

    }

    private BeregningsgrunnlagInputFelles getInputTjeneste(FagsakYtelseType ytelseType) {
        return beregningsgrunnlagInputProvider.getTjeneste(ytelseType);
    }
}

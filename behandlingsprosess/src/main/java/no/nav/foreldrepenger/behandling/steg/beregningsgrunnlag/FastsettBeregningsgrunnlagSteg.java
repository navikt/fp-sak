package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

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
import no.nav.foreldrepenger.domene.prosess.BeregningsgrunnlagKopierOgLagreTjeneste;

@FagsakYtelseTypeRef
@BehandlingStegRef(BehandlingStegType.FASTSETT_BEREGNINGSGRUNNLAG)
@BehandlingTypeRef
@ApplicationScoped
public class FastsettBeregningsgrunnlagSteg implements BeregningsgrunnlagSteg {

    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputProvider;

    FastsettBeregningsgrunnlagSteg() {
        // for CDI proxy
    }

    @Inject
    public FastsettBeregningsgrunnlagSteg(BehandlingRepository behandlingRepository,
                                          BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste,
                                          BeregningsgrunnlagInputProvider inputTjenesteProvider) {

        this.beregningsgrunnlagInputProvider = Objects.requireNonNull(inputTjenesteProvider, "inputTjenesteProvider");
        this.beregningsgrunnlagKopierOgLagreTjeneste = beregningsgrunnlagKopierOgLagreTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var input = getInputTjeneste(behandling.getFagsakYtelseType()).lagInput(behandlingId);
        beregningsgrunnlagKopierOgLagreTjeneste.fastsettBeregningsgrunnlag(input);
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    @Override
    public void vedHoppOverFramover(BehandlingskontrollKontekst kontekst,
                                    BehandlingStegModell modell,
                                    BehandlingStegType fraSteg,
                                    BehandlingStegType tilSteg) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        if (tilSteg.equals(BehandlingStegType.SØKNADSFRIST_FORELDREPENGER) && behandling.erRevurdering()) {
            // Kopier beregningsgrunnlag fra original, da uttaksresultat avhenger av denne
            behandling.getOriginalBehandlingId()
                .ifPresent(originalId -> beregningsgrunnlagKopierOgLagreTjeneste.kopierBeregningsresultatFraOriginalBehandling(originalId,
                    behandling.getId()));

        }
    }

    private BeregningsgrunnlagInputFelles getInputTjeneste(FagsakYtelseType ytelseType) {
        return beregningsgrunnlagInputProvider.getTjeneste(ytelseType);
    }
}

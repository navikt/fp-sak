package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.BeregningsgrunnlagKopierOgLagreTjeneste;

@FagsakYtelseTypeRef("*")
@BehandlingStegRef(kode = "FAST_BERGRUNN")
@BehandlingTypeRef
@ApplicationScoped
public class FastsettBeregningsgrunnlagSteg implements BeregningsgrunnlagSteg {

    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    private RyddDekningsgradTjeneste ryddDekningsgradTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputProvider;

    protected FastsettBeregningsgrunnlagSteg() {
        // for CDI proxy
    }

    @Inject
    public FastsettBeregningsgrunnlagSteg(BehandlingRepository behandlingRepository,
                                          BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste,
                                          RyddDekningsgradTjeneste ryddDekningsgradTjeneste,
                                          BehandlingsresultatRepository behandlingsresultatRepository,
                                          BeregningsgrunnlagInputProvider inputTjenesteProvider) {

        this.beregningsgrunnlagInputProvider = Objects.requireNonNull(inputTjenesteProvider, "inputTjenesteProvider");
        this.beregningsgrunnlagKopierOgLagreTjeneste = beregningsgrunnlagKopierOgLagreTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.ryddDekningsgradTjeneste = ryddDekningsgradTjeneste;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var input = getInputTjeneste(behandling.getFagsakYtelseType()).lagInput(behandlingId);
        beregningsgrunnlagKopierOgLagreTjeneste.fastsettBeregningsgrunnlag(input);
        return BehandleStegResultat.utførtMedAksjonspunktResultater(Collections.emptyList());
    }


    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        if (behandlingsresultatRepository.hent(behandling.getId()).isEndretDekningsgrad()) {
            ryddDekningsgradTjeneste.rydd(behandling);
        }
    }

    @Override
    public void vedHoppOverFramover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType fraSteg, BehandlingStegType tilSteg) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        if (tilSteg.equals(BehandlingStegType.SØKNADSFRIST_FORELDREPENGER)) {
            if (behandling.erRevurdering()) {
                // Kopier beregningsgrunnlag fra original, da uttaksresultat avhenger av denne
                behandling.getOriginalBehandlingId()
                    .ifPresent(originalId -> beregningsgrunnlagKopierOgLagreTjeneste.kopierBeregningsresultatFraOriginalBehandling(originalId, behandling.getId()));
            }
        }
        ryddDekningsgrad(behandling);
    }

    private void ryddDekningsgrad(Behandling behandling) {
        Optional<Behandlingsresultat> behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId());
        if (behandlingsresultat.isPresent() && behandlingsresultat.get().isEndretDekningsgrad()) {
            ryddDekningsgradTjeneste.rydd(behandling);
        }
    }

    private BeregningsgrunnlagInputFelles getInputTjeneste(FagsakYtelseType ytelseType) {
        return beregningsgrunnlagInputProvider.getTjeneste(ytelseType);
    }
}

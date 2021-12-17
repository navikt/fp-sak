package no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.RisikoklassifiseringRepository;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.KontrollresultatWrapper;
import no.nav.foreldrepenger.kontrakter.risk.v1.HentRisikovurderingDto;
import no.nav.foreldrepenger.kontrakter.risk.v1.LagreFaresignalVurderingDto;

import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.FaresignalVurdering;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.Kontrollresultat;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.RisikoklassifiseringEntitet;
import no.nav.foreldrepenger.domene.risikoklassifisering.json.KontrollresultatMapper;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.FaresignalWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class RisikovurderingTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(RisikovurderingTjeneste.class);

    private RisikoklassifiseringRepository risikoklassifiseringRepository;
    private BehandlingRepository behandlingRepository;
    private FpriskTjeneste fpriskTjeneste;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;


    public RisikovurderingTjeneste() {
        // CDI
    }

    @Inject
    public RisikovurderingTjeneste(RisikoklassifiseringRepository risikoklassifiseringRepository,
                                   BehandlingRepository behandlingRepository,
                                   FpriskTjeneste fpriskTjeneste,
                                   BehandlingskontrollTjeneste behandlingskontrollTjeneste) {
        this.risikoklassifiseringRepository = risikoklassifiseringRepository;
        this.behandlingRepository = behandlingRepository;
        this.fpriskTjeneste = fpriskTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
    }

    public boolean behandlingHarBlittRisikoklassifisert(BehandlingReferanse referanse) {
        return hentFaresignalerForBehandling(referanse).isPresent();
    }

    /**
     *
     * @deprecated kan fjernes når risikosaker er migrert
     */
    public void lagreKontrollresultat(KontrollresultatWrapper resultatWrapper) {
        var behandling = behandlingRepository.hentBehandlingHvisFinnes(resultatWrapper.getBehandlingUuid());
        behandling.ifPresent(beh -> {
            var eksisterende = risikoklassifiseringRepository.hentRisikoklassifiseringForBehandling(beh.getId());
            var isNyEvalueringEndretTilHøy = erNyEvalueringTilHøy(resultatWrapper, eksisterende);
            if (eksisterende.isEmpty()) {
                lagre(resultatWrapper, beh);
                if (Kontrollresultat.HØY.equals(resultatWrapper.getKontrollresultatkode()) && behandlingHarPassertVurderFaresignaler(beh)) {
                    LOG.info("Kontrollresultat HØY motatt for behandling med id {}. Behandlingens status var {}", beh.getId(), beh.getStatus().getKode());
                }
            } else if (isNyEvalueringEndretTilHøy) {
                if (!behandlingHarPassertVurderFaresignaler(beh) && !erVurdert(eksisterende)) {
                    lagre(resultatWrapper, beh);
                    LOG.info("Nytt Kontrollresultat HØY oppdatert for behandling med id {}. Behandlingens status var {}", beh.getId(), beh.getStatus().getKode());
                } else  {
                    LOG.info("Oppdatert Kontrollresultat HØY motatt for sak {}", beh.getFagsak().getSaksnummer().getVerdi());
                }
            }
        });
    }

    private boolean behandlingHarPassertVurderFaresignaler(Behandling beh) {
        if (beh.erStatusFerdigbehandlet()) {
            return true;
        }
        return behandlingskontrollTjeneste.erStegPassert(beh, BehandlingStegType.VURDER_FARESIGNALER);
    }

    private boolean erNyEvalueringTilHøy(KontrollresultatWrapper resultatWrapper, Optional<RisikoklassifiseringEntitet> eksisterende) {
        var eksisterendeEllerHøy = eksisterende.map(RisikoklassifiseringEntitet::getKontrollresultat).orElse(Kontrollresultat.HØY);
        return Kontrollresultat.HØY.equals(resultatWrapper.getKontrollresultatkode()) && !Kontrollresultat.HØY.equals(eksisterendeEllerHøy);
    }

    private boolean erVurdert(Optional<RisikoklassifiseringEntitet> eksisterende) {
        return eksisterende.map(RisikoklassifiseringEntitet::getFaresignalVurdering)
            .filter(v -> !FaresignalVurdering.UDEFINERT.equals(v)).isPresent();
    }


    public Optional<FaresignalWrapper> hentRisikoklassifisering(BehandlingReferanse referanse) {
        // Tidlig return for å spare oss unødige restkall og db oppslag, kun førstegangsbehandlinger blir klassifisert.
        if (!referanse.getBehandlingType().equals(BehandlingType.FØRSTEGANGSSØKNAD)) {
            return Optional.empty();
        }

        var klassifiseringFraRiskOpt = hentFaresignalerForBehandling(referanse);

        // Må gjøres frem til faresignalvurderinger i fpsak er migrert til fprisk
        if (klassifiseringFraRiskOpt.filter(res -> res.kontrollresultat().equals(Kontrollresultat.HØY) && res.faresignalVurdering() == null).isPresent()) {
            return Optional.of(leggPåFaresignalvurderingFraFpsak(klassifiseringFraRiskOpt.get(), referanse));
        }

        return klassifiseringFraRiskOpt;
    }

    public boolean skalVurdereFaresignaler(BehandlingReferanse referanse) {
        Objects.requireNonNull(referanse, "referanse");
        var wrapper = hentRisikoklassifisering(referanse);
        return wrapper.map(this::erHøyRisiko).orElse(false);
    }

    public void lagreVurderingAvFaresignalerForBehandling(BehandlingReferanse referanse, FaresignalVurdering vurdering) {
        Objects.requireNonNull(referanse, "referanse");
        // Send svar til fprisk
        var request = new LagreFaresignalVurderingDto(referanse.getBehandlingUuid(), KontrollresultatMapper.mapFaresignalvurderingTilKontrakt(vurdering));
        fpriskTjeneste.sendRisikovurderingTilFprisk(request);
    }

    private FaresignalWrapper leggPåFaresignalvurderingFraFpsak(FaresignalWrapper resultatFraFprisk, BehandlingReferanse ref) {
        var klassifiseringFraFpsakOpt = hentRisikoklassifiseringFraFpsak(ref.getBehandlingId());
        if (klassifiseringFraFpsakOpt.filter(kl -> kl.getFaresignalVurdering() != null).isEmpty()) {
            return resultatFraFprisk;
        }
        return new FaresignalWrapper(resultatFraFprisk.kontrollresultat(),
            klassifiseringFraFpsakOpt.get().getFaresignalVurdering(),
            resultatFraFprisk.medlemskapFaresignaler(),
            resultatFraFprisk.iayFaresignaler());
    }

    private Optional<RisikoklassifiseringEntitet> hentRisikoklassifiseringFraFpsak(Long behandlingId) {
        return risikoklassifiseringRepository.hentRisikoklassifiseringForBehandling(behandlingId);
    }

    private boolean erHøyRisiko(FaresignalWrapper wrapper) {
        return Objects.equals(wrapper.kontrollresultat(), Kontrollresultat.HØY);
    }

    private Optional<FaresignalWrapper> hentFaresignalerForBehandling(BehandlingReferanse ref) {
        var request = new HentRisikovurderingDto(ref.getBehandlingUuid());
        var faresignalerRespons = fpriskTjeneste.hentFaresignalerForBehandling(request);
        if (faresignalerRespons.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(KontrollresultatMapper.fraFaresignalRespons(faresignalerRespons.get()));
    }

    private void lagre(KontrollresultatWrapper resultatWrapper, Behandling beh) {
        var entitet = RisikoklassifiseringEntitet.builder()
            .medKontrollresultat(resultatWrapper.getKontrollresultatkode())
            .buildFor(beh.getId());
        risikoklassifiseringRepository.lagreRisikoklassifisering(entitet, beh.getId());
    }

}

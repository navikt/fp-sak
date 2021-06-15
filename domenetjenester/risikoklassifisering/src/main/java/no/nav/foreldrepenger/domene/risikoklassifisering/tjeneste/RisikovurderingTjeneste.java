package no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.FaresignalVurdering;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.Kontrollresultat;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.RisikoklassifiseringEntitet;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.RisikoklassifiseringRepository;
import no.nav.foreldrepenger.domene.risikoklassifisering.json.KontrollresultatMapper;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.FaresignalWrapper;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.KontrollresultatWrapper;

@ApplicationScoped
public class RisikovurderingTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(RisikovurderingTjeneste.class);

    private RisikoklassifiseringRepository risikoklassifiseringRepository;
    private BehandlingRepository behandlingRepository;
    private HentFaresignalerTjeneste hentFaresignalerTjeneste;
    private KontrollresultatMapper kontrollresultatMapper;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    public RisikovurderingTjeneste() {
        // CDI
    }

    @Inject
    public RisikovurderingTjeneste(RisikoklassifiseringRepository risikoklassifiseringRepository,
                                   BehandlingRepository behandlingRepository,
                                   HentFaresignalerTjeneste hentFaresignalerTjeneste,
                                   KontrollresultatMapper kontrollresultatMapper,
                                   BehandlingskontrollTjeneste behandlingskontrollTjeneste) {
        this.risikoklassifiseringRepository = risikoklassifiseringRepository;
        this.behandlingRepository = behandlingRepository;
        this.hentFaresignalerTjeneste = hentFaresignalerTjeneste;
        this.kontrollresultatMapper = kontrollresultatMapper;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
    }

    public boolean behandlingHarBlittRisikoklassifisert(Long behandlingId) {
        return risikoklassifiseringRepository.hentRisikoklassifiseringForBehandling(behandlingId).isPresent();
    }

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

    public Optional<RisikoklassifiseringEntitet> hentRisikoklassifiseringForBehandling(Long behandlingId) {
        return risikoklassifiseringRepository.hentRisikoklassifiseringForBehandling(behandlingId);
    }

    public Optional<FaresignalWrapper> finnKontrollresultatForBehandling(Behandling behandling) {
        var resultat = risikoklassifiseringRepository.hentRisikoklassifiseringForBehandling(behandling.getId());
        if (resultat.isPresent()) {
            if (erHøyRisiko(resultat.get())) {
                return hentFaresignalerForBehandling(behandling);
            }
            return lagKontrollresultatIkkeHøyRisiko(resultat.get());
        }
        return Optional.empty();
    }

    public boolean skalVurdereFaresignaler(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        var resultat = risikoklassifiseringRepository.hentRisikoklassifiseringForBehandling(behandlingId);
        return resultat.map(this::erHøyRisiko).orElse(false);
    }

    public void lagreVurderingAvFaresignalerForBehandling(Long behandlingId, FaresignalVurdering vurdering) {
        Objects.requireNonNull(behandlingId, "behandlingId");

        risikoklassifiseringRepository.lagreVurderingAvFaresignalerForRisikoklassifisering(vurdering, behandlingId);
    }

    private Optional<FaresignalWrapper> lagKontrollresultatIkkeHøyRisiko(RisikoklassifiseringEntitet risikoklassifiseringEntitet) {
        return Optional.of(FaresignalWrapper
            .builder()
            .medKontrollresultat(risikoklassifiseringEntitet.getKontrollresultat())
            .build());
    }

    private boolean erHøyRisiko(RisikoklassifiseringEntitet risikoklassifiseringEntitet) {
        return Objects.equals(risikoklassifiseringEntitet.getKontrollresultat(), Kontrollresultat.HØY);
    }

    private Optional<FaresignalWrapper> hentFaresignalerForBehandling(Behandling behandling) {
        var faresignalerRespons = hentFaresignalerTjeneste.hentFaresignalerForBehandling(behandling.getUuid());
        if (faresignalerRespons.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(kontrollresultatMapper.fraFaresignalRespons(faresignalerRespons.get()));
    }


    private void lagre(KontrollresultatWrapper resultatWrapper, Behandling beh) {
        var entitet = RisikoklassifiseringEntitet.builder()
            .medKontrollresultat(resultatWrapper.getKontrollresultatkode())
            .buildFor(beh.getId());
        risikoklassifiseringRepository.lagreRisikoklassifisering(entitet, beh.getId());
    }
}

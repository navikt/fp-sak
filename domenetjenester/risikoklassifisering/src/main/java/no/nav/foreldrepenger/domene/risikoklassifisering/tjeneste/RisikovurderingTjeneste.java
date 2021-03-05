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
import no.nav.foreldrepenger.domene.risikoklassifisering.json.KontrollresultatMapper;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.FaresignalVurdering;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.Kontrollresultat;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.RisikoklassifiseringEntitet;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.RisikoklassifiseringRepository;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.FaresignalWrapper;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.KontrollresultatWrapper;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.rest.FaresignalerRespons;

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
        Optional<Behandling> behandling = behandlingRepository.hentBehandlingHvisFinnes(resultatWrapper.getBehandlingUuid());
        behandling.ifPresent(beh -> {
            if (!behandlingHarBlittRisikoklassifisert(beh.getId())) {
                lagre(resultatWrapper, beh);
                if (Kontrollresultat.HØY.equals(resultatWrapper.getKontrollresultatkode()) && behandlingHarPassertVurderFaresignaler(beh)) {
                    LOG.info("Kontrollresultat HØY motatt for behandling med id {}. Behandlingens status var {}", beh.getId(), beh.getStatus().getKode());
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

    public Optional<RisikoklassifiseringEntitet> hentRisikoklassifiseringForBehandling(Long behandlingId) {
        return risikoklassifiseringRepository.hentRisikoklassifiseringForBehandling(behandlingId);
    }

    public Optional<FaresignalWrapper> finnKontrollresultatForBehandling(Behandling behandling) {
        Optional<RisikoklassifiseringEntitet> resultat = risikoklassifiseringRepository.hentRisikoklassifiseringForBehandling(behandling.getId());
        if (resultat.isPresent()) {
            if (erHøyRisiko(resultat.get())) {
                return hentFaresignalerForBehandling(behandling);
            } else {
                return lagKontrollresultatIkkeHøyRisiko(resultat.get());
            }
        }
        return Optional.empty();
    }

    public boolean skalVurdereFaresignaler(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        Optional<RisikoklassifiseringEntitet> resultat = risikoklassifiseringRepository.hentRisikoklassifiseringForBehandling(behandlingId);
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
        Optional<FaresignalerRespons> faresignalerRespons = hentFaresignalerTjeneste.hentFaresignalerForBehandling(behandling.getUuid());
        if (faresignalerRespons.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(kontrollresultatMapper.fraFaresignalRespons(faresignalerRespons.get()));
    }


    private void lagre(KontrollresultatWrapper resultatWrapper, Behandling beh) {
        RisikoklassifiseringEntitet entitet = RisikoklassifiseringEntitet.builder()
            .medKontrollresultat(resultatWrapper.getKontrollresultatkode())
            .buildFor(beh.getId());
        risikoklassifiseringRepository.lagreRisikoklassifisering(entitet, beh.getId());
    }
}

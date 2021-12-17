package no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.RisikoklassifiseringRepository;
import no.nav.foreldrepenger.kontrakter.risk.v1.HentRisikovurderingDto;
import no.nav.foreldrepenger.kontrakter.risk.v1.LagreFaresignalVurderingDto;

import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.FaresignalVurdering;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.Kontrollresultat;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.RisikoklassifiseringEntitet;
import no.nav.foreldrepenger.domene.risikoklassifisering.json.KontrollresultatMapper;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.FaresignalWrapper;

@ApplicationScoped
public class RisikovurderingTjeneste {
    private RisikoklassifiseringRepository risikoklassifiseringRepository;
    private FpriskTjeneste fpriskTjeneste;

    public RisikovurderingTjeneste() {
        // CDI
    }

    @Inject
    public RisikovurderingTjeneste(RisikoklassifiseringRepository risikoklassifiseringRepository,
                                   FpriskTjeneste fpriskTjeneste) {
        this.risikoklassifiseringRepository = risikoklassifiseringRepository;
        this.fpriskTjeneste = fpriskTjeneste;
    }

    public boolean behandlingHarBlittRisikoklassifisert(BehandlingReferanse referanse) {
        return hentFaresignalerForBehandling(referanse).isPresent();
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
}

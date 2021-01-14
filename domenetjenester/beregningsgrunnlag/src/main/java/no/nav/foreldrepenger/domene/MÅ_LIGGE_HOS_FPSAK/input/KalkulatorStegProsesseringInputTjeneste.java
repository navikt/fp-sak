package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.input;

import static no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.input.MapStegTilTilstand.mapTilKalkulatorStegTilstand;
import static no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.input.MapStegTilTilstand.mapTilKalkulatorStegUtTilstand;
import static no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.input.MapStegTilTilstand.mapTilStegTilstand;
import static no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.input.MapStegTilTilstand.mapTilStegUtTilstand;

import java.time.MonthDay;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.Grunnbeløp;
import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.input.FaktaOmBeregningInput;
import no.nav.folketrygdloven.kalkulator.input.FastsettBeregningsaktiviteterInput;
import no.nav.folketrygdloven.kalkulator.input.FordelBeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.input.ForeslåBeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.input.FullføreBeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.input.StegProsesseringInput;
import no.nav.folketrygdloven.kalkulator.input.VurderRefusjonBeregningsgrunnlagInput;
import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.GrunnbeløpTjeneste;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.KalkulusKonfigInjecter;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus.BehandlingslagerTilKalkulusMapper;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.typer.Beløp;

@ApplicationScoped
public class KalkulatorStegProsesseringInputTjeneste {

    public static final MonthDay ENDRING_AV_GRUNNBELØP = MonthDay.of(5, 1);

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private BehandlingRepository behandlingRepository;
    private BeregningTilInputTjeneste beregningTilInputTjeneste;
    private GrunnbeløpTjeneste grunnbeløpTjeneste;
    private KalkulusKonfigInjecter kalkulusKonfigInjecter;


    public KalkulatorStegProsesseringInputTjeneste() {
        // CDI
    }

    @Inject
    public KalkulatorStegProsesseringInputTjeneste(BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                                                   BehandlingRepository behandlingRepository,
                                                   BeregningTilInputTjeneste beregningTilInputTjeneste,
                                                   GrunnbeløpTjeneste grunnbeløpTjeneste,
                                                   KalkulusKonfigInjecter kalkulusKonfigInjecter) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.behandlingRepository = behandlingRepository;
        this.beregningTilInputTjeneste = beregningTilInputTjeneste;
        this.grunnbeløpTjeneste = grunnbeløpTjeneste;
        this.kalkulusKonfigInjecter = kalkulusKonfigInjecter;
    }

    public FastsettBeregningsaktiviteterInput lagStartInput(Long behandlingId,BeregningsgrunnlagInput input) {
        // Vurder om vi skal begynne å ta inn koblingId for originalbehandling ved revurdering
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Optional<BeregningsgrunnlagGrunnlagEntitet> grunnlagFraSteg = beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(behandling.getId(), behandling.getOriginalBehandlingId(), BeregningsgrunnlagTilstand.OPPRETTET);
        Optional<BeregningsgrunnlagGrunnlagEntitet> grunnlagFraStegUt = finnForrigeAvklarteGrunnlagForTilstand(behandling, grunnlagFraSteg, BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER);
        kalkulusKonfigInjecter.leggTilKonfigverdier(input);
        kalkulusKonfigInjecter.leggTilFeatureToggles(input);
        StegProsesseringInput stegProsesseringInput = new StegProsesseringInput(input, no.nav.folketrygdloven.kalkulus.kodeverk.BeregningsgrunnlagTilstand.OPPRETTET)
            .medForrigeGrunnlagFraStegUt(grunnlagFraStegUt.map(BehandlingslagerTilKalkulusMapper::mapGrunnlag).orElse(null))
            .medForrigeGrunnlagFraSteg(grunnlagFraSteg.map(BehandlingslagerTilKalkulusMapper::mapGrunnlag).orElse(null))
            .medStegUtTilstand(no.nav.folketrygdloven.kalkulus.kodeverk.BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER);
        return new FastsettBeregningsaktiviteterInput(stegProsesseringInput).medGrunnbeløpsatser(finnSatser());
    }

    public StegProsesseringInput lagFortsettInput(Long behandlingId, BeregningsgrunnlagInput input, BehandlingStegType stegType) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        return mapStegInput(behandling, input, stegType);
    }

    public StegProsesseringInput mapStegInput(Behandling behandling,
                                              BeregningsgrunnlagInput input,
                                              BehandlingStegType stegType) {
        StegProsesseringInput stegProsesseringInput = lagStegProsesseringInput(behandling, input, stegType);
        if (stegType.equals(BehandlingStegType.KONTROLLER_FAKTA_BEREGNING)) {
            return new FaktaOmBeregningInput(stegProsesseringInput).medGrunnbeløpsatser(finnSatser());
        } else if (stegType.equals(BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG)) {
            return lagInputForeslå(stegProsesseringInput);
        } else if (stegType.equals(BehandlingStegType.VURDER_REF_BERGRUNN)) {
            return lagInputVurderRefusjon(stegProsesseringInput, behandling);
        } else if (stegType.equals(BehandlingStegType.FORDEL_BEREGNINGSGRUNNLAG)) {
            Optional<BeregningsgrunnlagGrunnlagEntitet> førsteFastsatteGrunnlagEntitet = finnFørsteFastsatteGrunnlagEtterEndringAvGrunnbeløp(behandling.getId());
            return lagInputFordel(stegProsesseringInput, førsteFastsatteGrunnlagEntitet);
        } else if (stegType.equals(BehandlingStegType.FASTSETT_BEREGNINGSGRUNNLAG)) {
            Optional<BeregningsgrunnlagGrunnlagEntitet> førsteFastsatteGrunnlagEntitet = finnFørsteFastsatteGrunnlagEtterEndringAvGrunnbeløp(behandling.getId());
            return lagInputFullføre(stegProsesseringInput, førsteFastsatteGrunnlagEntitet);
        }
        return stegProsesseringInput;
    }

    private StegProsesseringInput lagInputVurderRefusjon(StegProsesseringInput stegProsesseringInput, Behandling behandling) {
        Optional<BeregningsgrunnlagGrunnlagEntitet> førsteFastsatteGrunnlagEntitet = finnFørsteFastsatteGrunnlagEtterEndringAvGrunnbeløp(behandling.getId());
        var vurderRefusjonBeregningsgrunnlagInput = new VurderRefusjonBeregningsgrunnlagInput(stegProsesseringInput);
        if (førsteFastsatteGrunnlagEntitet.isPresent()) {
            vurderRefusjonBeregningsgrunnlagInput = førsteFastsatteGrunnlagEntitet.get().getBeregningsgrunnlag()
                .map(BeregningsgrunnlagEntitet::getGrunnbeløp)
                .map(Beløp::getVerdi)
                .map(vurderRefusjonBeregningsgrunnlagInput::medUregulertGrunnbeløp)
                .orElse(vurderRefusjonBeregningsgrunnlagInput);
        }
        Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag = behandling.getOriginalBehandlingId().flatMap(beh -> beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(beh));
        if (forrigeGrunnlag.isPresent()) {
            // Trenger ikke vite hvilke andeler i orginalbehandling som hadde inntektsmeldinger
            vurderRefusjonBeregningsgrunnlagInput = vurderRefusjonBeregningsgrunnlagInput
                .medBeregningsgrunnlagGrunnlagFraForrigeBehandling(BehandlingslagerTilKalkulusMapper.mapGrunnlag(forrigeGrunnlag.get()));
        }

        return vurderRefusjonBeregningsgrunnlagInput;

    }

    private StegProsesseringInput lagStegProsesseringInput(Behandling behandling, BeregningsgrunnlagInput input, BehandlingStegType stegType) {
        BeregningsgrunnlagInput inputMedBG = beregningTilInputTjeneste.lagInputMedVerdierFraBeregning(input);
        Optional<BeregningsgrunnlagGrunnlagEntitet> grunnlagFraSteg = beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(behandling.getId(), Optional.empty(), mapTilStegTilstand(stegType));
        Optional<BeregningsgrunnlagGrunnlagEntitet> grunnlagFraStegUt = finnForrigeAvklartGrunnlagHvisFinnes(behandling, grunnlagFraSteg, stegType);
        return new StegProsesseringInput(inputMedBG, mapTilKalkulatorStegTilstand(stegType))
            .medForrigeGrunnlagFraStegUt(grunnlagFraStegUt.map(BehandlingslagerTilKalkulusMapper::mapGrunnlag).orElse(null))
            .medForrigeGrunnlagFraSteg(grunnlagFraSteg.map(BehandlingslagerTilKalkulusMapper::mapGrunnlag).orElse(null))
            .medStegUtTilstand(mapTilKalkulatorStegUtTilstand(stegType).orElse(null));
    }

    private ForeslåBeregningsgrunnlagInput lagInputForeslå(StegProsesseringInput stegProsesseringInput) {
        var foreslåBeregningsgrunnlagInput = new ForeslåBeregningsgrunnlagInput(stegProsesseringInput);
        return foreslåBeregningsgrunnlagInput.medGrunnbeløpsatser(finnSatser());
    }

    private FordelBeregningsgrunnlagInput lagInputFordel(StegProsesseringInput stegProsesseringInput, Optional<BeregningsgrunnlagGrunnlagEntitet> førsteFastsatteGrunnlagEntitet) {
        var fordelBeregningsgrunnlagInput = new FordelBeregningsgrunnlagInput(stegProsesseringInput);
        if (førsteFastsatteGrunnlagEntitet.isPresent()) {
            fordelBeregningsgrunnlagInput = førsteFastsatteGrunnlagEntitet.get().getBeregningsgrunnlag()
                .map(BeregningsgrunnlagEntitet::getGrunnbeløp)
                .map(Beløp::getVerdi)
                .map(fordelBeregningsgrunnlagInput::medUregulertGrunnbeløp)
                .orElse(fordelBeregningsgrunnlagInput);
        }
        return fordelBeregningsgrunnlagInput;
    }

    private FullføreBeregningsgrunnlagInput lagInputFullføre(StegProsesseringInput stegProsesseringInput, Optional<BeregningsgrunnlagGrunnlagEntitet> førsteFastsatteGrunnlagEntitet) {
        var fullføreBeregningsgrunnlagInput = new FullføreBeregningsgrunnlagInput(stegProsesseringInput);
        if (førsteFastsatteGrunnlagEntitet.isPresent()) {
            fullføreBeregningsgrunnlagInput = førsteFastsatteGrunnlagEntitet.get().getBeregningsgrunnlag()
                .map(BeregningsgrunnlagEntitet::getGrunnbeløp)
                .map(Beløp::getVerdi)
                .map(fullføreBeregningsgrunnlagInput::medUregulertGrunnbeløp)
                .orElse(fullføreBeregningsgrunnlagInput);
        }
        return fullføreBeregningsgrunnlagInput;
    }

    private List<Grunnbeløp> finnSatser() {
        return grunnbeløpTjeneste.mapGrunnbeløpSatser();
    }

    private Optional<BeregningsgrunnlagGrunnlagEntitet> finnFørsteFastsatteGrunnlagEtterEndringAvGrunnbeløp(Long behandlingId) {
        Long fagsakId = behandlingRepository.hentBehandling(behandlingId).getFagsak().getId();
        List<Behandling> behandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(fagsakId);
        return behandlinger.stream()
            .filter(b -> b.getStatus().erFerdigbehandletStatus())
            .map(kobling -> beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitet(kobling.getId(), no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand.FASTSATT))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(gr -> MonthDay.from(gr.getBeregningsgrunnlag().orElseThrow(() -> new IllegalStateException("Skal ha beregningsgrunnlag"))
                .getSkjæringstidspunkt()).isAfter(ENDRING_AV_GRUNNBELØP))
            .min(Comparator.comparing(BaseEntitet::getOpprettetTidspunkt));
    }


    private Optional<BeregningsgrunnlagGrunnlagEntitet> finnForrigeAvklartGrunnlagHvisFinnes(Behandling behandling,
                                                                                             Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlagFraSteg,
                                                                                             BehandlingStegType stegType) {
        var tilstandUt = mapTilStegUtTilstand(stegType);
        if (tilstandUt.isEmpty()) {
            return Optional.empty();
        }
        return finnForrigeAvklarteGrunnlagForTilstand(behandling, forrigeGrunnlagFraSteg, tilstandUt.get());
    }

    private Optional<BeregningsgrunnlagGrunnlagEntitet> finnForrigeAvklarteGrunnlagForTilstand(Behandling behandling,
                                                                                               Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlagFraSteg,
                                                                                               BeregningsgrunnlagTilstand beregningsgrunnlagTilstand) {
        if (forrigeGrunnlagFraSteg.isEmpty()) {
            return Optional.empty();
        }
        return beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlingerEtterTidspunkt(behandling.getId(), behandling.getOriginalBehandlingId(),
            forrigeGrunnlagFraSteg.get().getOpprettetTidspunkt(), beregningsgrunnlagTilstand);
    }

}

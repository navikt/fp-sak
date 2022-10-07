package no.nav.foreldrepenger.domene.input;

import static no.nav.foreldrepenger.domene.input.MapStegTilTilstand.mapTilKalkulatorStegTilstand;
import static no.nav.foreldrepenger.domene.input.MapStegTilTilstand.mapTilKalkulatorStegUtTilstand;
import static no.nav.foreldrepenger.domene.input.MapStegTilTilstand.mapTilStegTilstand;
import static no.nav.foreldrepenger.domene.input.MapStegTilTilstand.mapTilStegUtTilstand;

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
import no.nav.folketrygdloven.kalkulator.input.ForeslåBesteberegningInput;
import no.nav.folketrygdloven.kalkulator.input.FortsettForeslåBeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.input.FullføreBeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.input.StegProsesseringInput;
import no.nav.folketrygdloven.kalkulator.input.VurderBeregningsgrunnlagvilkårInput;
import no.nav.folketrygdloven.kalkulator.input.VurderRefusjonBeregningsgrunnlagInput;
import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.prosess.GrunnbeløpTjeneste;
import no.nav.foreldrepenger.domene.prosess.KalkulusKonfigInjecter;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.BehandlingslagerTilKalkulusMapper;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
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

    public FastsettBeregningsaktiviteterInput lagStartInput(Long behandlingId, BeregningsgrunnlagInput input) {
        // Vurder om vi skal begynne å ta inn koblingId for originalbehandling ved revurdering
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var grunnlagFraSteg = beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(
            behandling.getId(), behandling.getOriginalBehandlingId(), BeregningsgrunnlagTilstand.OPPRETTET);
        var grunnlagFraStegUt = finnForrigeAvklarteGrunnlagForTilstand(behandling, grunnlagFraSteg,
            BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER);
        kalkulusKonfigInjecter.leggTilKonfigverdier(input);
        kalkulusKonfigInjecter.leggTilFeatureToggles(input);
        var stegProsesseringInput = new StegProsesseringInput(input,
            no.nav.folketrygdloven.kalkulus.kodeverk.BeregningsgrunnlagTilstand.OPPRETTET).medForrigeGrunnlagFraStegUt(
            grunnlagFraStegUt.map(BehandlingslagerTilKalkulusMapper::mapGrunnlag).orElse(null))
            .medForrigeGrunnlagFraSteg(grunnlagFraSteg.map(BehandlingslagerTilKalkulusMapper::mapGrunnlag).orElse(null))
            .medStegUtTilstand(
                no.nav.folketrygdloven.kalkulus.kodeverk.BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER);
        return new FastsettBeregningsaktiviteterInput(stegProsesseringInput).medGrunnbeløpsatser(finnSatser());
    }

    public StegProsesseringInput lagFortsettInput(Long behandlingId,
                                                  BeregningsgrunnlagInput input,
                                                  BehandlingStegType stegType) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        return mapStegInput(behandling, input, stegType);
    }

    public StegProsesseringInput mapStegInput(Behandling behandling,
                                              BeregningsgrunnlagInput input,
                                              BehandlingStegType stegType) {
        var stegProsesseringInput = lagStegProsesseringInput(behandling, input, stegType);
        if (stegType.equals(BehandlingStegType.KONTROLLER_FAKTA_BEREGNING)) {
            return new FaktaOmBeregningInput(stegProsesseringInput).medGrunnbeløpsatser(finnSatser());
        }
        if (stegType.equals(BehandlingStegType.FORESLÅ_BESTEBEREGNING)) {
            return lagInputForeslåBesteberegning(stegProsesseringInput);
        }
        if (stegType.equals(BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG)) {
            return lagInputForeslå(stegProsesseringInput);
        }
        if (stegType.equals(BehandlingStegType.FORTSETT_FORESLÅ_BEREGNINGSGRUNNLAG)) {
            return lagInputFortsettForeslå(stegProsesseringInput);
        }
        if (stegType.equals(BehandlingStegType.VURDER_VILKAR_BERGRUNN)) {
            Optional<BeregningsgrunnlagGrunnlagEntitet> førsteFastsatteGrunnlagEntitet = finnFørsteFastsatteGrunnlagEtterEndringAvGrunnbeløp(behandling.getId());
            return lagInputVurderVilkår(stegProsesseringInput, førsteFastsatteGrunnlagEntitet);
        }
        if (stegType.equals(BehandlingStegType.VURDER_REF_BERGRUNN)) {
            return lagInputVurderRefusjon(stegProsesseringInput, behandling);
        }
        if (stegType.equals(BehandlingStegType.FORDEL_BEREGNINGSGRUNNLAG)) {
            var førsteFastsatteGrunnlagEntitet = finnFørsteFastsatteGrunnlagEtterEndringAvGrunnbeløp(
                behandling.getId());
            return lagInputFordel(stegProsesseringInput, førsteFastsatteGrunnlagEntitet);
        }
        if (stegType.equals(BehandlingStegType.FASTSETT_BEREGNINGSGRUNNLAG)) {
            var førsteFastsatteGrunnlagEntitet = finnFørsteFastsatteGrunnlagEtterEndringAvGrunnbeløp(
                behandling.getId());
            return lagInputFullføre(stegProsesseringInput, førsteFastsatteGrunnlagEntitet);
        }
        return stegProsesseringInput;
    }

    private VurderBeregningsgrunnlagvilkårInput lagInputVurderVilkår(StegProsesseringInput stegProsesseringInput,
                                                                     Optional<BeregningsgrunnlagGrunnlagEntitet> førsteFastsatteGrunnlagEntitet) {
        var vurderVilkårInput = new VurderBeregningsgrunnlagvilkårInput(stegProsesseringInput);
        if (førsteFastsatteGrunnlagEntitet.isPresent()) {
            vurderVilkårInput = førsteFastsatteGrunnlagEntitet.get().getBeregningsgrunnlag()
                .map(BeregningsgrunnlagEntitet::getGrunnbeløp)
                .map(Beløp::getVerdi)
                .map(vurderVilkårInput::medUregulertGrunnbeløp)
                .orElse(vurderVilkårInput);
        }
        return vurderVilkårInput;
    }

    private StegProsesseringInput lagInputVurderRefusjon(StegProsesseringInput stegProsesseringInput,
                                                         Behandling behandling) {
        var førsteFastsatteGrunnlagEntitet = finnFørsteFastsatteGrunnlagEtterEndringAvGrunnbeløp(behandling.getId());
        var vurderRefusjonBeregningsgrunnlagInput = new VurderRefusjonBeregningsgrunnlagInput(stegProsesseringInput);
        if (førsteFastsatteGrunnlagEntitet.isPresent()) {
            vurderRefusjonBeregningsgrunnlagInput = førsteFastsatteGrunnlagEntitet.get()
                .getBeregningsgrunnlag()
                .map(BeregningsgrunnlagEntitet::getGrunnbeløp)
                .map(Beløp::getVerdi)
                .map(vurderRefusjonBeregningsgrunnlagInput::medUregulertGrunnbeløp)
                .orElse(vurderRefusjonBeregningsgrunnlagInput);
        }
        var forrigeGrunnlag = behandling.getOriginalBehandlingId()
            .flatMap(beh -> beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(beh));
        if (forrigeGrunnlag.isPresent()) {
            // Trenger ikke vite hvilke andeler i orginalbehandling som hadde inntektsmeldinger
            vurderRefusjonBeregningsgrunnlagInput = vurderRefusjonBeregningsgrunnlagInput.medBeregningsgrunnlagGrunnlagFraForrigeBehandling(
                BehandlingslagerTilKalkulusMapper.mapGrunnlag(forrigeGrunnlag.get()));
        }

        return vurderRefusjonBeregningsgrunnlagInput;

    }

    private StegProsesseringInput lagStegProsesseringInput(Behandling behandling,
                                                           BeregningsgrunnlagInput input,
                                                           BehandlingStegType stegType) {
        var inputMedBG = beregningTilInputTjeneste.lagInputMedVerdierFraBeregning(input);
        var grunnlagFraSteg = beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(
            behandling.getId(), Optional.empty(), mapTilStegTilstand(stegType));
        var grunnlagFraStegUt = finnForrigeAvklartGrunnlagHvisFinnes(behandling, grunnlagFraSteg, stegType);
        return new StegProsesseringInput(inputMedBG,
            mapTilKalkulatorStegTilstand(stegType)).medForrigeGrunnlagFraStegUt(
            grunnlagFraStegUt.map(BehandlingslagerTilKalkulusMapper::mapGrunnlag).orElse(null))
            .medForrigeGrunnlagFraSteg(grunnlagFraSteg.map(BehandlingslagerTilKalkulusMapper::mapGrunnlag).orElse(null))
            .medStegUtTilstand(mapTilKalkulatorStegUtTilstand(stegType).orElse(null));
    }

    private ForeslåBesteberegningInput lagInputForeslåBesteberegning(StegProsesseringInput stegProsesseringInput) {
        var input = new ForeslåBesteberegningInput(stegProsesseringInput);
        return input.medGrunnbeløpsatser(finnSatser());
    }

    private ForeslåBeregningsgrunnlagInput lagInputForeslå(StegProsesseringInput stegProsesseringInput) {
        var foreslåBeregningsgrunnlagInput = new ForeslåBeregningsgrunnlagInput(stegProsesseringInput);
        return foreslåBeregningsgrunnlagInput.medGrunnbeløpsatser(finnSatser());
    }

    private FortsettForeslåBeregningsgrunnlagInput lagInputFortsettForeslå(StegProsesseringInput stegProsesseringInput) {
        var foreslåBeregningsgrunnlagInput = new FortsettForeslåBeregningsgrunnlagInput(stegProsesseringInput);
        return foreslåBeregningsgrunnlagInput.medGrunnbeløpsatser(finnSatser());
    }

    private FordelBeregningsgrunnlagInput lagInputFordel(StegProsesseringInput stegProsesseringInput,
                                                         Optional<BeregningsgrunnlagGrunnlagEntitet> førsteFastsatteGrunnlagEntitet) {
        var fordelBeregningsgrunnlagInput = new FordelBeregningsgrunnlagInput(stegProsesseringInput);
        if (førsteFastsatteGrunnlagEntitet.isPresent()) {
            fordelBeregningsgrunnlagInput = førsteFastsatteGrunnlagEntitet.get()
                .getBeregningsgrunnlag()
                .map(BeregningsgrunnlagEntitet::getGrunnbeløp)
                .map(Beløp::getVerdi)
                .map(fordelBeregningsgrunnlagInput::medUregulertGrunnbeløp)
                .orElse(fordelBeregningsgrunnlagInput);
        }
        return fordelBeregningsgrunnlagInput;
    }

    private FullføreBeregningsgrunnlagInput lagInputFullføre(StegProsesseringInput stegProsesseringInput,
                                                             Optional<BeregningsgrunnlagGrunnlagEntitet> førsteFastsatteGrunnlagEntitet) {
        var fullføreBeregningsgrunnlagInput = new FullføreBeregningsgrunnlagInput(stegProsesseringInput);
        if (førsteFastsatteGrunnlagEntitet.isPresent()) {
            fullføreBeregningsgrunnlagInput = førsteFastsatteGrunnlagEntitet.get()
                .getBeregningsgrunnlag()
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
        var fagsakId = behandlingRepository.hentBehandling(behandlingId).getFagsak().getId();
        var behandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(fagsakId);
        return behandlinger.stream()
            .filter(b -> b.getStatus().erFerdigbehandletStatus())
            .map(kobling -> beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitet(kobling.getId(),
                BeregningsgrunnlagTilstand.FASTSATT))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(gr -> MonthDay.from(gr.getBeregningsgrunnlag()
                .orElseThrow(() -> new IllegalStateException("Skal ha beregningsgrunnlag"))
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
        return beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlingerEtterTidspunkt(
            behandling.getId(), behandling.getOriginalBehandlingId(),
            forrigeGrunnlagFraSteg.get().getOpprettetTidspunkt(), beregningsgrunnlagTilstand);
    }

}

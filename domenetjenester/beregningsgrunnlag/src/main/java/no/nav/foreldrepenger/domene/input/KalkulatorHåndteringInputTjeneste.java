package no.nav.foreldrepenger.domene.input;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.input.HåndterBeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulus.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.BehandlingslagerTilKalkulusMapper;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagRepository;

@ApplicationScoped
public class KalkulatorHåndteringInputTjeneste {

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private BehandlingRepository behandlingRepository;
    private BeregningTilInputTjeneste beregningTilInputTjeneste;

    public KalkulatorHåndteringInputTjeneste() {
        // CDI
    }

    @Inject
    public KalkulatorHåndteringInputTjeneste(BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                                             BehandlingRepository behandlingRepository,
                                             BeregningTilInputTjeneste beregningTilInputTjeneste) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.behandlingRepository = behandlingRepository;
        this.beregningTilInputTjeneste = beregningTilInputTjeneste;
    }

    public HåndterBeregningsgrunnlagInput lagInput(Long behandlingId,
                                                   BeregningsgrunnlagInput input,
                                                   String aksjonspunktKode) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        var grunnlagFraForrigeOppdatering = beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(
            behandlingId, behandlingRepository.hentBehandling(behandlingId).getOriginalBehandlingId(),
            MapHåndteringskodeTilTilstand.map(aksjonspunktKode));
        return lagHåndteringBeregningsgrunnlagInput(input, aksjonspunktKode, grunnlagFraForrigeOppdatering);

    }

    private HåndterBeregningsgrunnlagInput lagHåndteringBeregningsgrunnlagInput(BeregningsgrunnlagInput input,
                                                                                String håndteringKode,
                                                                                Optional<BeregningsgrunnlagGrunnlagEntitet> grunnlagFraHåndteringTilstand) {
        var inputMedBG = beregningTilInputTjeneste.lagInputMedVerdierFraBeregning(input);
        return new HåndterBeregningsgrunnlagInput(inputMedBG,
            BeregningsgrunnlagTilstand.fraKode(
                MapHåndteringskodeTilTilstand.map(håndteringKode).getKode())).medForrigeGrunnlagFraHåndtering(
            grunnlagFraHåndteringTilstand.map(BehandlingslagerTilKalkulusMapper::mapGrunnlag).orElse(null));
    }

}

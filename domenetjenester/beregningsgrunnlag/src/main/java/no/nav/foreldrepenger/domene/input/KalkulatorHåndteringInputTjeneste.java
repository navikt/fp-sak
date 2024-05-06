package no.nav.foreldrepenger.domene.input;

import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.input.HåndterBeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulus.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.mappers.til_kalkulator.BehandlingslagerTilKalkulusMapper;

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
                                                   AksjonspunktDefinisjon aksjonspunkt) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        var grunnlagFraForrigeOppdatering = beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(
            behandlingId, behandlingRepository.hentBehandling(behandlingId).getOriginalBehandlingId(), MapHåndteringskodeTilTilstand.map(aksjonspunkt));
        return lagHåndteringBeregningsgrunnlagInput(input, aksjonspunkt, grunnlagFraForrigeOppdatering);

    }

    private HåndterBeregningsgrunnlagInput lagHåndteringBeregningsgrunnlagInput(BeregningsgrunnlagInput input,
                                                                                AksjonspunktDefinisjon aksjonspunkt,
                                                                                Optional<BeregningsgrunnlagGrunnlagEntitet> grunnlagFraHåndteringTilstand) {
        var inputMedBG = beregningTilInputTjeneste.lagInputMedVerdierFraBeregning(input);
        return new HåndterBeregningsgrunnlagInput(inputMedBG,
            BeregningsgrunnlagTilstand.fraKode(
                MapHåndteringskodeTilTilstand.map(aksjonspunkt).getKode())).medForrigeGrunnlagFraHåndtering(
            grunnlagFraHåndteringTilstand.map(BehandlingslagerTilKalkulusMapper::mapGrunnlag).orElse(null));
    }

}

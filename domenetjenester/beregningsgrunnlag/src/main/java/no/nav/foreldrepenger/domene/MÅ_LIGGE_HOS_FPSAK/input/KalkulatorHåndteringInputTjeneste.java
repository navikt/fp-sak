package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.input;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.input.HåndterBeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulus.kodeverk.HåndteringKode;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus.BehandlingslagerTilKalkulusMapper;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand;

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

    public HåndterBeregningsgrunnlagInput lagInput(Long behandlingId, BeregningsgrunnlagInput input, String aksjonspunktKode) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        Optional<BeregningsgrunnlagGrunnlagEntitet> grunnlagFraForrigeOppdatering = beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(
            behandlingId,
            behandlingRepository.hentBehandling(behandlingId).getOriginalBehandlingId(),
            MapHåndteringskodeTilTilstand.map(aksjonspunktKode));
        return lagHåndteringBeregningsgrunnlagInput(input, aksjonspunktKode, grunnlagFraForrigeOppdatering);

    }

    private HåndterBeregningsgrunnlagInput lagHåndteringBeregningsgrunnlagInput(BeregningsgrunnlagInput input,
                                                                                String håndteringKode,
                                                                                Optional<BeregningsgrunnlagGrunnlagEntitet> grunnlagFraHåndteringTilstand) {
        BeregningsgrunnlagInput inputMedBG = beregningTilInputTjeneste.lagInputMedVerdierFraBeregning(input);

        // Til vi får flyttet tilbakerulling før overstyring: https://github.com/navikt/fp-sak/pull/1406
        inputMedBG = spesialhåndterOverstyring(input, håndteringKode, inputMedBG);

        return new HåndterBeregningsgrunnlagInput(inputMedBG, no.nav.folketrygdloven.kalkulus.felles.kodeverk.domene.BeregningsgrunnlagTilstand.fraKode(MapHåndteringskodeTilTilstand.map(håndteringKode).getKode()))
            .medForrigeGrunnlagFraHåndtering(grunnlagFraHåndteringTilstand.map(BehandlingslagerTilKalkulusMapper::mapGrunnlag).orElse(null));
    }

    private BeregningsgrunnlagInput spesialhåndterOverstyring(BeregningsgrunnlagInput input, String håndteringKode, BeregningsgrunnlagInput inputMedBG) {
        if (håndteringKode.equals(HåndteringKode.OVERSTYRING_AV_BEREGNINGSGRUNNLAG_KODE.getKode())) {
            Long behandlingId = input.getKoblingReferanse().getKoblingId();
            var oppdatertMedAndeler = beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(
                behandlingId,
                behandlingRepository.hentBehandling(behandlingId).getOriginalBehandlingId(),
                BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER)
                .map(BehandlingslagerTilKalkulusMapper::mapGrunnlag);
            inputMedBG = oppdatertMedAndeler.map(inputMedBG::medBeregningsgrunnlagGrunnlag).orElse(inputMedBG);
        }
        return inputMedBG;
    }

}

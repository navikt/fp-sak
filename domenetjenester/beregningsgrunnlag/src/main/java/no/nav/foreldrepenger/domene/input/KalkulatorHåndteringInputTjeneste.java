package no.nav.foreldrepenger.domene.input;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.input.HåndterBeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulus.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.BehandlingslagerTilKalkulusMapper;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagRepository;

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
            behandlingId, behandlingRepository.hentBehandling(behandlingId).getOriginalBehandlingId(), finnAksjonspunktTilstand(aksjonspunkt, input));
        return lagHåndteringBeregningsgrunnlagInput(input, aksjonspunkt, grunnlagFraForrigeOppdatering);

    }

    private no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand finnAksjonspunktTilstand(AksjonspunktDefinisjon aksjonspunkt,
                                                                                                             BeregningsgrunnlagInput input) {
        if (aksjonspunkt.equals(AksjonspunktDefinisjon.VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE)) {
            return input.isEnabled("splitt-foreslå-toggle", false) ? no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand.FORESLÅTT_2_UT
                : no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand.FORESLÅTT_UT;
        }
        return MapHåndteringskodeTilTilstand.map(aksjonspunkt);
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

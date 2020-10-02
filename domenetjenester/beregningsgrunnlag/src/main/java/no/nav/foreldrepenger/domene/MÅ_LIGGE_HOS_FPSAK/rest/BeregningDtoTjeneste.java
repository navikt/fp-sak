package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulator.guitjenester.BeregningsgrunnlagDtoTjeneste;
import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagGUIInput;
import no.nav.folketrygdloven.kalkulator.modell.behandling.KoblingReferanse;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus.BehandlingslagerTilKalkulusMapper;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand;

@ApplicationScoped
public class BeregningDtoTjeneste {

    private BeregningsgrunnlagDtoTjeneste beregningsgrunnlagDtoTjeneste;
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;

    public BeregningDtoTjeneste() {
        // CDO
    }

    @Inject
    public BeregningDtoTjeneste(BeregningsgrunnlagDtoTjeneste beregningsgrunnlagDtoTjeneste, BeregningsgrunnlagRepository beregningsgrunnlagRepository) {
        this.beregningsgrunnlagDtoTjeneste = beregningsgrunnlagDtoTjeneste;
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
    }

    public Optional<BeregningsgrunnlagDto> lagBeregningsgrunnlagDto(BeregningsgrunnlagGUIInput input) {
        KoblingReferanse ref = input.getKoblingReferanse();
        Optional<BeregningsgrunnlagGrunnlagEntitet> beregningsgrunnlagGrunnlagEntitet = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(ref.getKoblingId());
        if (beregningsgrunnlagGrunnlagEntitet.isEmpty()) {
            return Optional.empty();
        }
        Optional<BeregningsgrunnlagGrunnlagEntitet> orginaltGrunnlag = ref.getOriginalKoblingId().flatMap(id -> beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(id));
        BeregningsgrunnlagGUIInput inputMedBeregningsgrunnlag = settBeregningsgrunnlagPåInput(input, beregningsgrunnlagGrunnlagEntitet.get(), orginaltGrunnlag);
        return Optional.of(beregningsgrunnlagDtoTjeneste.lagBeregningsgrunnlagDto(inputMedBeregningsgrunnlag));
    }

    private BeregningsgrunnlagGUIInput settBeregningsgrunnlagPåInput(BeregningsgrunnlagGUIInput input,
                                                                      BeregningsgrunnlagGrunnlagEntitet beregningsgrunnlagGrunnlagEntitet,
                                                                      Optional<BeregningsgrunnlagGrunnlagEntitet> orginaltGrunnlag) {
        BeregningsgrunnlagGrunnlagDto bgRestDto = BehandlingslagerTilKalkulusMapper.mapGrunnlag(beregningsgrunnlagGrunnlagEntitet);
        BeregningsgrunnlagGUIInput inputMedBg = input.medBeregningsgrunnlagGrunnlag(bgRestDto);
        BeregningsgrunnlagTilstand aktivTilstand = beregningsgrunnlagGrunnlagEntitet.getBeregningsgrunnlagTilstand();
        if (orginaltGrunnlag.isPresent() && orginaltGrunnlag.get().getBeregningsgrunnlag().isPresent()) {
            // Trenger ikke inntektsmeldinger på orginalt grunnlag
            BeregningsgrunnlagGrunnlagDto orginaltBG = BehandlingslagerTilKalkulusMapper.mapGrunnlag(orginaltGrunnlag.get());
            BeregningsgrunnlagGUIInput inputMedOrginaltBG = inputMedBg.medBeregningsgrunnlagGrunnlagFraForrigeBehandling(orginaltBG);
            return leggTilTilstandgrunnlag(inputMedOrginaltBG, aktivTilstand);
        } else {
            return leggTilTilstandgrunnlag(inputMedBg, aktivTilstand);
        }
    }

    private BeregningsgrunnlagGUIInput leggTilTilstandgrunnlag(BeregningsgrunnlagGUIInput input, BeregningsgrunnlagTilstand aktivTilstand) {
        KoblingReferanse ref = input.getKoblingReferanse();
        Optional<BeregningsgrunnlagGrunnlagEntitet> kofakbergGrunnlag = Optional.empty();
        Optional<BeregningsgrunnlagGrunnlagEntitet> refBG = Optional.empty();
        Optional<BeregningsgrunnlagGrunnlagEntitet> fordeltBG = Optional.empty();

        // Fakta om beregning
        if (!aktivTilstand.erFør(BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER)) {
            kofakbergGrunnlag = beregningsgrunnlagRepository.hentBeregningsgrunnlagForPreutfylling(ref.getKoblingId(), ref.getOriginalKoblingId(),
                BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER, BeregningsgrunnlagTilstand.KOFAKBER_UT);
        }

        // Vurder refusjon
        if (!aktivTilstand.erFør(BeregningsgrunnlagTilstand.VURDERT_REFUSJON)) {
            refBG = beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitet(ref.getKoblingId(), BeregningsgrunnlagTilstand.VURDERT_REFUSJON);
        }

        // Fordeling
        if (!aktivTilstand.erFør(BeregningsgrunnlagTilstand.OPPDATERT_MED_REFUSJON_OG_GRADERING)) {
            fordeltBG = beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitet(ref.getKoblingId(), BeregningsgrunnlagTilstand.OPPDATERT_MED_REFUSJON_OG_GRADERING);
        }

        BeregningsgrunnlagGUIInput inputMedBGKofakber = kofakbergGrunnlag.map(BehandlingslagerTilKalkulusMapper::mapGrunnlag)
            .map(input::medBeregningsgrunnlagGrunnlagFraFaktaOmBeregning).orElse(input);

        BeregningsgrunnlagGUIInput inputMedRefBG = refBG.map(BehandlingslagerTilKalkulusMapper::mapGrunnlag)
            .map(inputMedBGKofakber::medBeregningsgrunnlagGrunnlagFraVurderRefusjon).orElse(inputMedBGKofakber);

        return fordeltBG.map(BehandlingslagerTilKalkulusMapper::mapGrunnlag)
            .map(inputMedRefBG::medBeregningsgrunnlagGrunnlagFraFordel).orElse(inputMedRefBG);
    }

}

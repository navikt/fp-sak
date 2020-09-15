package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest;

import java.util.Collections;
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
        BeregningsgrunnlagGrunnlagDto bgRestDto = BehandlingslagerTilKalkulusMapper.mapGrunnlag(beregningsgrunnlagGrunnlagEntitet, input.getInntektsmeldinger());
        BeregningsgrunnlagGUIInput inputMedBg = input.medBeregningsgrunnlagGrunnlag(bgRestDto);
        if (orginaltGrunnlag.isPresent() && orginaltGrunnlag.get().getBeregningsgrunnlag().isPresent()) {
            // Trenger ikke inntektsmeldinger på orginalt grunnlag
            BeregningsgrunnlagGrunnlagDto orginaltBG = BehandlingslagerTilKalkulusMapper.mapGrunnlag(orginaltGrunnlag.get(), Collections.emptyList());
            BeregningsgrunnlagGUIInput inputMedOrginaltBG = inputMedBg.medBeregningsgrunnlagGrunnlagFraForrigeBehandling(orginaltBG);
            return leggTilFordelBeregningsgrunnlag(inputMedOrginaltBG);
        } else {
            return leggTilFordelBeregningsgrunnlag(inputMedBg);
        }
    }

    private BeregningsgrunnlagGUIInput leggTilFordelBeregningsgrunnlag(BeregningsgrunnlagGUIInput input) {
        Optional<BeregningsgrunnlagGrunnlagEntitet> sisteBg = beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitet(input.getKoblingReferanse().getKoblingId(), BeregningsgrunnlagTilstand.OPPDATERT_MED_REFUSJON_OG_GRADERING);
        return sisteBg.map(gr -> BehandlingslagerTilKalkulusMapper.mapGrunnlag(gr, input.getInntektsmeldinger()))
            .map(input::medBeregningsgrunnlagGrunnlagFraFordel).orElse(input);
    }

}

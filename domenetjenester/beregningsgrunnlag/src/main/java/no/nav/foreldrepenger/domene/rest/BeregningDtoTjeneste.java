package no.nav.foreldrepenger.domene.rest;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulator.guitjenester.BeregningsgrunnlagDtoTjeneste;
import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagGUIInput;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.BehandlingslagerTilKalkulusMapper;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagRepository;

@ApplicationScoped
public class BeregningDtoTjeneste {

    private BeregningsgrunnlagDtoTjeneste beregningsgrunnlagDtoTjeneste;
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;

    public BeregningDtoTjeneste() {
        // CDO
    }

    @Inject
    public BeregningDtoTjeneste(BeregningsgrunnlagDtoTjeneste beregningsgrunnlagDtoTjeneste,
                                BeregningsgrunnlagRepository beregningsgrunnlagRepository) {
        this.beregningsgrunnlagDtoTjeneste = beregningsgrunnlagDtoTjeneste;
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
    }

    public Optional<BeregningsgrunnlagDto> lagBeregningsgrunnlagDto(BeregningsgrunnlagGUIInput input) {
        var ref = input.getKoblingReferanse();
        var beregningsgrunnlagGrunnlagEntitet = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(ref.getKoblingId());
        if (beregningsgrunnlagGrunnlagEntitet.isEmpty()) {
            return Optional.empty();
        }
        var orginaltGrunnlag = ref.getOriginalKoblingId().flatMap(id -> beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(id));
        var inputMedBeregningsgrunnlag = settBeregningsgrunnlagPåInput(input, beregningsgrunnlagGrunnlagEntitet.get(), orginaltGrunnlag);
        return Optional.of(beregningsgrunnlagDtoTjeneste.lagBeregningsgrunnlagDto(inputMedBeregningsgrunnlag));
    }

    private BeregningsgrunnlagGUIInput settBeregningsgrunnlagPåInput(BeregningsgrunnlagGUIInput input,
                                                                     BeregningsgrunnlagGrunnlagEntitet beregningsgrunnlagGrunnlagEntitet,
                                                                     Optional<BeregningsgrunnlagGrunnlagEntitet> orginaltGrunnlag) {
        var bgRestDto = BehandlingslagerTilKalkulusMapper.mapGrunnlag(
            beregningsgrunnlagGrunnlagEntitet);
        var inputMedBg = input.medBeregningsgrunnlagGrunnlag(bgRestDto);
        if (orginaltGrunnlag.isPresent() && orginaltGrunnlag.get().getBeregningsgrunnlag().isPresent()) {
            // Trenger ikke inntektsmeldinger på orginalt grunnlag
            var orginaltBG = BehandlingslagerTilKalkulusMapper.mapGrunnlag(
                orginaltGrunnlag.get());
            // Burde egentlig fikset konstruktøren så man ikke trenger ta med avklaringsbehov igjen her, men alt dette skal vekk når fpsak kaller kalulus direkte
            return inputMedBg.medBeregningsgrunnlagGrunnlagFraForrigeBehandling(
                orginaltBG).medAvklaringsbehov(input.getAvklaringsbehov());
        }
        // Burde egentlig fikset konstruktøren så man ikke trenger ta med avklaringsbehov igjen her, men alt dette skal vekk når fpsak kaller kalulus direkte
        return inputMedBg.medAvklaringsbehov(input.getAvklaringsbehov());
    }

}

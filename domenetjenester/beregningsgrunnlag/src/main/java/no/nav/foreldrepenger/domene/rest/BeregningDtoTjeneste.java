package no.nav.foreldrepenger.domene.rest;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulator.guitjenester.BeregningsgrunnlagDtoTjeneste;
import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagGUIInput;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.BehandlingslagerTilKalkulusMapper;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagTilstand;

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
        var beregningsgrunnlagGrunnlagEntitet = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(
            ref.getKoblingId());
        if (beregningsgrunnlagGrunnlagEntitet.isEmpty()) {
            return Optional.empty();
        }
        var orginaltGrunnlag = ref.getOriginalKoblingId()
            .flatMap(id -> beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(id));
        var inputMedBeregningsgrunnlag = settBeregningsgrunnlagPåInput(input,
            beregningsgrunnlagGrunnlagEntitet.get(), orginaltGrunnlag);
        return Optional.of(beregningsgrunnlagDtoTjeneste.lagBeregningsgrunnlagDto(inputMedBeregningsgrunnlag));
    }

    private BeregningsgrunnlagGUIInput settBeregningsgrunnlagPåInput(BeregningsgrunnlagGUIInput input,
                                                                     BeregningsgrunnlagGrunnlagEntitet beregningsgrunnlagGrunnlagEntitet,
                                                                     Optional<BeregningsgrunnlagGrunnlagEntitet> orginaltGrunnlag) {
        var bgRestDto = BehandlingslagerTilKalkulusMapper.mapGrunnlag(
            beregningsgrunnlagGrunnlagEntitet);
        var inputMedBg = input.medBeregningsgrunnlagGrunnlag(bgRestDto);
        var aktivTilstand = beregningsgrunnlagGrunnlagEntitet.getBeregningsgrunnlagTilstand();
        if (orginaltGrunnlag.isPresent() && orginaltGrunnlag.get().getBeregningsgrunnlag().isPresent()) {
            // Trenger ikke inntektsmeldinger på orginalt grunnlag
            var orginaltBG = BehandlingslagerTilKalkulusMapper.mapGrunnlag(
                orginaltGrunnlag.get());
            var inputMedOrginaltBG = inputMedBg.medBeregningsgrunnlagGrunnlagFraForrigeBehandling(
                orginaltBG);
            return leggTilTilstandgrunnlag(inputMedOrginaltBG, aktivTilstand);
        }
        return leggTilTilstandgrunnlag(inputMedBg, aktivTilstand);
    }

    private BeregningsgrunnlagGUIInput leggTilTilstandgrunnlag(BeregningsgrunnlagGUIInput input,
                                                               BeregningsgrunnlagTilstand aktivTilstand) {
        var ref = input.getKoblingReferanse();
        Optional<BeregningsgrunnlagGrunnlagEntitet> fordeltBG = Optional.empty();

        // Fordeling
        if (!aktivTilstand.erFør(BeregningsgrunnlagTilstand.OPPDATERT_MED_REFUSJON_OG_GRADERING)) {
            fordeltBG = beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitet(ref.getKoblingId(),
                BeregningsgrunnlagTilstand.OPPDATERT_MED_REFUSJON_OG_GRADERING);
        }

        return fordeltBG.map(BehandlingslagerTilKalkulusMapper::mapGrunnlag)
            .map(input::medBeregningsgrunnlagGrunnlagFraFordel)
            .orElse(input);
    }

}

package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest;

import java.util.Collections;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulator.guitjenester.BeregningsgrunnlagDtoTjeneste;
import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagRestInput;
import no.nav.folketrygdloven.kalkulator.modell.behandling.BehandlingReferanse;
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

    public Optional<BeregningsgrunnlagDto> lagBeregningsgrunnlagDto(BeregningsgrunnlagRestInput input) {
        BehandlingReferanse ref = input.getBehandlingReferanse();
        Optional<BeregningsgrunnlagGrunnlagEntitet> beregningsgrunnlagGrunnlagEntitet = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(ref.getBehandlingId());
        if (beregningsgrunnlagGrunnlagEntitet.isEmpty()) {
            return Optional.empty();
        }
        Optional<BeregningsgrunnlagGrunnlagEntitet> orginaltGrunnlag = ref.getOriginalBehandlingId().flatMap(id -> beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(id));
        BeregningsgrunnlagRestInput inputMedBeregningsgrunnlag = settBeregningsgrunnlagPåInput(input, beregningsgrunnlagGrunnlagEntitet.get(), orginaltGrunnlag);
        return Optional.of(beregningsgrunnlagDtoTjeneste.lagBeregningsgrunnlagDto(inputMedBeregningsgrunnlag));
    }

    private BeregningsgrunnlagRestInput settBeregningsgrunnlagPåInput(BeregningsgrunnlagRestInput input,
                                                                      BeregningsgrunnlagGrunnlagEntitet beregningsgrunnlagGrunnlagEntitet,
                                                                      Optional<BeregningsgrunnlagGrunnlagEntitet> orginaltGrunnlag) {
        BeregningsgrunnlagGrunnlagDto bgRestDto = BehandlingslagerTilKalkulusMapper.mapGrunnlag(beregningsgrunnlagGrunnlagEntitet, input.getInntektsmeldinger());
        BeregningsgrunnlagRestInput inputMedBg = input.medBeregningsgrunnlagGrunnlag(bgRestDto);
        if (orginaltGrunnlag.isPresent() && orginaltGrunnlag.get().getBeregningsgrunnlag().isPresent()) {
            // Trenger ikke inntektsmeldinger på orginalt grunnlag
            BeregningsgrunnlagGrunnlagDto orginaltBG = BehandlingslagerTilKalkulusMapper.mapGrunnlag(orginaltGrunnlag.get(), Collections.emptyList());
            BeregningsgrunnlagRestInput inputMedOrginaltBG = inputMedBg.medBeregningsgrunnlagGrunnlagFraForrigeBehandling(orginaltBG);
            lagBeregningsgrunnlagHistorikk(inputMedOrginaltBG);
            return inputMedOrginaltBG;
        } else {
            lagBeregningsgrunnlagHistorikk(inputMedBg);
            return inputMedBg;
        }
    }

    private BeregningsgrunnlagRestInput lagBeregningsgrunnlagHistorikk(BeregningsgrunnlagRestInput input) {
        BeregningsgrunnlagTilstand[] tilstander = BeregningsgrunnlagTilstand.values();
        for (BeregningsgrunnlagTilstand tilstand : tilstander) {
            Optional<BeregningsgrunnlagGrunnlagEntitet> sisteBg = beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitet(input.getBehandlingReferanse().getBehandlingId(), tilstand);
            sisteBg.ifPresent(gr -> input.leggTilBeregningsgrunnlagIHistorikk(BehandlingslagerTilKalkulusMapper.mapGrunnlag(gr, input.getInntektsmeldinger()),
                no.nav.folketrygdloven.kalkulus.felles.kodeverk.domene.BeregningsgrunnlagTilstand.fraKode(tilstand.getKode())));
        }
        return input;
    }


}

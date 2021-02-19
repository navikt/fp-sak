package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.domene.rest.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.domene.rest.dto.BesteberegningFødendeKvinneAndelDto;
import no.nav.foreldrepenger.domene.rest.dto.BesteberegningFødendeKvinneDto;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBgKunYtelseDto;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;


@ApplicationScoped
@FaktaOmBeregningTilfelleRef("VURDER_BESTEBEREGNING")
public class VurderBesteberegningHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    @Override
    public void lagHistorikk(Long behandlingId, FaktaBeregningLagreDto dto, HistorikkInnslagTekstBuilder tekstBuilder, BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                             Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        lagBesteberegningHistorikk(
            dto,
            tekstBuilder,
            forrigeGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag));
    }

    private void lagBesteberegningHistorikk(FaktaBeregningLagreDto dto, HistorikkInnslagTekstBuilder tekstBuilder, Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        Optional<Boolean> forrigeVerdi = forrigeBg
            .map(beregningsgrunnlag -> beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
                .flatMap(periode -> periode.getBeregningsgrunnlagPrStatusOgAndelList().stream())
                .anyMatch(andel -> andel.getBesteberegningPrÅr() != null));
        boolean tilVerdi = finnTilVerdi(dto);
        if (forrigeVerdi.isEmpty() || !forrigeVerdi.get().equals(tilVerdi)) {
            tekstBuilder
                .medEndretFelt(HistorikkEndretFeltType.FORDELING_ETTER_BESTEBEREGNING, forrigeVerdi.orElse(null), tilVerdi);
        }
    }

    private boolean finnTilVerdi(FaktaBeregningLagreDto dto) {
        BesteberegningFødendeKvinneDto besteberegningDto = dto.getBesteberegningAndeler();
        if (besteberegningDto != null) {
            List<BesteberegningFødendeKvinneAndelDto> andelListe = besteberegningDto.getBesteberegningAndelListe();
            return !andelListe.isEmpty();
        }
        FastsettBgKunYtelseDto kunYtelseDto = dto.getKunYtelseFordeling();
        if (kunYtelseDto != null) {
            return kunYtelseDto.getSkalBrukeBesteberegning();
        }
        return false;
    }


}

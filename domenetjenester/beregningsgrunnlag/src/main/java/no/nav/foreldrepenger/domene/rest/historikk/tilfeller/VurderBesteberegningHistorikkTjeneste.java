package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.rest.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

import java.util.Optional;


@ApplicationScoped
@FaktaOmBeregningTilfelleRef(FaktaOmBeregningTilfelle.VURDER_BESTEBEREGNING)
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
        var forrigeVerdi = forrigeBg
            .map(beregningsgrunnlag -> beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
                .flatMap(periode -> periode.getBeregningsgrunnlagPrStatusOgAndelList().stream())
                .anyMatch(andel -> andel.getBesteberegningPrÅr() != null));
        var tilVerdi = finnTilVerdi(dto);
        if (forrigeVerdi.isEmpty() || !forrigeVerdi.get().equals(tilVerdi)) {
            tekstBuilder
                .medEndretFelt(HistorikkEndretFeltType.FORDELING_ETTER_BESTEBEREGNING, forrigeVerdi.orElse(null), tilVerdi);
        }
    }

    private boolean finnTilVerdi(FaktaBeregningLagreDto dto) {
        var besteberegningDto = dto.getBesteberegningAndeler();
        if (besteberegningDto != null) {
            var andelListe = besteberegningDto.getBesteberegningAndelListe();
            return !andelListe.isEmpty();
        }
        var kunYtelseDto = dto.getKunYtelseFordeling();
        if (kunYtelseDto != null) {
            return kunYtelseDto.getSkalBrukeBesteberegning();
        }
        return false;
    }


}

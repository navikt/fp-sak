package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.rest.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;


@ApplicationScoped
@FaktaOmBeregningTilfelleRef(FaktaOmBeregningTilfelle.VURDER_BESTEBEREGNING)
public class VurderBesteberegningHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    @Override
    public List<HistorikkinnslagLinjeBuilder> lagHistorikk(FaktaBeregningLagreDto dto,
                                                           BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                                                           Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag,
                                                           InntektArbeidYtelseGrunnlag iayGrunnlag) {
        List<HistorikkinnslagLinjeBuilder> linjeBuilder = new ArrayList<>();
        linjeBuilder.add(lagBesteberegningHistorikk(dto, forrigeGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag)));

        return linjeBuilder;
    }

    private HistorikkinnslagLinjeBuilder lagBesteberegningHistorikk(FaktaBeregningLagreDto dto, Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        var forrigeVerdi = forrigeBg.map(beregningsgrunnlag -> beregningsgrunnlag.getBeregningsgrunnlagPerioder()
            .stream()
            .flatMap(periode -> periode.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .anyMatch(andel -> andel.getBesteberegningPrÅr() != null));
        var tilVerdi = finnTilVerdi(dto);
        if (forrigeVerdi.isEmpty() || !forrigeVerdi.get().equals(tilVerdi)) {
            return new HistorikkinnslagLinjeBuilder().fraTil("Fordeling etter besteberegning", forrigeVerdi.orElse(null), tilVerdi);
        }
        return null;
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

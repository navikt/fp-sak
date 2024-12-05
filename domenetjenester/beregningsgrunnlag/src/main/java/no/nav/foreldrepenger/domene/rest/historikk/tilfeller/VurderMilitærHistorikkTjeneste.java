package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.rest.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef(FaktaOmBeregningTilfelle.VURDER_MILITÆR_SIVILTJENESTE)
public class VurderMilitærHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    @Override
    public List<HistorikkinnslagLinjeBuilder> lagHistorikk(FaktaBeregningLagreDto dto,
                                                           BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                                                           Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag,
                                                           InntektArbeidYtelseGrunnlag iayGrunnlag) {
        List<HistorikkinnslagLinjeBuilder> linjerBuilder = new ArrayList<>();
        var militærDto = dto.getVurderMilitaer();
        var haddeMilitærIForrigeGrunnlag = finnForrigeVerdi(forrigeGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag));
        linjerBuilder.add(lagHistorikkInnslag(militærDto.getHarMilitaer(), haddeMilitærIForrigeGrunnlag));
        linjerBuilder.add(HistorikkinnslagLinjeBuilder.LINJESKIFT);

        return linjerBuilder;
    }

    private HistorikkinnslagLinjeBuilder lagHistorikkInnslag(Boolean harMilitærEllerSivil, Boolean forrigeVerdi) {
        return new HistorikkinnslagLinjeBuilder().fraTil("Har søker militær- eller siviltjeneste i opptjeningsperioden", forrigeVerdi,
            harMilitærEllerSivil);
    }

    private Boolean finnForrigeVerdi(Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        return forrigeBg.map(this::harMilitærstatus).orElse(null);
    }

    private boolean harMilitærstatus(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return beregningsgrunnlag.getAktivitetStatuser()
            .stream()
            .anyMatch(status -> AktivitetStatus.MILITÆR_ELLER_SIVIL.equals(AktivitetStatus.fraKode(status.getAktivitetStatus().getKode())));
    }

}

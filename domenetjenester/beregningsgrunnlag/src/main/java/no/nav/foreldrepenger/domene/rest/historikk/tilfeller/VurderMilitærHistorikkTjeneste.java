package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder;
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
    public List<HistorikkinnslagTekstlinjeBuilder> lagHistorikk(FaktaBeregningLagreDto dto,
                                                                BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                                                                Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag,
                                                                InntektArbeidYtelseGrunnlag iayGrunnlag) {
        List<HistorikkinnslagTekstlinjeBuilder> tekstlinjerBuilder = new ArrayList<>();
        var militærDto = dto.getVurderMilitaer();
        var haddeMilitærIForrigeGrunnlag = finnForrigeVerdi(forrigeGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag));
        tekstlinjerBuilder.add(lagHistorikkInnslag(militærDto.getHarMilitaer(), haddeMilitærIForrigeGrunnlag));
        tekstlinjerBuilder.add(new HistorikkinnslagTekstlinjeBuilder().linjeskift());

        return tekstlinjerBuilder;
    }

    private HistorikkinnslagTekstlinjeBuilder lagHistorikkInnslag(Boolean harMilitærEllerSivil, Boolean forrigeVerdi) {
        return new HistorikkinnslagTekstlinjeBuilder().fraTil("Har søker militær- eller siviltjeneste i opptjeningsperioden", forrigeVerdi,
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

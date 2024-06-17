package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.rest.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef(FaktaOmBeregningTilfelle.VURDER_MILITÆR_SIVILTJENESTE)
public class VurderMilitærHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    @Override
    public void lagHistorikk(Long behandlingId,
                             FaktaBeregningLagreDto dto,
                             HistorikkInnslagTekstBuilder tekstBuilder,
                             BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                             Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag,
                             InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var militærDto = dto.getVurderMilitaer();
        var haddeMilitærIForrigeGrunnlag = finnForrigeVerdi(forrigeGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag));
        lagHistorikkInnslag(militærDto.getHarMilitaer(), haddeMilitærIForrigeGrunnlag, tekstBuilder);
    }

    private void lagHistorikkInnslag(Boolean harMilitærEllerSivil, Boolean forrigeVerdi, HistorikkInnslagTekstBuilder tekstBuilder) {
        tekstBuilder.medEndretFelt(HistorikkEndretFeltType.MILITÆR_ELLER_SIVIL, forrigeVerdi, harMilitærEllerSivil);
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

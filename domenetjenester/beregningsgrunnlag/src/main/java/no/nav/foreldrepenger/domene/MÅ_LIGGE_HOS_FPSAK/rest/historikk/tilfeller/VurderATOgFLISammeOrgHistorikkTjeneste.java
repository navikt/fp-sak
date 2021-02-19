package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.historikk.tilfeller;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.historikk.InntektHistorikkTjeneste;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.historikk.MapTilLønnsendring;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef("VURDER_AT_OG_FL_I_SAMME_ORGANISASJON")
public class VurderATOgFLISammeOrgHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    private InntektHistorikkTjeneste inntektHistorikkTjeneste;

    public VurderATOgFLISammeOrgHistorikkTjeneste() {
        // For CDI
    }

    @Inject
    public VurderATOgFLISammeOrgHistorikkTjeneste(InntektHistorikkTjeneste inntektHistorikkTjeneste) {
        this.inntektHistorikkTjeneste = inntektHistorikkTjeneste;
    }

    @Override
    public void lagHistorikk(Long behandlingId, FaktaBeregningLagreDto dto, HistorikkInnslagTekstBuilder tekstBuilder, BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        inntektHistorikkTjeneste.lagHistorikk(
            tekstBuilder,
            MapTilLønnsendring.mapLønnsendringFraATogFLSammeOrg(dto, nyttBeregningsgrunnlag,
            forrigeGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag)),
            iayGrunnlag);
    }

}

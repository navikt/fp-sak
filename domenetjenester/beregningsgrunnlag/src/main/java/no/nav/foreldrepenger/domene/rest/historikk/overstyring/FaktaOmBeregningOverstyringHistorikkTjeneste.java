package no.nav.foreldrepenger.domene.rest.historikk.overstyring;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.rest.dto.MatchBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.rest.dto.OverstyrBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.rest.historikk.InntektHistorikkTjeneste;
import no.nav.foreldrepenger.domene.rest.historikk.MapTilLønnsendring;

@ApplicationScoped
public class FaktaOmBeregningOverstyringHistorikkTjeneste {

    private InntektHistorikkTjeneste inntektHistorikkTjeneste;

    public FaktaOmBeregningOverstyringHistorikkTjeneste() {
        // For CDI
    }

    @Inject
    public FaktaOmBeregningOverstyringHistorikkTjeneste(InntektHistorikkTjeneste inntektHistorikkTjeneste) {
        this.inntektHistorikkTjeneste = inntektHistorikkTjeneste;
    }

    /**
     * Lager historikk for overstyring av inntekter, refusjon og inntektskategori i fakta om beregning.
     *
     * @param dto                    Dto for overstyring av beregningsgrunnlag
     * @param nyttBeregningsgrunnlag Aktivt og oppdatert beregningsgrunnlag
     * @param forrigeGrunnlag        Forrige beregningsgrunnlag fra KOFAKBER_UT
     * @param iayGrunnlag            InntektArbeidYtelseGrunnlag
     */
    public List<HistorikkinnslagTekstlinjeBuilder> lagHistorikk(OverstyrBeregningsgrunnlagDto dto,
                                                                BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                                                                Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag,
                                                                InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var bgPerioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        var overstyrteAndeler = dto.getOverstyrteAndeler();
        List<HistorikkinnslagTekstlinjeBuilder> tekstlinjerBuilder = new ArrayList<>();
        for (var bgPeriode : bgPerioder) {
            var forrigeBgPeriode = MatchBeregningsgrunnlagTjeneste.finnOverlappendePeriodeOmKunEnFinnes(bgPeriode,
                forrigeGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag));
            var endringer = overstyrteAndeler.stream()
                .map(andelDto -> MapTilLønnsendring.mapTilLønnsendringForAndelIPeriode(andelDto, andelDto.getFastsatteVerdier(), bgPeriode,
                    forrigeBgPeriode))
                .toList();
            tekstlinjerBuilder = inntektHistorikkTjeneste.lagHistorikk(endringer, iayGrunnlag);
            tekstlinjerBuilder.add(new HistorikkinnslagTekstlinjeBuilder().linjeskift());
        }
        return tekstlinjerBuilder;
    }
}

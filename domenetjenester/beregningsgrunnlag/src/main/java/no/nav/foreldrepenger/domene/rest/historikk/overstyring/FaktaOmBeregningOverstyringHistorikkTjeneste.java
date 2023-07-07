package no.nav.foreldrepenger.domene.rest.historikk.overstyring;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.rest.dto.MatchBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.rest.dto.OverstyrBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.rest.historikk.InntektHistorikkTjeneste;
import no.nav.foreldrepenger.domene.rest.historikk.MapTilLønnsendring;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

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
     *  Lager historikk for overstyring av inntekter, refusjon og inntektskategori i fakta om beregning.
     *  @param behandlingId Id for behandling
     * @param dto Dto for overstyring av beregningsgrunnlag
     * @param tekstBuilder Builder for historikkinnslag
     * @param nyttBeregningsgrunnlag Aktivt og oppdatert beregningsgrunnlag
     * @param forrigeGrunnlag Forrige beregningsgrunnlag fra KOFAKBER_UT
     * @param iayGrunnlag InntektArbeidYtelseGrunnlag
     */
    public void lagHistorikk(Long behandlingId,
                             OverstyrBeregningsgrunnlagDto dto,
                             HistorikkInnslagTekstBuilder tekstBuilder,
                             BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var bgPerioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        var overstyrteAndeler = dto.getOverstyrteAndeler();
        for (var bgPeriode : bgPerioder) {
            var forrigeBgPeriode = MatchBeregningsgrunnlagTjeneste
                .finnOverlappendePeriodeOmKunEnFinnes(bgPeriode, forrigeGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag));
            var endringer = overstyrteAndeler.stream()
                .map(andelDto -> MapTilLønnsendring.mapTilLønnsendringForAndelIPeriode(andelDto, andelDto.getFastsatteVerdier(), bgPeriode, forrigeBgPeriode))
                .toList();
            inntektHistorikkTjeneste.lagHistorikk(tekstBuilder, endringer, iayGrunnlag);
        }
        settSkjermlenke(tekstBuilder);
        tekstBuilder.ferdigstillHistorikkinnslagDel();
    }

    private void settSkjermlenke(HistorikkInnslagTekstBuilder tekstBuilder) {
        var erSkjermlenkeSatt = tekstBuilder.getHistorikkinnslagDeler().stream().anyMatch(historikkDel -> historikkDel.getSkjermlenke().isPresent());
        if (!erSkjermlenkeSatt) {
            tekstBuilder.medSkjermlenke(SkjermlenkeType.FAKTA_OM_BEREGNING);
        }
    }
}

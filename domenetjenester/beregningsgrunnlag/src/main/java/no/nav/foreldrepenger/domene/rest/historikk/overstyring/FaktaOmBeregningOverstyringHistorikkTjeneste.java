package no.nav.foreldrepenger.domene.rest.historikk.overstyring;

import java.util.Collections;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.oppdateringresultat.BeregningsgrunnlagEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.OppdaterBeregningsgrunnlagResultat;
import no.nav.foreldrepenger.domene.rest.dto.OverstyrBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.rest.historikk.InntektHistorikkTjeneste;
import no.nav.foreldrepenger.domene.rest.historikk.MapTilLønnsendring;
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
     * Lager historikk for overstyring av inntekter, refusjon og inntektskategori i fakta om beregning.
     *
     * @param oppdaterResultat Endringsresultat
     * @param dto              Dto for overstyring av beregningsgrunnlag
     * @param tekstBuilder     Builder for historikkinnslag
     * @param iayGrunnlag      InntektArbeidYtelseGrunnlag
     */
    public void lagHistorikk(OppdaterBeregningsgrunnlagResultat oppdaterResultat,
                             OverstyrBeregningsgrunnlagDto dto,
                             HistorikkInnslagTekstBuilder tekstBuilder,
                             InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var overstyrteAndeler = dto.getOverstyrteAndeler();
        var periodeEndringer = oppdaterResultat.getBeregningsgrunnlagEndring()
            .map(BeregningsgrunnlagEndring::getBeregningsgrunnlagPeriodeEndringer)
            .orElse(Collections.emptyList());
        for (var periodeEndring : periodeEndringer) {
            var endringer = overstyrteAndeler.stream()
                .flatMap(andelDto -> periodeEndring.getBeregningsgrunnlagPrStatusOgAndelEndringer()
                    .stream()
                    .filter(a -> a.getAndelsnr().equals(andelDto.getAndelsnr()))
                    .map(MapTilLønnsendring::mapAndelEndringTilLønnsendring))
                .collect(Collectors.toList());
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

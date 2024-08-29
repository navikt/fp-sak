package no.nav.foreldrepenger.domene.rest.historikk.kalkulus;

import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.domene.aksjonspunkt.BeregningsgrunnlagPeriodeEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeregningsgrunnlagPrStatusOgAndelEndring;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

/**
 * Lager historikk for endret inntekt og inntektskategori etter oppdatering fra Kalkulus.
 */
@Dependent
public class BeregningsgrunnlagVerdierHistorikkTjeneste {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private InntektHistorikkKalkulusTjeneste inntektHistorikkKalkulusTjeneste;
    private InntektskategoriHistorikkKalkukusTjeneste inntektskategoriHistorikkTjeneste;
    private RefusjonHistorikkKalkulusTjeneste refusjonHistorikkTjeneste;

    public BeregningsgrunnlagVerdierHistorikkTjeneste() {
        // CDI
    }

    @Inject
    public BeregningsgrunnlagVerdierHistorikkTjeneste(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                      InntektHistorikkKalkulusTjeneste inntektHistorikkKalkulusTjeneste,
                                                      InntektskategoriHistorikkKalkukusTjeneste inntektskategoriHistorikkTjeneste,
                                                      RefusjonHistorikkKalkulusTjeneste refusjonHistorikkTjeneste) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.inntektHistorikkKalkulusTjeneste = inntektHistorikkKalkulusTjeneste;
        this.inntektskategoriHistorikkTjeneste = inntektskategoriHistorikkTjeneste;
        this.refusjonHistorikkTjeneste = refusjonHistorikkTjeneste;
    }

    public void lagHistorikkForBeregningsgrunnlagVerdier(Long behandlingId, BeregningsgrunnlagPeriodeEndring periode,
                                                         HistorikkInnslagTekstBuilder tekstBuilder) {
        InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId);
        List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer = inntektArbeidYtelseGrunnlag.getArbeidsforholdOverstyringer();
        periode.getBeregningsgrunnlagPrStatusOgAndelEndringer()
            .forEach(andelEndring -> lagHistorikkForAndel(tekstBuilder, arbeidsforholdOverstyringer, andelEndring));
    }

    private void lagHistorikkForAndel(HistorikkInnslagTekstBuilder tekstBuilder, List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer, BeregningsgrunnlagPrStatusOgAndelEndring andelEndring) {
        inntektHistorikkKalkulusTjeneste.lagHistorikkOmEndret(tekstBuilder, arbeidsforholdOverstyringer, andelEndring);
        inntektskategoriHistorikkTjeneste.lagHistorikkOmEndret(tekstBuilder, arbeidsforholdOverstyringer, andelEndring);
        refusjonHistorikkTjeneste.lagHistorikkOmEndret(tekstBuilder, arbeidsforholdOverstyringer, andelEndring);

    }

}

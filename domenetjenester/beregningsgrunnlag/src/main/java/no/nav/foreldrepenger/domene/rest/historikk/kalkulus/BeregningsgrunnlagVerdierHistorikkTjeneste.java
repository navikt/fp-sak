package no.nav.foreldrepenger.domene.rest.historikk.kalkulus;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeregningsgrunnlagPeriodeEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeregningsgrunnlagPrStatusOgAndelEndring;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;

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

    public List<HistorikkinnslagLinjeBuilder> lagHistorikkForBeregningsgrunnlagVerdier(Long behandlingId, BeregningsgrunnlagPeriodeEndring periode) {
        var inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId);
        var arbeidsforholdOverstyringer = inntektArbeidYtelseGrunnlag.getArbeidsforholdOverstyringer();
        return periode.getBeregningsgrunnlagPrStatusOgAndelEndringer().stream()
            .flatMap(andelEndring -> lagHistorikkForAndel(arbeidsforholdOverstyringer, andelEndring).stream())
            .toList();
    }

    private List<HistorikkinnslagLinjeBuilder> lagHistorikkForAndel(List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer, BeregningsgrunnlagPrStatusOgAndelEndring andelEndring) {
        var linjer = new ArrayList<HistorikkinnslagLinjeBuilder>();
        inntektHistorikkKalkulusTjeneste.lagHistorikkOmEndret(arbeidsforholdOverstyringer, andelEndring).ifPresent(linjer::add);
        inntektskategoriHistorikkTjeneste.lagHistorikkOmEndret(arbeidsforholdOverstyringer, andelEndring).ifPresent(linjer::add);
        refusjonHistorikkTjeneste.lagHistorikkOmEndret(arbeidsforholdOverstyringer, andelEndring).ifPresent(linjer::add);
        return linjer;
    }

}

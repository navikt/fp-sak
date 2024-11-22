package no.nav.foreldrepenger.domene.rest.historikk.kalkulus;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeregningsgrunnlagPrStatusOgAndelEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.InntektskategoriEndring;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.rest.historikk.ArbeidsgiverHistorikkinnslag;

/**
 * Historikktjeneste for endring av inntektskategori
 */
@Dependent
public class InntektskategoriHistorikkKalkukusTjeneste {

    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslag;

    public InntektskategoriHistorikkKalkukusTjeneste() {
        // CDI
    }

    @Inject
    public InntektskategoriHistorikkKalkukusTjeneste(ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslag) {
        this.arbeidsgiverHistorikkinnslag = arbeidsgiverHistorikkinnslag;
    }

    Optional<HistorikkinnslagTekstlinjeBuilder> lagHistorikkOmEndret(List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer, BeregningsgrunnlagPrStatusOgAndelEndring andelEndring) {
        return andelEndring.getInntektskategoriEndring()
            .map(inntektskategoriEndring -> lagHistorikkinnslagTekstlinjeFor(arbeidsforholdOverstyringer, andelEndring, inntektskategoriEndring));
    }

    private HistorikkinnslagTekstlinjeBuilder lagHistorikkinnslagTekstlinjeFor(List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer,
                                                                               BeregningsgrunnlagPrStatusOgAndelEndring andelEndring,
                                                                               InntektskategoriEndring endring) {
        var arbeidsgiverinfo = arbeidsgiverHistorikkinnslag.lagHistorikkinnslagTekstForBeregningsgrunnlag(
            andelEndring.getAktivitetStatus(),
            andelEndring.getArbeidsgiver(),
            Optional.of(andelEndring.getArbeidsforholdRef()),
            arbeidsforholdOverstyringer);
        return HistorikkinnslagTekstlinjeBuilder.fraTilEquals(String.format("Inntektskategori %s", arbeidsgiverinfo), endring.getFraVerdi(), endring.getTilVerdi());
    }
}

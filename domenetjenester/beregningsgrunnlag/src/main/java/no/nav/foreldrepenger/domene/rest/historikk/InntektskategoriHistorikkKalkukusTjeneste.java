package no.nav.foreldrepenger.domene.rest.historikk;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeregningsgrunnlagPrStatusOgAndelEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.InntektskategoriEndring;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;

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

    Optional<HistorikkinnslagLinjeBuilder> lagHistorikkOmEndret(List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer, BeregningsgrunnlagPrStatusOgAndelEndring andelEndring) {
        return andelEndring.getInntektskategoriEndring()
            .map(inntektskategoriEndring -> lagHistorikkinnslagLinjeFor(arbeidsforholdOverstyringer, andelEndring, inntektskategoriEndring));
    }

    private HistorikkinnslagLinjeBuilder lagHistorikkinnslagLinjeFor(List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer,
                                                                     BeregningsgrunnlagPrStatusOgAndelEndring andelEndring,
                                                                     InntektskategoriEndring endring) {
        var arbeidsgiverinfo = arbeidsgiverHistorikkinnslag.lagHistorikkinnslagTekstForBeregningsgrunnlag(
            andelEndring.getAktivitetStatus(),
            andelEndring.getArbeidsgiver(),
            Optional.of(andelEndring.getArbeidsforholdRef()),
            arbeidsforholdOverstyringer);
        return HistorikkinnslagLinjeBuilder.fraTilEquals(String.format("Inntektskategori %s", arbeidsgiverinfo), endring.getFraVerdi(), endring.getTilVerdi());
    }
}

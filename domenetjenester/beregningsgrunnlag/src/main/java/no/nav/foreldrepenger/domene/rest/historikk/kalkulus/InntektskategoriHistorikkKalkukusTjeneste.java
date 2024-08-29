package no.nav.foreldrepenger.domene.rest.historikk.kalkulus;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeregningsgrunnlagPrStatusOgAndelEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.InntektskategoriEndring;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.rest.historikk.ArbeidsgiverHistorikkinnslag;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

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

    void lagHistorikkOmEndret(HistorikkInnslagTekstBuilder tekstBuilder, List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer, BeregningsgrunnlagPrStatusOgAndelEndring andelEndring) {
        Optional<InntektskategoriEndring> inntektskategoriEndring = andelEndring.getInntektskategoriEndring();
        inntektskategoriEndring.ifPresent(endring -> tekstBuilder.medEndretFelt(HistorikkEndretFeltType.INNTEKTSKATEGORI,
            arbeidsgiverHistorikkinnslag.lagHistorikkinnslagTekstForBeregningsgrunnlag(
                andelEndring.getAktivitetStatus(),
                andelEndring.getArbeidsgiver(),
                Optional.of(andelEndring.getArbeidsforholdRef()),
                arbeidsforholdOverstyringer),
            endring.getFraVerdi(),
            endring.getTilVerdi()));
    }
}

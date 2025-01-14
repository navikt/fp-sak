package no.nav.foreldrepenger.domene.rest.historikk.kalkulus;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkBelop;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeløpEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeregningsgrunnlagPrStatusOgAndelEndring;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.rest.historikk.ArbeidsgiverHistorikkinnslag;

/**
 * Historikktjeneste for endring av inntekt
 */
@Dependent
public class InntektHistorikkKalkulusTjeneste {

    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslag;

    public InntektHistorikkKalkulusTjeneste() {
        // CDI
    }

    @Inject
    public InntektHistorikkKalkulusTjeneste(ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslag) {
        this.arbeidsgiverHistorikkinnslag = arbeidsgiverHistorikkinnslag;
    }

    public Optional<HistorikkinnslagLinjeBuilder> lagHistorikkOmEndret(List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer,
                                                                       BeregningsgrunnlagPrStatusOgAndelEndring andelEndring) {
        return andelEndring.getInntektEndring().map(endring -> opprettInntektHistorikk(arbeidsforholdOverstyringer, andelEndring, endring));
    }

    private HistorikkinnslagLinjeBuilder opprettInntektHistorikk(List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer, BeregningsgrunnlagPrStatusOgAndelEndring andelEndring, BeløpEndring beløpEndring) {
        if (andelEndring.getAktivitetStatus().erArbeidstaker() && OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE.equals(andelEndring.getArbeidsforholdType())) {
            return fraTilEquals("Fastsett søkers månedsinntekt fra etterlønn eller sluttpakke",
                HistorikkBelop.valueOf(beløpEndring.getFraMånedsbeløp()), HistorikkBelop.valueOf(beløpEndring.getTilMånedsbeløp()));
        } else {
            var arbeidsforholdInfo = andelEndring.getArbeidsgiver()
                .map(ag -> arbeidsgiverHistorikkinnslag.lagTekstForArbeidsgiver(ag, arbeidsforholdOverstyringer))
                .orElse(andelEndring.getAktivitetStatus().getNavn());
            return fraTilEquals(String.format("Inntekt fra %s", arbeidsforholdInfo), HistorikkBelop.valueOf(beløpEndring.getFraMånedsbeløp()),
                HistorikkBelop.valueOf(beløpEndring.getTilMånedsbeløp()));
        }
    }
}

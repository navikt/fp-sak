package no.nav.foreldrepenger.domene.rest.historikk;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkBeløp;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeløpEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeregningsgrunnlagPrStatusOgAndelEndring;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;

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
                HistorikkBeløp.ofNullable(beløpEndring.getFraMånedsbeløp()), HistorikkBeløp.ofNullable(beløpEndring.getTilMånedsbeløp()));
        } else {
            var arbeidsforholdInfo = andelEndring.getArbeidsgiver()
                .map(ag -> arbeidsgiverHistorikkinnslag.lagTekstForArbeidsgiver(ag, arbeidsforholdOverstyringer))
                .orElse(andelEndring.getAktivitetStatus().getNavn());
            return fraTilEquals(String.format("Inntekt fra %s", arbeidsforholdInfo), HistorikkBeløp.ofNullable(beløpEndring.getFraMånedsbeløp()),
                HistorikkBeløp.ofNullable(beløpEndring.getTilMånedsbeløp()));
        }
    }
}

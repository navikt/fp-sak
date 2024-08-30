package no.nav.foreldrepenger.domene.rest.historikk.kalkulus;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeløpEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeregningsgrunnlagPrStatusOgAndelEndring;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.rest.historikk.ArbeidsgiverHistorikkinnslag;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

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

    public void lagHistorikkOmEndret(HistorikkInnslagTekstBuilder tekstBuilder, List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer, BeregningsgrunnlagPrStatusOgAndelEndring andelEndring) {
        Optional<BeløpEndring> inntektEndring = andelEndring.getInntektEndring();
        inntektEndring.ifPresent(endring -> opprettInntektHistorikk(tekstBuilder, arbeidsforholdOverstyringer, andelEndring, endring));
    }

    private void opprettInntektHistorikk(HistorikkInnslagTekstBuilder tekstBuilder, List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer, BeregningsgrunnlagPrStatusOgAndelEndring andelEndring, BeløpEndring beløpEndring) {
        if (andelEndring.getAktivitetStatus().erArbeidstaker()) {
            opprettHistorikkArbeidstakerInntekt(tekstBuilder, arbeidsforholdOverstyringer, andelEndring, beløpEndring);
        } else if (andelEndring.getAktivitetStatus().erFrilanser()) {
            opprettHistorikkFrilansinntekt(tekstBuilder, beløpEndring);
        } else if (AktivitetStatus.DAGPENGER.equals(andelEndring.getAktivitetStatus())) {
            opprettHistorikkDagpengeinntekt(tekstBuilder, beløpEndring);
        } else {
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.INNTEKT_FRA_ARBEIDSFORHOLD,
                andelEndring.getAktivitetStatus().getNavn(),
                tilInt(beløpEndring.getFraMånedsbeløp()),
                tilInt(beløpEndring.getTilMånedsbeløp()));
        }
    }

    private void opprettHistorikkArbeidstakerInntekt(HistorikkInnslagTekstBuilder tekstBuilder, List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer, BeregningsgrunnlagPrStatusOgAndelEndring andelEndring, BeløpEndring beløpEndring) {
        if (OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE.equals(andelEndring.getArbeidsforholdType())) {
            opprettHistorikkEtterlønnSluttpakke(tekstBuilder, beløpEndring);
        } else if (andelEndring.getArbeidsgiver().isPresent()) {
            opprettHistorikkArbeidsinntekt(
                tekstBuilder,
                arbeidsforholdOverstyringer,
                andelEndring,
                beløpEndring);
        }
    }

    private void opprettHistorikkDagpengeinntekt(HistorikkInnslagTekstBuilder tekstBuilder, BeløpEndring beløpEndring) {
        tekstBuilder.medEndretFelt(HistorikkEndretFeltType.DAGPENGER_INNTEKT,
            beløpEndring.getFraMånedsbeløp(),
            beløpEndring.getTilMånedsbeløp());
    }

    private void opprettHistorikkEtterlønnSluttpakke(HistorikkInnslagTekstBuilder tekstBuilder, BeløpEndring beløpEndring) {
        tekstBuilder.medEndretFelt(HistorikkEndretFeltType.FASTSETT_ETTERLØNN_SLUTTPAKKE,
            beløpEndring.getFraMånedsbeløp(),
            beløpEndring.getTilMånedsbeløp());
    }

    private void opprettHistorikkFrilansinntekt(HistorikkInnslagTekstBuilder tekstBuilder, BeløpEndring beløpEndring) {
        tekstBuilder.medEndretFelt(
            HistorikkEndretFeltType.FRILANS_INNTEKT,
            beløpEndring.getFraMånedsbeløp(),
            beløpEndring.getTilMånedsbeløp());
    }

    private void opprettHistorikkArbeidsinntekt(HistorikkInnslagTekstBuilder tekstBuilder, List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer, BeregningsgrunnlagPrStatusOgAndelEndring andelEndring, BeløpEndring beløpEndring) {
        String arbeidsforholdInfo = arbeidsgiverHistorikkinnslag.lagTekstForArbeidsgiver(
            andelEndring.getArbeidsgiver().get(),
            arbeidsforholdOverstyringer);
        tekstBuilder.medEndretFelt(HistorikkEndretFeltType.INNTEKT_FRA_ARBEIDSFORHOLD,
            arbeidsforholdInfo,
            tilInt(beløpEndring.getFraMånedsbeløp()),
            tilInt(beløpEndring.getTilMånedsbeløp()));
    }

    private Integer tilInt(BigDecimal bdBeløp) {
        return bdBeløp == null ? null : bdBeløp.intValue();
    }

}

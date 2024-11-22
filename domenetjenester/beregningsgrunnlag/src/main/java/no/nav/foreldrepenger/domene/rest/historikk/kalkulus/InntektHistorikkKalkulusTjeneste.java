package no.nav.foreldrepenger.domene.rest.historikk.kalkulus;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder.fraTilEquals;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeløpEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeregningsgrunnlagPrStatusOgAndelEndring;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
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

    public Optional<HistorikkinnslagTekstlinjeBuilder> lagHistorikkOmEndret(List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer, BeregningsgrunnlagPrStatusOgAndelEndring andelEndring) {
        return andelEndring.getInntektEndring()
            .map(endring -> opprettInntektHistorikk(arbeidsforholdOverstyringer, andelEndring, endring));
    }

    private HistorikkinnslagTekstlinjeBuilder opprettInntektHistorikk(List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer, BeregningsgrunnlagPrStatusOgAndelEndring andelEndring, BeløpEndring beløpEndring) {
        if (andelEndring.getAktivitetStatus().erArbeidstaker() && OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE.equals(andelEndring.getArbeidsforholdType())) {
            return fraTilEquals("Fastsett søkers månedsinntekt fra etterlønn eller sluttpakke", beløpEndring.getFraMånedsbeløp(), beløpEndring.getTilMånedsbeløp());
        } else if (andelEndring.getAktivitetStatus().erFrilanser()) {
            return fraTilEquals("Frilans inntekt", beløpEndring.getFraMånedsbeløp(), beløpEndring.getTilMånedsbeløp());
        } else if (AktivitetStatus.DAGPENGER.equals(andelEndring.getAktivitetStatus())) {
            return fraTilEquals("Dagpenger", beløpEndring.getFraMånedsbeløp(), beløpEndring.getTilMånedsbeløp());
        } else {
            var arbeidsforholdInfo = andelEndring.getArbeidsgiver().isPresent()
                ? arbeidsgiverHistorikkinnslag.lagTekstForArbeidsgiver(andelEndring.getArbeidsgiver().get(), arbeidsforholdOverstyringer)
                : andelEndring.getAktivitetStatus().getNavn();
            return fraTilEquals(
                String.format("Inntekt fra arbeidsforhold %s", arbeidsforholdInfo),
                tilInt(beløpEndring.getFraMånedsbeløp()),
                tilInt(beløpEndring.getTilMånedsbeløp())
            );
        }
    }

    private static Integer tilInt(BigDecimal bdBeløp) {
        return bdBeløp == null ? null : bdBeløp.intValue();
    }
}

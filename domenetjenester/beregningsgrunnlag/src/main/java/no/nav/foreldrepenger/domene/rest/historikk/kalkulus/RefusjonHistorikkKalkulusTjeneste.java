package no.nav.foreldrepenger.domene.rest.historikk.kalkulus;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeregningsgrunnlagPrStatusOgAndelEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.RefusjonEndring;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.rest.historikk.ArbeidsgiverHistorikkinnslag;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Historikktjeneste for endring av inntekt
 */
@Dependent
public class RefusjonHistorikkKalkulusTjeneste {

    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslag;

    public RefusjonHistorikkKalkulusTjeneste() {
        // CDI
    }

    @Inject
    public RefusjonHistorikkKalkulusTjeneste(ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslag) {
        this.arbeidsgiverHistorikkinnslag = arbeidsgiverHistorikkinnslag;
    }

    public void lagHistorikkOmEndret(HistorikkInnslagTekstBuilder tekstBuilder, List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer, BeregningsgrunnlagPrStatusOgAndelEndring andelEndring) {
        Optional<RefusjonEndring> refusjonEndring = andelEndring.getRefusjonEndring();
        refusjonEndring.ifPresent(endring -> opprettRefusjonHistorikk(tekstBuilder, arbeidsforholdOverstyringer, andelEndring, endring));
    }

    private void opprettRefusjonHistorikk(HistorikkInnslagTekstBuilder tekstBuilder, List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer, BeregningsgrunnlagPrStatusOgAndelEndring andelEndring, RefusjonEndring refusjonEndring) {
        if (andelEndring.getAktivitetStatus().erArbeidstaker()) {
            opprettHistorikkArbeidstakerInntekt(tekstBuilder, arbeidsforholdOverstyringer, andelEndring, refusjonEndring);
        }
    }

    private void opprettHistorikkArbeidstakerInntekt(HistorikkInnslagTekstBuilder tekstBuilder, List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer, BeregningsgrunnlagPrStatusOgAndelEndring andelEndring, RefusjonEndring refusjonEndring) {
        if (OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE.equals(andelEndring.getArbeidsforholdType())) {
            opprettHistorikkEtterlønnSluttpakke(tekstBuilder, refusjonEndring);
        } else if (andelEndring.getArbeidsgiver().isPresent()) {
            opprettHistorikkArbeidsinntekt(
                tekstBuilder,
                arbeidsforholdOverstyringer,
                andelEndring,
                refusjonEndring);
        }
    }

    private void opprettHistorikkEtterlønnSluttpakke(HistorikkInnslagTekstBuilder tekstBuilder, RefusjonEndring inntektEndring) {
        tekstBuilder.medEndretFelt(HistorikkEndretFeltType.FASTSETT_ETTERLØNN_SLUTTPAKKE,
            inntektEndring.getFraRefusjon(),
            inntektEndring.getTilRefusjon());
    }

    private void opprettHistorikkArbeidsinntekt(HistorikkInnslagTekstBuilder tekstBuilder, List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer, BeregningsgrunnlagPrStatusOgAndelEndring andelEndring, RefusjonEndring refusjonEndring) {
        String arbeidsforholdInfo = arbeidsgiverHistorikkinnslag.lagTekstForArbeidsgiver(
            andelEndring.getArbeidsgiver().get(),
            arbeidsforholdOverstyringer);
        tekstBuilder.medEndretFelt(HistorikkEndretFeltType.INNTEKT_FRA_ARBEIDSFORHOLD,
            arbeidsforholdInfo,
            tilInt(refusjonEndring.getFraRefusjon()),
            tilInt(refusjonEndring.getTilRefusjon()));
    }

    private Integer tilInt(BigDecimal bdBeløp) {
        return bdBeløp == null ? null : bdBeløp.intValue();
    }

}

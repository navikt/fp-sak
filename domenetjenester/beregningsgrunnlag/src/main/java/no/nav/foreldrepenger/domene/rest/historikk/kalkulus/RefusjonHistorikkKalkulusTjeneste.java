package no.nav.foreldrepenger.domene.rest.historikk.kalkulus;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkBeløp;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeregningsgrunnlagPrStatusOgAndelEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.RefusjonEndring;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.rest.historikk.ArbeidsgiverHistorikkinnslag;

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

    public Optional<HistorikkinnslagLinjeBuilder> lagHistorikkOmEndret(List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer,
                                                                       BeregningsgrunnlagPrStatusOgAndelEndring andelEndring) {
        if (!andelEndring.getAktivitetStatus().erArbeidstaker()) {
            return Optional.empty();
        }
        return andelEndring.getRefusjonEndring()
            .map(refusjonEndring -> opprettRefusjonHistorikkForArbeidstakerInntekt(arbeidsforholdOverstyringer, andelEndring, refusjonEndring));
    }

    private HistorikkinnslagLinjeBuilder opprettRefusjonHistorikkForArbeidstakerInntekt(List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer, BeregningsgrunnlagPrStatusOgAndelEndring andelEndring, RefusjonEndring refusjonEndring) {
        if (OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE.equals(andelEndring.getArbeidsforholdType())) {
            return fraTilEquals("Fastsett søkers månedsinntekt fra etterlønn eller sluttpakke",
                HistorikkBeløp.ofNullable(refusjonEndring.fraRefusjon()), HistorikkBeløp.ofNullable(refusjonEndring.tilRefusjon()));
        }

        var arbeidsforholdInfo = andelEndring.getArbeidsgiver()
            .map(arbeidsgiver -> arbeidsgiverHistorikkinnslag.lagTekstForArbeidsgiver(arbeidsgiver, arbeidsforholdOverstyringer))
            .orElse(andelEndring.getAktivitetStatus().getNavn());
        return fraTilEquals(String.format("Inntekt fra %s", arbeidsforholdInfo), HistorikkBeløp.ofNullable(refusjonEndring.fraRefusjon()),
            HistorikkBeløp.ofNullable(refusjonEndring.tilRefusjon()));
    }
}

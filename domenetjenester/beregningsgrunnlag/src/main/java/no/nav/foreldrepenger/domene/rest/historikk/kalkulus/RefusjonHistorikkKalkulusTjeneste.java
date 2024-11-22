package no.nav.foreldrepenger.domene.rest.historikk.kalkulus;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder.fraTilEquals;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder;
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

    public Optional<HistorikkinnslagTekstlinjeBuilder> lagHistorikkOmEndret(List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer, BeregningsgrunnlagPrStatusOgAndelEndring andelEndring) {
        if (!andelEndring.getAktivitetStatus().erArbeidstaker()) {
            return Optional.empty();
        }
        return andelEndring.getRefusjonEndring()
            .map(refusjonEndring -> opprettRefusjonHistorikkForArbeidstakerInntekt(arbeidsforholdOverstyringer, andelEndring, refusjonEndring));
    }

    private HistorikkinnslagTekstlinjeBuilder opprettRefusjonHistorikkForArbeidstakerInntekt(List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer, BeregningsgrunnlagPrStatusOgAndelEndring andelEndring, RefusjonEndring refusjonEndring) {
        if (OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE.equals(andelEndring.getArbeidsforholdType())) {
            return fraTilEquals("Fastsett søkers månedsinntekt fra etterlønn eller sluttpakke", refusjonEndring.getFraRefusjon(), refusjonEndring.getTilRefusjon());
        }

        var arbeidsforholdInfo = andelEndring.getArbeidsgiver().isPresent()
            ? arbeidsgiverHistorikkinnslag.lagTekstForArbeidsgiver(andelEndring.getArbeidsgiver().get(), arbeidsforholdOverstyringer)
            : andelEndring.getAktivitetStatus().getNavn();
        return fraTilEquals(
            String.format("Inntekt fra arbeidsforhold %s", arbeidsforholdInfo),
            tilInt(refusjonEndring.getFraRefusjon()),
            tilInt(refusjonEndring.getTilRefusjon()));
    }

    private Integer tilInt(BigDecimal bdBeløp) {
        return bdBeløp == null ? null : bdBeløp.intValue();
    }

}

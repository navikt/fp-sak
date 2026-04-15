package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import no.nav.foreldrepenger.domene.modell.kodeverk.Hjemmel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

record OversiktBeregningsgrunnlag(LocalDate skjæringstidspunkt,
                                  List<BeregningsAndel> beregningsandeler,
                                  List<BeregningAktivitetStatus> beregningAktivitetStatuser,
                                  BigDecimal grunnbeløp) {

    record BeregningsAndel(OversiktAktivitetStatus aktivitetStatus,
                           BigDecimal fastsattPrÅr,
                           InntektsKilde inntektsKilde,
                           Arbeidsforhold arbeidsforhold,
                           BigDecimal dagsatsArbeidsgiver,
                           BigDecimal dagsatsSøker) {
    }

    record Arbeidsforhold(String arbeidsgiverIdent, String arbeidsgivernavn, BigDecimal refusjonPrMnd) {
    }

    record BeregningAktivitetStatus(OversiktAktivitetStatus aktivitetStatus, Hjemmel hjemmel) {
    }

    enum InntektsKilde {
        INNTEKTSMELDING,
        A_INNTEKT,
        VEDTAK_ANNEN_YTELSE,
        SKJØNNSFASTSATT,
        PENSJONSGIVENDE_INNTEKT,
    }
}

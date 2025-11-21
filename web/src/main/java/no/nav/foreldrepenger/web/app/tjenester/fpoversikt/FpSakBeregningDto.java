package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.kodeverk.Hjemmel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

record FpSakBeregningDto(LocalDate skjæringsTidspunkt, List<BeregningsAndel> beregningsAndeler, List<BeregningAktivitetStatus> beregningAktivitetStatuser) {

    // TODO: bruke denne AktivtetStatus?
    record BeregningsAndel(AktivitetStatus aktivitetStatus, BigDecimal fastsattPrMnd, InntektsKilde inntektsKilde, Arbeidsforhold arbeidsforhold, BigDecimal dagsats) {}

    record Arbeidsforhold(String arbeidsgiverIdent, BigDecimal refusjonPrMnd) {}

    record BeregningAktivitetStatus(AktivitetStatus aktivitetStatus, Hjemmel hjemmel) {}

    enum InntektsKilde {
        INNTEKTSMELDING,
        A_INNTEKT,
        SKJØNNSFASTSATT,
        PGI // Pensjonsgivendeinntekt
    }
}

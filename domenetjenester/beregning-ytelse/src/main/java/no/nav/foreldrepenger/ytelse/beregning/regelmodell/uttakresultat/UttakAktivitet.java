package no.nav.foreldrepenger.ytelse.beregning.regelmodell.uttakresultat;

import java.math.BigDecimal;

import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;

public record UttakAktivitet(BigDecimal stillingsgrad,
                             BigDecimal arbeidstidsprosent,
                             BigDecimal utbetalingsgrad,
                             Arbeidsforhold arbeidsforhold,
                             AktivitetStatus aktivitetStatus,
                             boolean erGradering,
                             BigDecimal totalStillingsgradHosAG) {
}

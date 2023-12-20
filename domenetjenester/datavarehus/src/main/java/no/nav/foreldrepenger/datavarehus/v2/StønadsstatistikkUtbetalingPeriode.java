package no.nav.foreldrepenger.datavarehus.v2;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

record StønadsstatistikkUtbetalingPeriode(@NotNull LocalDate fom, @NotNull LocalDate tom,
                                          @NotNull Inntektskategori inntektskategori,
                                          String arbeidsgiver,  // Orgnummer eller aktørId
                                          @NotNull Mottaker mottaker,
                                          @NotNull Integer dagsats,
                                          @NotNull Integer dagsatsFraBeregningsgrunnlag,
                                          @NotNull BigDecimal utbetalingsgrad) {

    //Feriepenger ikke interessant - sier konsumenten
    enum Mottaker { BRUKER, ARBEIDSGIVER }

    enum Inntektskategori {
        ARBEIDSTAKER, ARBEIDSTAKER_UTEN_FERIEPENGER, SJØMANN, FRILANSER,
        DAGPENGER, ARBEIDSAVKLARINGSPENGER,
        SELVSTENDIG_NÆRINGSDRIVENDE, DAGMAMMA, JORDBRUKER, FISKER
    }

    @Override
    public String toString() {
        return "StønadsstatistikkUtbetalingPeriode{" + "fom=" + fom + ", tom=" + tom + ", inntektskategori=" + inntektskategori + ", mottaker="
            + mottaker + ", dagsats=" + dagsats + '}';
    }
}

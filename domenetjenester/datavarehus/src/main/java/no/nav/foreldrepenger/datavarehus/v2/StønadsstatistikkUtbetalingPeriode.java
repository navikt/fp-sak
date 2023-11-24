package no.nav.foreldrepenger.datavarehus.v2;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

record StønadsstatistikkUtbetalingPeriode(@NotNull LocalDate fom, @NotNull LocalDate tom,
                                          @NotNull String klasseKode, // Koder som brukes mot OS
                                          String arbeidsgiver,  // Orgnummer eller aktørId
                                          @NotNull Integer dagsats,
                                          BigDecimal stillingsprosent,
                                          @NotNull BigDecimal utbetalingsgrad) {

    //Feriepenger ikke interessant - sier konsumenten


    @Override
    public String toString() {
        return "StønadsstatistikkUtbetalingPeriode{" + "fom=" + fom + ", tom=" + tom + ", klasseKode='" + klasseKode + '\'' + ", dagsats=" + dagsats
            + ", stillingsprosent=" + stillingsprosent + ", utbetalingsgrad=" + utbetalingsgrad + '}';
    }
}

package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

record OversiktTilkjentYtelse(List<TilkjentYtelsePeriode> utbetalingsperioder, List<FeriepengeAndel> feriepenger) {

    record TilkjentYtelsePeriode(LocalDate fom, LocalDate tom, List<Andel> andeler) {
        record Andel(OversiktAktivitetStatus aktivitetStatus,
                     String arbeidsgiverIdent,
                     String arbeidsgivernavn,
                     BigDecimal dagsats,
                     boolean tilBruker,
                     BigDecimal utbetalingsgrad) {
        }
    }

    record FeriepengeAndel(LocalDate opptjeningsår, BigDecimal årsbeløp, String arbeidsgiverIdent, boolean tilBruker) {
    }
}

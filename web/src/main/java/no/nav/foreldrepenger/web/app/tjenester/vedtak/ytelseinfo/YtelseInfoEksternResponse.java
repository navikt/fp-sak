package no.nav.foreldrepenger.web.app.tjenester.vedtak.ytelseinfo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record YtelseInfoEksternResponse(Ytelse ytelse,
                                        String saksnummer,
                                        List<UtbetalingEksternDto> utbetalinger) {

    public record UtbetalingEksternDto(LocalDate fom, LocalDate tom, BigDecimal grad) { }

    public enum Ytelse { ENGANGSSTØNAD, FORELDREPENGER, SVANGERSKAPSPENGER }
}


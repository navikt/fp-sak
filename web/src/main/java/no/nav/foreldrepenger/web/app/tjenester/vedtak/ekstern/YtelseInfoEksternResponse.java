package no.nav.foreldrepenger.web.app.tjenester.vedtak.ekstern;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record YtelseInfoEksternResponse(UUID vedtaksreferanse,
                                        List<UtbetalingEksternDto> utbetalinger,
                                        LocalDateTime vedtattTidspunkt,
                                        Ytelse ytelse,
                                        String saksnummer) {

    public record UtbetalingEksternDto(LocalDate fom, LocalDate tom, BigDecimal grad) { }

    public enum Ytelse { ENGANGSTØNAD, FORELDREPENGER, SVANGERSKAPSPENGER }
}


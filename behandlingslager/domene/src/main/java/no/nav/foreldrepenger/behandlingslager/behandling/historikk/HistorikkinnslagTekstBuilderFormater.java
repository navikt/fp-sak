package no.nav.foreldrepenger.behandlingslager.behandling.historikk;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import no.nav.fpsak.tidsserie.LocalDateInterval;

public final class HistorikkinnslagTekstBuilderFormater {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");


    private HistorikkinnslagTekstBuilderFormater() {
    }

    public static <T> String formatString(T verdi) {
        if (verdi == null) {
            return null;
        }
        if (verdi instanceof LocalDate localDate) {
            return formatDate(localDate);
        }
        if (verdi instanceof LocalDateInterval interval) {
            return formatDate(interval.getFomDato()) + " - " + formatDate(interval.getTomDato());
        }
        return verdi.toString();
    }

    public static String formatDate(LocalDate localDate) {
        return DATE_FORMATTER.format(localDate);
    }
}

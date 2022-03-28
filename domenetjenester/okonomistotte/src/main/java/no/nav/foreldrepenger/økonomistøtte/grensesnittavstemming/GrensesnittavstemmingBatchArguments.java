package no.nav.foreldrepenger.økonomistøtte.grensesnittavstemming;

import static java.time.LocalDate.parse;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.time.temporal.ChronoUnit.DAYS;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import no.nav.foreldrepenger.batch.BatchArgument;
import no.nav.foreldrepenger.batch.BatchArguments;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;

public class GrensesnittavstemmingBatchArguments extends BatchArguments {

    private static final String ANTALL_DAGER_KEY = "antallDager";
    private static final String FOM_KEY = "fom";
    private static final String TOM_KEY = "tom";
    private static final String FAGOMRÅDE_KEY = "fagomrade";
    private static final String DATE_PATTERN = "dd-MM-yyyy";
    private static final Integer MAX_PERIOD = 7;

    @BatchArgument(beskrivelse = "Antall dager tilbake i tid det skal genereres rapport for.")
    private Integer antallDager;
    @BatchArgument(beskrivelse = "Fra og med dato på formatet '" + DATE_PATTERN + "'")
    private LocalDate fom;
    @BatchArgument(beskrivelse = "Til og med dato på formatet '" + DATE_PATTERN + "'")
    private LocalDate tom;
    @BatchArgument(beskrivelse = "Fagområde (REFUTG, FP, FPREF, SVP, SVPREF)")
    private String fagområde;
    private boolean harGenerertDatoer = false;

    GrensesnittavstemmingBatchArguments(Map<String, String> arguments) {
        super(arguments);

        if (antallDager != null && tom == null && fom == null) { // NOSONAR
            beregneFomOgTomDato();
            harGenerertDatoer = true;

        } else if (antallDager == null && tom == null && fom == null) {
            antallDager = 1;
            beregneFomOgTomDato();
            harGenerertDatoer = true;
        }
    }

    @Override
    public boolean settParameterVerdien(String key, String value) {
        if (ANTALL_DAGER_KEY.equals(key)) {
            this.antallDager = Integer.valueOf(value);
            return true;
        }
        if (FOM_KEY.equals(key)) {
            this.fom = parsedato(value);
            return true;
        }
        if (TOM_KEY.equals(key)) {
            this.tom = parsedato(value);
            return true;
        }
        if (FAGOMRÅDE_KEY.equals(key)) {
            this.fagområde = value;
            return true;
        }
        return false;
    }

    private void beregneFomOgTomDato() {
        fom = LocalDate.now().minusDays(antallDager);
        tom = LocalDate.now().minusDays(1);
    }

    private LocalDate parsedato(String datoString) {
        return Optional.ofNullable(datoString).map(dato -> parse(dato, ofPattern(DATE_PATTERN))).orElse(null);
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public String getFagområde() {
        return fagområde;
    }

    @Override
    public boolean isValid() {
        try {
            KodeFagområde.valueOf(getFagområde());
        } catch (Exception e) {
            return false;
        }

        if (antallDager != null) {
            return harGenerertDatoer && isValidPeriod();
        }
        return hasSetDates() && isValidDateRange() && isValidPeriod();
    }

    private boolean hasSetDates() {
        return fom != null && tom != null;
    }

    private boolean isValidDateRange() {
        return tom.isEqual(fom) || tom.isAfter(fom);
    }

    private boolean isValidPeriod() {
        return DAYS.between(fom, tom) <= MAX_PERIOD;
    }

    @Override
    public String toString() {
        return "GrensesnittavstemmingBatchArguments{" +
            "antallDager=" + antallDager +
            ", fom=" + fom +
            ", tom=" + tom +
            ", fagomrade=" + fagområde +
            '}';
    }
}

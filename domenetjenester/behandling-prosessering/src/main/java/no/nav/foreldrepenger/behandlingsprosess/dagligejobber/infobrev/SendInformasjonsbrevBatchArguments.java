package no.nav.foreldrepenger.behandlingsprosess.dagligejobber.infobrev;

import static java.time.LocalDate.parse;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.time.temporal.ChronoUnit.DAYS;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import no.nav.foreldrepenger.batch.BatchArgument;
import no.nav.foreldrepenger.batch.BatchArguments;

public class SendInformasjonsbrevBatchArguments extends BatchArguments {

    static final String FOM_KEY = "fom";
    static final String TOM_KEY = "tom";
    static final String DATE_PATTERN = "dd-MM-yyyy";
    private static final String ANTALL_DAGER_KEY = "antallDager";
    private static final Integer MAX_PERIOD = 365;

    @BatchArgument(beskrivelse = "Antall dager tilbake i tid det skal genereres rapport for.")
    private Integer antallDager;
    @BatchArgument(beskrivelse = "Fra og med dato på formatet '" + DATE_PATTERN + "'")
    private LocalDate fom;
    @BatchArgument(beskrivelse = "Til og med dato på formatet '" + DATE_PATTERN + "'")
    private LocalDate tom;
    private boolean harGenerertDatoer = false;

    private Integer antallUkerFremover;

    SendInformasjonsbrevBatchArguments(Map<String, String> arguments, Integer antallUkerFremover) {
        super(arguments);
        this.antallUkerFremover = antallUkerFremover;
        if ((antallDager != null) && (tom == null) && (fom == null)) {
            beregneFomOgTomDato();
            harGenerertDatoer = true;

        } else if ((antallDager == null) && (tom == null) && (fom == null)) {
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
        return false;
    }

    private void beregneFomOgTomDato() {
        fom = LocalDate.now().minusDays(antallDager).plusWeeks(antallUkerFremover);
        tom = LocalDate.now().minusDays(1).plusWeeks(antallUkerFremover);
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

    @Override
    public boolean isValid() {
        if (antallDager != null) {
            return harGenerertDatoer && isValidPeriod();
        }
        return hasSetDates() && isValidDateRange() && isValidPeriod();
    }

    private boolean hasSetDates() {
        return (fom != null) && (tom != null);
    }

    private boolean isValidDateRange() {
        return tom.isEqual(fom) || tom.isAfter(fom);
    }

    private boolean isValidPeriod() {
        return DAYS.between(fom, tom) <= MAX_PERIOD;
    }

    @Override
    public String toString() {
        return "SendInformasjonsbrevBatchArguments{" +
                "antallDager=" + antallDager +
                ", fom=" + fom +
                ", tom=" + tom +
                '}';
    }
}

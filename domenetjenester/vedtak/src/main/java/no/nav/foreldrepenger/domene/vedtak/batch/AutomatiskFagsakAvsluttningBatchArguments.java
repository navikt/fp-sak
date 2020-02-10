package no.nav.foreldrepenger.domene.vedtak.batch;

import static java.time.LocalDate.parse;
import static java.time.format.DateTimeFormatter.ofPattern;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import no.nav.foreldrepenger.batch.BatchArgument;
import no.nav.foreldrepenger.batch.BatchArguments;
import no.nav.vedtak.util.FPDateUtil;

public class AutomatiskFagsakAvsluttningBatchArguments extends BatchArguments {

    private static final String DATE_PATTERN = "dd-MM-yyyy";

    @BatchArgument(beskrivelse = "Dato '" + DATE_PATTERN + "'")
    private LocalDate date;

    @BatchArgument(beskrivelse = "Antall dager tilbake i tid det skal sjekkes for avsluttning av fagsak.")
    private Integer antallDager;

    public AutomatiskFagsakAvsluttningBatchArguments(Map<String, String> arguments) {
        super(arguments);

        if (date == null) { // NOSONAR
            beregneDato();
        }

        if (antallDager == null) { // NOSONAR
            this.antallDager = 0;
        }
    }

    @Override
    public boolean settParameterVerdien(String key, String value) {
        if ("date".equals(key)) {
            this.date = parsedato(value);
            return true;
        } else  if ("antallDager".equals(key)) {
            this.antallDager = Integer.valueOf(value);
            return true;
        }
        return false;
    }

    private void beregneDato() {
        date = FPDateUtil.iDag();
    }

    private LocalDate parsedato(String datoString) {
        return Optional.ofNullable(datoString).map(dato -> parse(dato, ofPattern(DATE_PATTERN))).orElse(null);
    }

    public LocalDate getDate() {
        return date;
    }

    public int getAntallDager() {
        return antallDager.intValue();
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public String toString() {
        return "AutomatiskFagsakAvsluttningBatchArguments{" +
            "Dato=" + date +
            '}';
    }
}

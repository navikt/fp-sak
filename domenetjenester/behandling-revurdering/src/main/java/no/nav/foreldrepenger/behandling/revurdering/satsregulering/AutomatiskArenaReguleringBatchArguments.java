package no.nav.foreldrepenger.behandling.revurdering.satsregulering;

import static java.time.format.DateTimeFormatter.ofPattern;

import java.time.LocalDate;
import java.util.Map;

import no.nav.foreldrepenger.batch.BatchArgument;
import no.nav.foreldrepenger.batch.BatchArguments;

public class AutomatiskArenaReguleringBatchArguments extends BatchArguments {

    static final LocalDate DATO = LocalDate.of(LocalDate.now().getYear(), 5, 1);
    static final String REVURDER_KEY = "revurder";
    static final String SATS_DATO_KEY = "satsDato";
    static final String DATE_PATTERN = "dd-MM-yyyy";

    @BatchArgument(beskrivelse = "Skal revurderinger opprettes.")
    private boolean revurder;
    @BatchArgument(beskrivelse = "Mandag etter ny sats i Arena '" + DATE_PATTERN + "'")
    private LocalDate satsDato;

    AutomatiskArenaReguleringBatchArguments(Map<String, String> arguments) {
        super(arguments);
    }

    @Override
    public boolean settParameterVerdien(String key, String value) {
        if (REVURDER_KEY.equals(key)) {
            this.revurder = Boolean.parseBoolean(value);
            return true;
        } else if (SATS_DATO_KEY.equals(key)) {
            this.satsDato = value == null ? null : LocalDate.parse(value, ofPattern(DATE_PATTERN));
            return (satsDato != null) && validerDatoer(satsDato);
        }
        return false;
    }

    boolean getSkalRevurdere() {
        return revurder;
    }

    LocalDate getSatsDato() {
        return satsDato;
    }

    @Override
    public boolean isValid() {
        return satsDato != null;
    }

    @Override
    public String toString() {
        return "AutomatiskArenaReguleringBatchArguments{revurder=" + revurder + ", satsDato=" + satsDato + '}';
    }

    private boolean validerDatoer(LocalDate satsDato) {
        return satsDato.isAfter(DATO);
    }
}

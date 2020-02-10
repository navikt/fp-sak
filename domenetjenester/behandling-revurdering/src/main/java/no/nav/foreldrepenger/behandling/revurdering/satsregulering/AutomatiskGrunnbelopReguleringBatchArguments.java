package no.nav.foreldrepenger.behandling.revurdering.satsregulering;

import java.util.Map;

import no.nav.foreldrepenger.batch.BatchArgument;
import no.nav.foreldrepenger.batch.BatchArguments;

public class AutomatiskGrunnbelopReguleringBatchArguments extends BatchArguments {

    public static final String REVURDER_KEY = "revurder";

    @BatchArgument(beskrivelse = "Skal revurderinger opprettes.")
    private boolean revurder;

    AutomatiskGrunnbelopReguleringBatchArguments(Map<String, String> arguments) {
        super(arguments);
    }

    @Override
    public boolean settParameterVerdien(String key, String value) {
        if (REVURDER_KEY.equals(key)) {
            this.revurder = Boolean.parseBoolean(value);
            return true;
        }
        return false;
    }

    public boolean getSkalRevurdere() {
        return revurder;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public String toString() {
        return "AutomatiskGrunnbelopReguleringBatchArguments{revurder=" + revurder + '}';
    }
}

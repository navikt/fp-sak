package no.nav.foreldrepenger.produksjonsstyring.batch;

import java.util.Map;

import no.nav.foreldrepenger.batch.BatchArgument;
import no.nav.foreldrepenger.batch.BatchArguments;

public class FlyttEnhetBatchArguments extends BatchArguments {

    private static final String ENHET_KEY = "enhet";

    @BatchArgument(beskrivelse = "Enhet som skal ommøbleres.")
    private String enhetId;

    FlyttEnhetBatchArguments(Map<String, String> arguments) {
        super(arguments);
    }

    @Override
    public boolean settParameterVerdien(String key, String value) {
        if (ENHET_KEY.equals(key)) {
            this.enhetId = value;
            return true;
        }
        return false;
    }

    public String getEnhetId() {
        return enhetId;
    }

    @Override
    public boolean isValid() {
        return enhetId != null;
    }

    @Override
    public String toString() {
        return "FlyttEnhetBatchArguments{enhet=" + enhetId + '}';
    }
}

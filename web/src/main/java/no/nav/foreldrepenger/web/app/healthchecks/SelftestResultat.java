package no.nav.foreldrepenger.web.app.healthchecks;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SelftestResultat {

    public enum AggregateResult {
        OK(0), ERROR(1), WARNING(2);

        private int intValue;

        AggregateResult(int intValue) {
            this.intValue = intValue;
        }

        int getIntValue() {
            return intValue;
        }
    }

    private String application;
    private LocalDateTime timestamp;
    private List<InternalResult> kritiskeResultater = new ArrayList<>();
    private List<InternalResult> ikkeKritiskeResultater = new ArrayList<>();

    public void leggTilResultatForKritiskTjeneste(boolean ready, String description, String endpoint) {
        kritiskeResultater.add(new InternalResult(ready, description, endpoint));
    }

    public void leggTilResultatForIkkeKritiskTjeneste(boolean ready, String description, String endpoint) {
        ikkeKritiskeResultater.add(new InternalResult(ready, description, endpoint));
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public AggregateResult getAggregateResult() {
        for (var result : kritiskeResultater) {
            if (!result.isReady()) {
                return AggregateResult.ERROR;
            }
        }
        for (var result : ikkeKritiskeResultater) {
            if (!result.isReady()) {
                return AggregateResult.WARNING;
            }
        }
        return AggregateResult.OK;
    }

    public List<InternalResult> getAlleResultater() {
        List<InternalResult> alle = new ArrayList<>();
        alle.addAll(kritiskeResultater);
        alle.addAll(ikkeKritiskeResultater);
        return alle;
    }

    public static class InternalResult {
        private boolean ready = false;
        private String description;
        private String endpoint;

        public InternalResult(boolean ready, String description, String endpoint) {
            this.ready = ready;
            this.description = description;
            this.endpoint = endpoint;
        }

        public boolean isReady() {
            return ready;
        }

        public void setReady(boolean ready) {
            this.ready = ready;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }
    }
}

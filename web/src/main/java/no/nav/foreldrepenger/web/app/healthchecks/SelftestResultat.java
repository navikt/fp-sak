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

    public record InternalResult(boolean isReady, String description, String endpoint) {
    }
}

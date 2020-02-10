package no.nav.foreldrepenger.batch.task;

import java.time.LocalTime;

class BatchConfig {
    String name;
    String params;
    int hour;
    int minute;

    BatchConfig(int time, int minutt, String name, String params) { // NOSONAR
        this.name = name;
        this.params = params;
        this.hour = time;
        this.minute = minutt;
    }

    BatchConfig(BatchConfig config, String appendParams) { // NOSONAR
        this.name = config.name;
        this.params = config.params + appendParams;
        this.hour = config.hour;
        this.minute = config.minute;
    }

    String getName() {
            return name;
        }

    String getParams() {
            return params;
        }

    LocalTime getKjøreTidspunkt() {
            return LocalTime.of(hour, minute);
        }

}

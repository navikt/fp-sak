package no.nav.foreldrepenger.økonomistøtte.queue.consumer;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.felles.jms.ToggleJms;

class FellesJmsToggle implements ToggleJms {

    private static final Environment ENV = Environment.current();
    private static final boolean MQ_DISABLED = ENV.getProperty("test.only.disable.mq", Boolean.class, false);

    private final boolean enabled;

    public FellesJmsToggle() {
        this.enabled = !ENV.isLocal() && !FellesJmsToggle.MQ_DISABLED;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isDisabled() {
        return !isEnabled();
    }
}

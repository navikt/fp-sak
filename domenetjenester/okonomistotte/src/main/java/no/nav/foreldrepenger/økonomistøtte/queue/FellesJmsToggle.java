package no.nav.foreldrepenger.økonomistøtte.queue;

import no.nav.foreldrepenger.felles.jms.ToggleJms;
import no.nav.foreldrepenger.konfig.Environment;

public class FellesJmsToggle implements ToggleJms {

    private static final Environment ENV = Environment.current();

    private final boolean enabled;

    public FellesJmsToggle() {
        this.enabled = !ENV.isLocal();
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isDisabled() {
        return !isEnabled();
    }
}

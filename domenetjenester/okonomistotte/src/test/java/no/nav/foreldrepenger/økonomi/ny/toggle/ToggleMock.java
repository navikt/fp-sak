package no.nav.foreldrepenger.økonomi.ny.toggle;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Specializes;

@Specializes
@ApplicationScoped
public class ToggleMock extends OppdragKjerneimplementasjonToggle {

    private boolean brukNyImpl = false;

    @Override
    public boolean brukNyImpl(Long behandlingId) {
        return brukNyImpl;
    }

    public void setBrukNyImpl(boolean brukNyImpl) {
        this.brukNyImpl = brukNyImpl;
    }
}

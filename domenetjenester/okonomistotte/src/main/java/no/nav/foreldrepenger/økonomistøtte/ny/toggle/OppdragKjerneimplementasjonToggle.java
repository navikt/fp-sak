package no.nav.foreldrepenger.økonomistøtte.ny.toggle;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OppdragKjerneimplementasjonToggle {

    OppdragKjerneimplementasjonToggle() {
        //cdi proxy
    }

    public boolean brukNyImpl() {
        return true;
    }
}

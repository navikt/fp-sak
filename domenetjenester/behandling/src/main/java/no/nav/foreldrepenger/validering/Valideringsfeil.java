package no.nav.foreldrepenger.validering;

import no.nav.vedtak.exception.TekniskException;

import java.util.Collection;

public class Valideringsfeil extends TekniskException {
    private final Collection<FeltFeilDto> feltFeil;

    public Valideringsfeil(Collection<FeltFeilDto> feltFeil) {
        super("VALIDERING", "Valideringsfeil");
        this.feltFeil = feltFeil;
    }

    public Collection<FeltFeilDto> getFeltFeil() {
        return feltFeil;
    }

}

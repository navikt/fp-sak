package no.nav.foreldrepenger.web.app.exceptions;

import no.nav.foreldrepenger.validering.FeltFeilDto;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public class FeilDto implements Serializable {

    private final String feilmelding;
    private final Collection<FeltFeilDto> feltFeil;
    private final FeilType type;

    public FeilDto(String feilmelding) {
        this(feilmelding, List.of());
    }

    public FeilDto(String feilmelding, Collection<FeltFeilDto> feltFeil) {
        this(null, feilmelding, feltFeil);
    }

    public FeilDto(FeilType type, String feilmelding) {
        this(type, feilmelding, List.of());
    }

    public FeilDto(FeilType type, String feilmelding, Collection<FeltFeilDto> feltFeil) {
        this.type = type;
        this.feilmelding = feilmelding;
        this.feltFeil = feltFeil;
    }

    public String getFeilmelding() {
        return feilmelding;
    }

    public Collection<FeltFeilDto> getFeltFeil() {
        return feltFeil;
    }

    public FeilType getType() {
        return type;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [feilmelding=" + feilmelding + ", feltFeil=" + feltFeil + ", type=" + type + "]";
    }
}

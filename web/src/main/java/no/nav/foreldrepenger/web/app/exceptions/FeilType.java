package no.nav.foreldrepenger.web.app.exceptions;

public enum FeilType {
    MANGLER_TILGANG_FEIL,
    TOMT_RESULTAT_FEIL,
    BEHANDLING_ENDRET_FEIL,
    GENERELL_FEIL;

    @Override
    public String toString() {
        return name();
    }
}

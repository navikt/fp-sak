package no.nav.foreldrepenger.web.app.exceptions;

public enum FeilType {
    MANGLER_TILGANG_FEIL,
    TOMT_RESULTAT_FEIL,
    BEHANDLING_ENDRET_FEIL,
    OPPDRAG_FORVENTET_NEDETID,
    GENERELL_FEIL;

    @Override
    public String toString() {
        return name();
    }
}

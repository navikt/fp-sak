package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

/**
 * Spesialhåndterte virksomhetsnumre
 */
public enum Spesialnummer {

    STORTINGET("874707112"),
    ;

    private final String orgnummer;

    Spesialnummer(String orgnummer) {
        this.orgnummer = orgnummer;
    }

    public String getOrgnummer() {
        return orgnummer;
    }
}

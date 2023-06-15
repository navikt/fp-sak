package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

public record UttakAktivitet(UttakAktivitet.Type type, Arbeidsgiver arbeidsgiver, String arbeidsforholdId) {
    public enum Type {
        ORDINÆRT_ARBEID,
        SELVSTENDIG_NÆRINGSDRIVENDE,
        FRILANS,
        ANNET
    }
}

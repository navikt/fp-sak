package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import com.fasterxml.jackson.annotation.JsonValue;

public record UttakAktivitet(UttakAktivitet.Type type, Arbeidsgiver arbeidsgiver, String arbeidsforholdId) {
    public enum Type {
        ORDINÆRT_ARBEID,
        SELVSTENDIG_NÆRINGSDRIVENDE,
        FRILANS,
        ANNET
    }

    public record Arbeidsgiver(@JsonValue String identifikator) {

        @Override
        public String toString() {
            return "Arbeidsgiver{" + "identifikator='***' + '}'";
        }
    }
}

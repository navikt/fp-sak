package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import com.fasterxml.jackson.annotation.JsonValue;

public record Arbeidsgiver(@JsonValue String identifikator) {

    @Override
    public String toString() {
        return "Arbeidsgiver{" + "identifikator='***' + '}'";
    }
}

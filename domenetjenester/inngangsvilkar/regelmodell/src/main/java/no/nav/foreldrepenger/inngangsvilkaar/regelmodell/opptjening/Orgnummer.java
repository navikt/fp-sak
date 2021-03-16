package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening;

import java.util.Objects;

public class Orgnummer implements AktivitetIdentifikator {

    private final String value;

    public Orgnummer(String value) {
        this.value = Objects.requireNonNull(value, "orgnr må ha en verdi");
    }

    @Override
    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return tilMaskertNummer(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        var orgnummer = (Orgnummer) o;
        return value.equals(orgnummer.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}

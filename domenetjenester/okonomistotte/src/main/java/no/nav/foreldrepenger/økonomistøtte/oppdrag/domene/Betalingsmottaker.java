package no.nav.foreldrepenger.økonomistøtte.oppdrag.domene;

import java.util.Comparator;
import java.util.Objects;

import no.nav.foreldrepenger.konfig.Environment;

public interface Betalingsmottaker {
    Bruker BRUKER = new Bruker();

    Comparator<Betalingsmottaker> COMPARATOR = (Betalingsmottaker a, Betalingsmottaker b) -> {
        if (a.equals(b)) {
            return 0;
        }
        if (a.equals(BRUKER)) {
            return -1;
        }
        if (b.equals(BRUKER)) {
            return 1;
        }
        return ((ArbeidsgiverOrgnr) a).compareTo((ArbeidsgiverOrgnr) b);
    };

    boolean erArbeidsgiver();

    static Betalingsmottaker forArbeidsgiver(String orgNr) {
        return new ArbeidsgiverOrgnr(orgNr);
    }

    class Bruker implements Betalingsmottaker {

        private Bruker() {
        }

        @Override
        public String toString() {
            return "Bruker";
        }

        @Override
        public boolean erArbeidsgiver() {
            return false;
        }
    }

    class ArbeidsgiverOrgnr implements Betalingsmottaker, Comparable<ArbeidsgiverOrgnr> {
        private String orgnr;

        private ArbeidsgiverOrgnr(String orgnr) {
            Objects.requireNonNull(orgnr, "orgnr kan ikke være null");
            if (!orgnr.matches("^[0-9]*$")) {
                throw new IllegalArgumentException("Ugyldig orgnr, skal kun bestå av tall");
            }
            if (orgnr.length() != 9) {
                throw new IllegalArgumentException("orgnr skal være 9 tegn");
            }
            this.orgnr = orgnr;
        }

        public String getOrgnr() {
            return orgnr;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            var that = (ArbeidsgiverOrgnr) o;
            return Objects.equals(orgnr, that.orgnr);
        }

        @Override
        public int hashCode() {
            return Objects.hash(orgnr);
        }

        @Override
        public String toString() {
            return "orgnr='" + (Environment.current().isProd() ? "MASKERT" : orgnr);
        }

        @Override
        public int compareTo(ArbeidsgiverOrgnr o) {
            return orgnr.compareTo(o.orgnr);
        }

        @Override
        public boolean erArbeidsgiver() {
            return true;
        }
    }

}

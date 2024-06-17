package no.nav.foreldrepenger.domene.uttak;

import java.time.LocalDate;

public class UttakPeriodeEndringDto {

    private LocalDate fom;

    private LocalDate tom;

    private TypeEndring typeEndring;

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public boolean getErSlettet() {
        return TypeEndring.SLETTET.equals(typeEndring);
    }

    public boolean getErLagtTil() {
        return TypeEndring.LAGT_TIL.equals(typeEndring);
    }

    public boolean getErEndret() {
        return TypeEndring.ENDRET.equals(typeEndring);
    }

    public enum TypeEndring {
        ENDRET,
        SLETTET,
        LAGT_TIL
    }

    public static class Builder {

        UttakPeriodeEndringDto kladd = new UttakPeriodeEndringDto();

        public Builder medPeriode(LocalDate fom, LocalDate tom) {
            kladd.fom = fom;
            kladd.tom = tom;
            return this;
        }

        public Builder medTypeEndring(TypeEndring typeEndring) {
            kladd.typeEndring = typeEndring;
            return this;
        }

        public UttakPeriodeEndringDto build() {
            return kladd;
        }
    }
}

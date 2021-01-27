package no.nav.foreldrepenger.økonomi.feriepengeavstemming;

import no.nav.foreldrepenger.domene.typer.Beløp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public class FeriepengeAndel {
    private final boolean brukerErMottaker;
    private final String orgnr;
    private final LocalDate opptjeningsår;
    private final Beløp beløp;

    public FeriepengeAndel(boolean brukerErMottaker, String orgnr, LocalDate opptjeningsår, Beløp beløp) {
        this.brukerErMottaker = brukerErMottaker;
        this.orgnr = orgnr;
        this.opptjeningsår = opptjeningsår;
        this.beløp = beløp;
    }

    public boolean isBrukerErMottaker() {
        return brukerErMottaker;
    }

    public String getOrgnr() {
        return orgnr;
    }

    public LocalDate getOpptjeningsår() {
        return opptjeningsår;
    }

    public Beløp getBeløp() {
        return beløp;
    }

    public MottakerPrÅr getMottakerPrÅr() {
        return new MottakerPrÅr(brukerErMottaker, orgnr, opptjeningsår);
    }

    protected class MottakerPrÅr {
        private final boolean søkerErMottaker;
        private final String orgnr;
        private final LocalDate opptjeningsår;

        public MottakerPrÅr(boolean søkerErMottaker, String orgnr, LocalDate opptjeningsår) {
            this.søkerErMottaker = søkerErMottaker;
            this.orgnr = orgnr;
            this.opptjeningsår = opptjeningsår;
        }

        public boolean isSøkerErMottaker() {
            return søkerErMottaker;
        }

        public String getOrgnr() {
            return orgnr;
        }

        public LocalDate getOpptjeningsår() {
            return opptjeningsår;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MottakerPrÅr mottaker = (MottakerPrÅr) o;
            return søkerErMottaker == mottaker.søkerErMottaker && Objects.equals(orgnr, mottaker.orgnr) && Objects.equals(opptjeningsår, mottaker.opptjeningsår);
        }

        @Override
        public int hashCode() {
            return Objects.hash(søkerErMottaker, orgnr, opptjeningsår);
        }
    }
}

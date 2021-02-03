package no.nav.foreldrepenger.økonomistøtte.dagytelse.wrapper;

import java.time.LocalDate;
import java.util.Objects;

import no.nav.foreldrepenger.domene.typer.Beløp;

public class TilkjenteFeriepengerPrÅr {

    private LocalDate opptjeningÅr;
    private Beløp årsbeløp;

    private TilkjenteFeriepenger tilkjenteFeriepenger;
    private TilkjentYtelseAndel tilkjentYtelseAndel;

    private TilkjenteFeriepengerPrÅr() {
    }

    public Beløp getÅrsbeløp() {
        return årsbeløp;
    }

    public LocalDate getOpptjeningÅr() {
        return opptjeningÅr;
    }

    public boolean skalTilBrukerEllerPrivatperson() {
        return tilkjentYtelseAndel.skalTilBrukerEllerPrivatperson();
    }

    public String getArbeidsforholdOrgnr() {
        return tilkjentYtelseAndel.getArbeidsforholdOrgnr();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(TilkjenteFeriepengerPrÅr eksisterende) {
        return new Builder(eksisterende);
    }

    public static class Builder {

        private TilkjenteFeriepengerPrÅr kladd;

        public Builder() {
            kladd = new TilkjenteFeriepengerPrÅr();
        }

        public Builder(TilkjenteFeriepengerPrÅr kladd) {
            this.kladd = kladd;
        }

        public Builder medOpptjeningÅr(LocalDate opptjeningÅr) {
            kladd.opptjeningÅr = opptjeningÅr;
            return this;
        }

        public Builder medÅrsbeløp(Beløp årsbeløp) {
            kladd.årsbeløp = årsbeløp;
            return this;
        }

        public TilkjenteFeriepengerPrÅr build(TilkjenteFeriepenger tilkjenteFeriepenger, TilkjentYtelseAndel tilkjentYtelseAndel) {
            kladd.tilkjenteFeriepenger = tilkjenteFeriepenger;
            TilkjenteFeriepenger.builder(tilkjenteFeriepenger).leggTilOppdragFeriepengerPrÅr(kladd);
            kladd.tilkjentYtelseAndel = tilkjentYtelseAndel;
            verifyStateForBuild();
            return kladd;
        }

        private void verifyStateForBuild() {
            Objects.requireNonNull(kladd.opptjeningÅr, "opptjeningÅr");
            Objects.requireNonNull(kladd.årsbeløp, "årsbeløp");
            Objects.requireNonNull(kladd.tilkjenteFeriepenger, "tilkjenteFeriepenger");
            Objects.requireNonNull(kladd.tilkjentYtelseAndel, "tilkjentYtelseAndel");
        }
    }
}

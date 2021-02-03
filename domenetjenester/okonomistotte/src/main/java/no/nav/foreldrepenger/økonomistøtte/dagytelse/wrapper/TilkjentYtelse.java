package no.nav.foreldrepenger.økonomistøtte.dagytelse.wrapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class TilkjentYtelse {

    private List<TilkjentYtelsePeriode> tilkjentYtelsePerioder = new ArrayList<>();
    private TilkjenteFeriepenger tilkjenteFeriepenger;
    private LocalDate endringsdato;

    public List<TilkjentYtelsePeriode> getTilkjentYtelsePerioder() {
        return tilkjentYtelsePerioder;
    }

    public Optional<TilkjenteFeriepenger> getTilkjenteFeriepenger() {
        return Optional.ofNullable(tilkjenteFeriepenger);
    }

    public Optional<LocalDate> getEndringsdato() {
        return Optional.ofNullable(endringsdato);
    }

    public void addOppdragPeriode(TilkjentYtelsePeriode tilkjentYtelsePeriode) {
        Objects.requireNonNull(tilkjentYtelsePeriode, "tilkjentYtelsePeriode");
        if (!tilkjentYtelsePerioder.contains(tilkjentYtelsePeriode)) {
            tilkjentYtelsePerioder.add(tilkjentYtelsePeriode);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(TilkjentYtelse eksisterende) {
        return new Builder(eksisterende);
    }

    public static class Builder {
        private TilkjentYtelse kladd;

        public Builder() {
            kladd = new TilkjentYtelse();
        }

        public Builder(TilkjentYtelse eksisterende) {
            this.kladd = eksisterende;
        }

        public Builder medOppdragFeriepenger(TilkjenteFeriepenger tilkjenteFeriepenger) {
            kladd.tilkjenteFeriepenger = tilkjenteFeriepenger;
            return this;
        }

        public Builder medEndringsdato(LocalDate endringsdato) {
            kladd.endringsdato = endringsdato;
            return this;
        }

        public TilkjentYtelse build() {
            verifyStateForBuild();
            return kladd;
        }

        private void verifyStateForBuild() {
            Objects.requireNonNull(kladd.tilkjentYtelsePerioder, "tilkjentYtelsePerioder");
        }
    }
}

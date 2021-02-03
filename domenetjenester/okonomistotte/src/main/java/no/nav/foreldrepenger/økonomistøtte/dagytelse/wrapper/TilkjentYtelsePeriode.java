package no.nav.foreldrepenger.økonomistøtte.dagytelse.wrapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import no.nav.fpsak.tidsserie.LocalDateInterval;

public class TilkjentYtelsePeriode {

    private LocalDateInterval periode;
    private List<TilkjentYtelseAndel> tilkjentYtelseAndeler = new ArrayList<>();
    private TilkjentYtelse tilkjentYtelse;

    public LocalDate getFom() {
        return periode.getFomDato();
    }

    public LocalDate getTom() {
        return periode.getTomDato();
    }

    public LocalDateInterval getPeriode() {
        return periode;
    }

    public List<TilkjentYtelseAndel> getTilkjentYtelseAndeler() {
        return tilkjentYtelseAndeler;
    }

    public boolean inneholder(LocalDate dato) {
        return periode.encloses(dato);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof TilkjentYtelsePeriode)) {
            return false;
        }
        TilkjentYtelsePeriode other = (TilkjentYtelsePeriode) obj;
        return Objects.equals(this.getFom(), other.getFom())
            && Objects.equals(this.getTom(), other.getTom());
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode);
    }

    public static Builder builder(TilkjentYtelsePeriode eksisterendeTilkjentYtelsePeriode) {
        return new Builder(eksisterendeTilkjentYtelsePeriode);
    }

    public static class Builder {
        private TilkjentYtelsePeriode tilkjentYtelsePeriodeMal;

        public Builder() {
            tilkjentYtelsePeriodeMal = new TilkjentYtelsePeriode();
        }

        public Builder(TilkjentYtelsePeriode eksisterendeTilkjentYtelsePeriode) {
            tilkjentYtelsePeriodeMal = eksisterendeTilkjentYtelsePeriode;
        }

        public Builder medPeriode(LocalDateInterval periode) {
            tilkjentYtelsePeriodeMal.periode = periode;
            return this;
        }

        public Builder leggTilOppdragAndel(TilkjentYtelseAndel tilkjentYtelseAndel) {
            tilkjentYtelsePeriodeMal.tilkjentYtelseAndeler.add(tilkjentYtelseAndel);
            return this;
        }

        public TilkjentYtelsePeriode build() {
            verifyStateForBuild();
            return tilkjentYtelsePeriodeMal;
        }

        public TilkjentYtelsePeriode build(TilkjentYtelse tilkjentYtelse) {
            tilkjentYtelsePeriodeMal.tilkjentYtelse = tilkjentYtelse;
            tilkjentYtelsePeriodeMal.tilkjentYtelse.addOppdragPeriode(tilkjentYtelsePeriodeMal);
            verifyStateForBuild();
            return tilkjentYtelsePeriodeMal;
        }

        void verifyStateForBuild() {
            Objects.requireNonNull(tilkjentYtelsePeriodeMal.tilkjentYtelseAndeler, "oppdragAndeler");
            Objects.requireNonNull(tilkjentYtelsePeriodeMal.periode, "periode");
        }
    }
}

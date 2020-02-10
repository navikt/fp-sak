package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TilkjenteFeriepenger {

    private TilkjentYtelse tilkjentYtelse;
    private List<TilkjenteFeriepengerPrÅr> tilkjenteFeriepengerPrÅrList = new ArrayList<>();

    public List<TilkjenteFeriepengerPrÅr> getTilkjenteFeriepengerPrÅrList() {
        return tilkjenteFeriepengerPrÅrList;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(TilkjenteFeriepenger eksisterende) {
        return new Builder(eksisterende);
    }

    public static class Builder {

        private TilkjenteFeriepenger kladd;

        public Builder() {
            kladd = new TilkjenteFeriepenger();
        }

        public Builder(TilkjenteFeriepenger eksisterende) {
            kladd = eksisterende;
        }

        public Builder leggTilOppdragFeriepengerPrÅr(TilkjenteFeriepengerPrÅr tilkjenteFeriepengerPrÅr) {
            kladd.tilkjenteFeriepengerPrÅrList.add(tilkjenteFeriepengerPrÅr);
            return this;
        }

        public TilkjenteFeriepenger build(TilkjentYtelse tilkjentYtelse) {
            kladd.tilkjentYtelse = tilkjentYtelse;
            TilkjentYtelse.builder(tilkjentYtelse)
                .medOppdragFeriepenger(kladd)
                .build();
            verifyStateForBuild();
            return kladd;
        }

        private void verifyStateForBuild() {
            Objects.requireNonNull(kladd.tilkjentYtelse, "tilkjentYtelse");
            Objects.requireNonNull(kladd.tilkjenteFeriepengerPrÅrList, "tilkjenteFeriepengerPrÅrList");
        }
    }
}

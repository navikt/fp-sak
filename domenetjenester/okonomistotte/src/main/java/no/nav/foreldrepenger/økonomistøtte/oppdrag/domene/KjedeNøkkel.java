package no.nav.foreldrepenger.økonomistøtte.oppdrag.domene;

import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.util.ØkonomiKodeKlassifikSortering;

public class KjedeNøkkel implements Comparable<KjedeNøkkel> {

    private final KodeKlassifik klassekode;
    private final Betalingsmottaker betalingsmottaker;
    private final Integer feriepengeÅr;
    private final Integer knektKjedeDel;

    public static KjedeNøkkel lag(KodeKlassifik klassekode, Betalingsmottaker betalingsmottaker) {
        return KjedeNøkkel.builder()
            .medKlassekode(klassekode)
            .medBetalingsmottaker(betalingsmottaker)
            .build();
    }

    public static KjedeNøkkel lag(KodeKlassifik klassekode, Betalingsmottaker betalingsmottaker, int feriepengeÅr) {
        return KjedeNøkkel.builder()
            .medKlassekode(klassekode)
            .medBetalingsmottaker(betalingsmottaker)
            .medFeriepengeÅr(feriepengeÅr)
            .build();
    }

    private KjedeNøkkel(KodeKlassifik klassekode, Betalingsmottaker betalingsmottaker, Integer feriepengeÅr, Integer knektKjedeDel) {
        this.klassekode = klassekode;
        this.betalingsmottaker = betalingsmottaker;
        this.feriepengeÅr = feriepengeÅr;
        this.knektKjedeDel = knektKjedeDel;
    }

    public KjedeNøkkel forNesteKnekteKjededel() {
        return new KjedeNøkkel(klassekode, betalingsmottaker, feriepengeÅr, knektKjedeDel != null ? knektKjedeDel + 1 : 1);
    }

    public KodeKlassifik getKlassekode() {
        return klassekode;
    }

    public Betalingsmottaker getBetalingsmottaker() {
        return betalingsmottaker;
    }

    public Integer getKnektKjedeDel() {
        return knektKjedeDel;
    }

    public Integer getFeriepengeÅr() {
        return feriepengeÅr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (KjedeNøkkel) o;
        return Objects.equals(klassekode, that.klassekode) &&
            Objects.equals(betalingsmottaker, that.betalingsmottaker) &&
            Objects.equals(feriepengeÅr, that.feriepengeÅr) &&
            Objects.equals(knektKjedeDel, that.knektKjedeDel)
            ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(klassekode, betalingsmottaker, feriepengeÅr, knektKjedeDel);
    }

    @Override
    public String toString() {
        return "KjedeNøkkel{" +
            "klassekode=" + klassekode +
            ", betalingsmottaker=" + betalingsmottaker +
            (feriepengeÅr != null ? ", feriepengeår=" + feriepengeÅr : "") +
            (knektKjedeDel != null ? ", knektKjedeDel=" + knektKjedeDel : "") +
            '}';
    }

    @Override
    public int compareTo(KjedeNøkkel o) {
        var mottakerSammenligning = Betalingsmottaker.COMPARATOR.compare(getBetalingsmottaker(), o.getBetalingsmottaker());
        if (mottakerSammenligning != 0) {
            return mottakerSammenligning;
        }
        var klassekodeSammenligning = Integer.compare(ØkonomiKodeKlassifikSortering.getSorteringsplassering(getKlassekode()), ØkonomiKodeKlassifikSortering.getSorteringsplassering(o.getKlassekode()));
        if (klassekodeSammenligning != 0) {
            return klassekodeSammenligning;
        }
        var feriepengeår = getFeriepengeÅr() != null ? getFeriepengeÅr() : 0;
        var annenFeriepengeår = o.getFeriepengeÅr() != null ? o.getFeriepengeÅr() : 0;
        var feriepengeSammenligning = Integer.compare(feriepengeår, annenFeriepengeår);
        if (feriepengeSammenligning != 0) {
            return feriepengeSammenligning;
        }
        var kjedeDel = getKnektKjedeDel() != null ? getKnektKjedeDel() : 0;
        var annenKjedeDel = o.getKnektKjedeDel() != null ? o.getKnektKjedeDel() : 0;
        return Integer.compare(kjedeDel, annenKjedeDel);
    }

    public boolean gjelderEngangsutbetaling() {
        return klassekode.gjelderFeriepenger() || klassekode.gjelderEngangsstønad();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(KodeKlassifik klassekode, Betalingsmottaker betalingsmottaker) {
        return new Builder(klassekode, betalingsmottaker);
    }

    public static class Builder {
        private KodeKlassifik klassekode;
        private Betalingsmottaker betalingsmottaker;
        private Integer feriepengeÅr;
        private Integer knektKjedeDel;

        private Builder() {
        }

        private Builder(KodeKlassifik klassekode, Betalingsmottaker betalingsmottaker) {
            this.klassekode = klassekode;
            this.betalingsmottaker = betalingsmottaker;
        }

        public Builder medKlassekode(KodeKlassifik klassekode) {
            this.klassekode = klassekode;
            return this;
        }

        public Builder medBetalingsmottaker(Betalingsmottaker betalingsmottaker) {
            this.betalingsmottaker = betalingsmottaker;
            return this;
        }

        public Builder medFeriepengeÅr(Integer feriepengeÅr) {
            this.feriepengeÅr = feriepengeÅr;
            return this;
        }

        public Builder medKnektKjedeDel(Integer knektKjedeDel) {
            this.knektKjedeDel = knektKjedeDel;
            return this;
        }

        public KjedeNøkkel build() {
            Objects.requireNonNull(klassekode, "klassekode mangler");
            Objects.requireNonNull(betalingsmottaker, "betalingsmottaker mangler");
            if (klassekode.gjelderFeriepenger()) {
                Objects.requireNonNull(feriepengeÅr, "feriepengeår kreves når klassekode " + klassekode + " gjelder ferie");
            } else if (feriepengeÅr != null) {
                throw new IllegalArgumentException("feriepengeår skal ikke benyttes når klassekode " + klassekode + " ikke gjelder ferie");
            }
            return new KjedeNøkkel(klassekode, betalingsmottaker, feriepengeÅr, knektKjedeDel);
        }
    }
}

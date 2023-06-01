package no.nav.foreldrepenger.økonomistøtte.oppdrag.domene;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.util.ØkonomiKodeKlassifikSortering;

public class KjedeNøkkel implements Comparable<KjedeNøkkel> {

    public static final MonthDay SLUTT_FERIEPENGER = MonthDay.of(5, 31);

    private final KodeKlassifik klassekode;
    private final Betalingsmottaker betalingsmottaker;
    private final LocalDate feriepengeMaksdato;
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
            .medOpptjeningsÅr(feriepengeÅr)
            .build();
    }

    public static KjedeNøkkel lag(KodeKlassifik klassekode, Betalingsmottaker betalingsmottaker, LocalDate feriepengerMaksdato) {
        return KjedeNøkkel.builder()
            .medKlassekode(klassekode)
            .medBetalingsmottaker(betalingsmottaker)
            .medFeriepengeMaksdato(feriepengerMaksdato)
            .build();
    }

    private KjedeNøkkel(KodeKlassifik klassekode, Betalingsmottaker betalingsmottaker, LocalDate feriepengeMaksdato, Integer knektKjedeDel) {
        this.klassekode = klassekode;
        this.betalingsmottaker = betalingsmottaker;
        this.feriepengeMaksdato = feriepengeMaksdato;
        this.knektKjedeDel = knektKjedeDel;
    }

    public KjedeNøkkel forNesteKnekteKjededel() {
        return new KjedeNøkkel(klassekode, betalingsmottaker, feriepengeMaksdato, knektKjedeDel != null ? knektKjedeDel + 1 : 1);
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

    public LocalDate getFeriepengeMaksdato() {
        return feriepengeMaksdato;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (KjedeNøkkel) o;
        return Objects.equals(klassekode, that.klassekode) &&
            Objects.equals(betalingsmottaker, that.betalingsmottaker) &&
            Objects.equals(feriepengeMaksdato, that.feriepengeMaksdato) &&
            Objects.equals(knektKjedeDel, that.knektKjedeDel)
            ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(klassekode, betalingsmottaker, feriepengeMaksdato, knektKjedeDel);
    }

    @Override
    public String toString() {
        return "KjedeNøkkel{" +
            "klassekode=" + klassekode +
            ", betalingsmottaker=" + betalingsmottaker +
            (feriepengeMaksdato != null ? ", feriepengemaksdato=" + feriepengeMaksdato : "") +
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
        var idag = LocalDate.now();
        var denneFeriepengeMaksdato = Optional.ofNullable(getFeriepengeMaksdato()).orElse(idag);
        var annenFeriepengeMaksdato = Optional.ofNullable(o.getFeriepengeMaksdato()).orElse(idag);
        var feriepengeSammenligning = denneFeriepengeMaksdato.compareTo(annenFeriepengeMaksdato);
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
        private LocalDate feriepengeMaksdato;
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

        public Builder medOpptjeningsÅr(Integer opptjeningsÅr) {
            this.feriepengeMaksdato = LocalDate.ofYearDay(opptjeningsÅr + 1, 1).with(SLUTT_FERIEPENGER);
            return this;
        }

        public Builder medFeriepengeMaksdato(LocalDate feriepengeMaksdato) {
            this.feriepengeMaksdato = feriepengeMaksdato;
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
                Objects.requireNonNull(feriepengeMaksdato, "feriepengeMaksdato kreves når klassekode " + klassekode + " gjelder ferie");
            } else if (feriepengeMaksdato != null) {
                throw new IllegalArgumentException("feriepengeMaksdato skal ikke benyttes når klassekode " + klassekode + " ikke gjelder ferie");
            }
            return new KjedeNøkkel(klassekode, betalingsmottaker, feriepengeMaksdato, knektKjedeDel);
        }
    }
}

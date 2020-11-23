package no.nav.foreldrepenger.økonomi.ny.domene;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeKlassifik;

public class KjedeNøkkel implements Comparable<KjedeNøkkel> {

    private ØkonomiKodeKlassifik klassekode;
    private Betalingsmottaker betalingsmottaker;
    private Integer feriepengeÅr;

    public KjedeNøkkel(ØkonomiKodeKlassifik klassekode, Betalingsmottaker betalingsmottaker) {
        Objects.requireNonNull(klassekode);
        Objects.requireNonNull(betalingsmottaker);
        this.klassekode = klassekode;
        this.betalingsmottaker = betalingsmottaker;
    }

    public KjedeNøkkel(ØkonomiKodeKlassifik klassekode, Betalingsmottaker betalingsmottaker, Integer feriepengeÅr) {
        this(klassekode, betalingsmottaker);
        this.feriepengeÅr = feriepengeÅr;
    }

    public ØkonomiKodeKlassifik getKlassekode() {
        return klassekode;
    }

    public Betalingsmottaker getBetalingsmottaker() {
        return betalingsmottaker;
    }

    public Integer getFeriepengeÅr() {
        return feriepengeÅr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KjedeNøkkel that = (KjedeNøkkel) o;
        return Objects.equals(klassekode, that.klassekode) &&
            Objects.equals(betalingsmottaker, that.betalingsmottaker) &&
            Objects.equals(feriepengeÅr, that.feriepengeÅr)
            ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(klassekode, betalingsmottaker, feriepengeÅr);
    }

    @Override
    public String toString() {
        return "KjedeNøkkel{" +
            "klassekode=" + klassekode +
            ", betalingsmottaker=" + betalingsmottaker +
            (feriepengeÅr != null ? ", feriepengeår=" + feriepengeÅr : "") +
            '}';
    }

    @Override
    public int compareTo(KjedeNøkkel o) {
        int mottakerSammenligning = Betalingsmottaker.COMPARATOR.compare(getBetalingsmottaker(), o.getBetalingsmottaker());
        if (mottakerSammenligning != 0) {
            return mottakerSammenligning;
        }
        int klassekodeSammenligning = Integer.compare(ØkonomiKodeKlassifikSortering.getSorteringsplassering(getKlassekode()), ØkonomiKodeKlassifikSortering.getSorteringsplassering(o.getKlassekode()));
        if (klassekodeSammenligning != 0) {
            return klassekodeSammenligning;
        }
        int feriepengeår = getFeriepengeÅr() != null ? getFeriepengeÅr() : 0;
        int annenFeriepengeår = o.getFeriepengeÅr() != null ? o.getFeriepengeÅr() : 0;
        return Integer.compare(feriepengeår, annenFeriepengeår);
    }

    public boolean gjelderFeriepenger() {
        return klassekode.gjelderFerie();
    }

    public static class ØkonomiKodeKlassifikSortering {

        private static final List<String> SUFFIX_SORTERING = Arrays.asList(
            "ATORD", "ATAL", "ATFRI", "ATSJO",
            "SND-OP", "SNDDM-OP", "SNDJB-OP", "SNDFI",
            "FRISINN-FRILANS", "FRISINN-SELVST-OP",
            "REFAG-IOP",
            "FER", "FER-IOP", "FERPP-IOP"
        );

        public static int getSorteringsplassering(ØkonomiKodeKlassifik økonomiKodeKlassifik) {
            for (int i = 0; i < SUFFIX_SORTERING.size(); i++) {
                if (økonomiKodeKlassifik.getKodeKlassifik().endsWith(SUFFIX_SORTERING.get(i))) {
                    return i;
                }
            }
            throw new IllegalArgumentException("Ikke-definert sorteringsplassering for " + økonomiKodeKlassifik);
        }
    }
}

package no.nav.foreldrepenger.økonomistøtte.oppdrag.domene;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.Virkedager;

public class YtelsePeriode {

    private Periode periode;
    private YtelseVerdi verdi;

    public YtelsePeriode(Periode periode, YtelseVerdi verdi) {
        Objects.requireNonNull(periode);
        Objects.requireNonNull(verdi);
        this.periode = periode;
        this.verdi = verdi;
    }

    public YtelsePeriode(Periode periode, Satsen sats) {
        this(periode, new YtelseVerdi(sats));
    }

    public YtelsePeriode(Periode periode, Satsen sats, Utbetalingsgrad utbetalingsgrad) {
        this(periode, new YtelseVerdi(sats, utbetalingsgrad));
    }

    public Periode getPeriode() {
        return periode;
    }

    public YtelseVerdi getVerdi() {
        return verdi;
    }

    public Satsen getSats() {
        return verdi.getSats();
    }

    public Utbetalingsgrad getUtbetalingsgrad() {
        return verdi.getUtbetalingsgrad();
    }

    public YtelsePeriode fraOgMed(LocalDate nyFomDato) {
        if (!periode.overlapper(nyFomDato)) {
            throw new IllegalArgumentException("Kan ikke sette ny fom-dato som ikke er i eksisterende periode");
        }
        var satsType = verdi.getSats().getSatsType();
        if (satsType != SatsType.DAG) {
            throw new IllegalArgumentException("Kan ikke sette ny fom-dato for satsType: " + satsType);
        }
        var nyPeriode = Periode.of(nyFomDato, periode.getTom());
        return new YtelsePeriode(nyPeriode, verdi);
    }

    public static <T> YtelsePeriode summer(Collection<T> perioder, Function<T, YtelsePeriode> periodeKonverterer) {
        return summer(perioder.stream().map(periodeKonverterer).collect(Collectors.toList()));
    }

    public static YtelsePeriode summer(Collection<YtelsePeriode> perioder) {
        if (perioder.isEmpty()) {
            throw new IllegalArgumentException("Kan ikke summer 0 perioder");
        }
        if (perioder.size() == 1) {
            return perioder.iterator().next();
        }

        var iterator = perioder.iterator();
        var førstePeriode = iterator.next();
        var satsType = førstePeriode.getSats().getSatsType();
        while (iterator.hasNext()) {
            var periode = iterator.next();
            validerNøkler(førstePeriode, periode);
        }
        var sats = summerSats(satsType, perioder);
        var utbetalingsgrad = summerUtbetalingsgrad(perioder);
        return new YtelsePeriode(førstePeriode.getPeriode(), sats, utbetalingsgrad);
    }

    private static Utbetalingsgrad summerUtbetalingsgrad(Collection<YtelsePeriode> perioder) {
        var utbetalingsgrad = perioder.stream()
            .map(YtelsePeriode::getUtbetalingsgrad)
            .filter(Objects::nonNull)
            .map(Utbetalingsgrad::getUtbetalingsgrad)
            .reduce(Integer::sum)
            .orElse(null);
        if (utbetalingsgrad == null) {
            return null;
        }
        return new Utbetalingsgrad(utbetalingsgrad > 100 ? 100 : utbetalingsgrad);
    }

    private static Satsen summerSats(SatsType satsType, Collection<YtelsePeriode> perioder) {
        var sats = perioder.stream()
            .map(YtelsePeriode::getSats)
            .mapToLong(Satsen::getSats)
            .reduce(Long::sum)
            .orElseThrow();
        return new Satsen(satsType, sats);
    }

    public static void validerNøkler(YtelsePeriode p1, YtelsePeriode p2) {
        if (!p1.getPeriode().equals(p2.getPeriode())) {
            throw new IllegalArgumentException("Kan ikke slå sammen YtelsePeriode med ulik tidsperiode");
        }
        if (p1.getVerdi().getSats().getSatsType() != p2.getVerdi().getSats().getSatsType()) {
            throw new IllegalArgumentException("Kan ikke slå sammen YtelsePeriode med ulik satstype");
        }
        if ((p1.getUtbetalingsgrad() == null) != (p2.getUtbetalingsgrad() == null)) {
            throw new IllegalArgumentException("Kan ikke slå periode med utbetalingsgrad med en som ikke har det");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (YtelsePeriode) o;
        return periode.equals(that.periode) &&
            verdi.equals(that.verdi);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, verdi);
    }

    @Override
    public String toString() {
        return "YtelsePeriode{" +
            "periode=" + periode +
            ", sats=" + getSats() +
            ", utbetalingsgrad=" + getUtbetalingsgrad() +
            '}';
    }


    public long summerYtelse() {
        return switch (verdi.getSats().getSatsType()) {
            case DAG -> verdi.getSats().getSats() * Virkedager.beregnAntallVirkedager(periode.getFom(), periode.getTom());
            case ENG -> verdi.getSats().getSats();
            default -> throw new IllegalArgumentException("Ikke-støttet sats-type: " + verdi.getSats().getSatsType());
        };
    }
}

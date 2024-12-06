package no.nav.foreldrepenger.behandlingslager.behandling.historikk;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2.BOLD_MARKØR;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.domene.tid.AbstractLocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateInterval;

public class HistorikkinnslagLinjeBuilder {

    public static final HistorikkinnslagLinjeBuilder LINJESKIFT = linjeskift();

    private static HistorikkinnslagLinjeBuilder linjeskift() {
        var b = new HistorikkinnslagLinjeBuilder();
        b.type = HistorikkinnslagLinjeType.LINJESKIFT;
        return b;
    }

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final StringBuilder stringBuilder = new StringBuilder();
    private HistorikkinnslagLinjeType type = HistorikkinnslagLinjeType.TEKST;

    public HistorikkinnslagLinjeBuilder bold(String b) {
        stringBuilder.append(" ").append(BOLD_MARKØR).append(b).append(BOLD_MARKØR);
        return this;
    }

    public HistorikkinnslagLinjeBuilder bold(Integer integer) {
        return bold(String.valueOf(integer));
    }

    public HistorikkinnslagLinjeBuilder bold(LocalDate dato) {
        return bold(DATE_FORMATTER.format(dato));
    }

    public HistorikkinnslagLinjeBuilder tekst(String t) {
        stringBuilder.append(" ").append(t);
        return this;
    }

    public HistorikkinnslagLinjeBuilder tekst(LocalDate dato) {
        return tekst(DATE_FORMATTER.format(dato));
    }

    public HistorikkinnslagLinjeBuilder fraTil(String hva, String fra, String til) {
        if (Objects.equals(fra, til)) {
            throw new IllegalArgumentException("Like verdier " + fra);
        }
        if (fra == null) {
            return bold(hva).tekst("er satt til").bold(til);
        }
        if (til == null) {
            return bold(hva).bold(fra).tekst("er fjernet");
        }
        return bold(hva).tekst("er endret fra").tekst(fra).tekst("til").bold(til);
    }

    public HistorikkinnslagLinjeBuilder fraTil(String hva, Kodeverdi fra, Kodeverdi til) {
        return fraTil(hva, format(fra), format(til));
    }

    public HistorikkinnslagLinjeBuilder fraTil(String hva, Boolean fra, boolean til) {
        return fraTil(hva, format(fra), format(til));
    }

    public HistorikkinnslagLinjeBuilder fraTil(String hva, BigDecimal fra, BigDecimal til) {
        return fraTil(hva, format(fra), format(til));
    }

    public HistorikkinnslagLinjeBuilder fraTil(String hva, LocalDate fra, LocalDate til) {
        return fraTil(hva, format(fra), format(til));
    }

    public HistorikkinnslagLinjeBuilder fraTil(String hva, Integer fra, Integer til) {
        return fraTil(hva, format(fra), format(til));
    }

    public HistorikkinnslagLinjeBuilder til(String hva, Number til) {
        return fraTil(hva, null, format(til));
    }

    public HistorikkinnslagLinjeBuilder til(String hva, LocalDate til) {
        return fraTil(hva, null, til);
    }

    public HistorikkinnslagLinjeBuilder til(String hva, Kodeverdi til) {
        return fraTil(hva, null, til);
    }

    public HistorikkinnslagLinjeBuilder til(String hva, String til) {
        return fraTil(hva, null, til);
    }

    public HistorikkinnslagLinjeBuilder til(String hva, boolean til) {
        return fraTil(hva, null, til);
    }

    public static HistorikkinnslagLinjeBuilder fraTilEquals(String hva, String fra, String til) {
        if (Objects.equals(fra, til)) {
            return null;
        }
        return new HistorikkinnslagLinjeBuilder().fraTil(hva, fra, til);
    }

    public static HistorikkinnslagLinjeBuilder fraTilEquals(String hva, Kodeverdi fra, Kodeverdi til) {
        return fraTilEquals(hva, format(fra), format(til));
    }

    public static HistorikkinnslagLinjeBuilder fraTilEquals(String hva, BigDecimal fra, BigDecimal til) {
        return fraTilEquals(hva, format(fra), format(til));
    }

    public static HistorikkinnslagLinjeBuilder fraTilEquals(String hva, Boolean fra, boolean til) {
        return fraTilEquals(hva, format(fra), format(til));
    }

    public static HistorikkinnslagLinjeBuilder fraTilEquals(String hva, LocalDate fra, LocalDate til) {
        return fraTilEquals(hva, format(fra), format(til));
    }

    public static HistorikkinnslagLinjeBuilder fraTilEquals(String hva, Number fra, Number til) {
        return fraTilEquals(hva, format(fra), format(til));
    }

    public String tilTekst() {
        if (type == HistorikkinnslagLinjeType.TEKST && stringBuilder.isEmpty()) {
            throw new IllegalArgumentException("Forventer ikke tom streng for type " + type);
        }
        if (type == HistorikkinnslagLinjeType.LINJESKIFT && !stringBuilder.isEmpty()) {
            throw new IllegalStateException("Kan ikke kombinere linjeskift og tekst");
        }
        return stringBuilder.delete(0, 1).toString();
    }

    public static <T> String format(T verdi) {
        return switch (verdi) {
            case null -> null;
            case LocalDate localDate -> DATE_FORMATTER.format(localDate);
            case LocalDateInterval interval -> DATE_FORMATTER.format(interval.getFomDato()) + " - " + DATE_FORMATTER.format(interval.getTomDato());
            case AbstractLocalDateInterval interval -> DATE_FORMATTER.format(interval.getFomDato()) + " - " + DATE_FORMATTER.format(interval.getTomDato());
            case BigDecimal bd -> bd.toString();
            case Number n -> n.toString();
            case Boolean b -> b ? "Ja" : "Nei";
            case Kodeverdi k -> k.getNavn();
            default -> throw new IllegalStateException("Ikke støttet historikkformatering for " + verdi);
        };
    }

    public HistorikkinnslagLinjeType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "HistorikkinnslagLinjeBuilder{" + "tekst='" + "***" + '\'' + '}';
    }
}

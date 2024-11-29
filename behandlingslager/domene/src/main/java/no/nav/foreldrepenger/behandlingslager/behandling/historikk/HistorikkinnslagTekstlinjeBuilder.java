package no.nav.foreldrepenger.behandlingslager.behandling.historikk;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstBuilderFormater.formatDate;
import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstBuilderFormater.formatString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public class HistorikkinnslagTekstlinjeBuilder {

    private static final String LINJESKIFT = "linjeskift";

    private final StringBuilder stringBuilder = new StringBuilder();

    public HistorikkinnslagTekstlinjeBuilder bold(String b) {
        stringBuilder.append(" __").append(b).append("__");
        return this;
    }

    public HistorikkinnslagTekstlinjeBuilder bold(Integer integer) {
        return bold(String.valueOf(integer));
    }

    public HistorikkinnslagTekstlinjeBuilder tekst(String t) {
        stringBuilder.append(" ").append(t);
        return this;
    }

    public HistorikkinnslagTekstlinjeBuilder linjeskift() {
        stringBuilder.append(LINJESKIFT);
        return this;
    }

    public HistorikkinnslagTekstlinjeBuilder tekst(LocalDate dato) {
        return tekst(formatDate(dato));
    }

    public HistorikkinnslagTekstlinjeBuilder fraTil(String hva, String fra, String til) {
        if (Objects.equals(fra, til)) {
            throw new IllegalArgumentException("Like verdier " + fra);
        }
        if (fra == null) {
            return bold(hva).tekst("er satt til").bold(til);
        }
        if (til == null) {
            //TODO TFP-5554 tekst for at noe er fjernet. Trenger vi?
            return bold(hva).tekst("er fjernet");
        }
        return bold(hva).tekst("er endret fra").tekst(fra).tekst("til").bold(til);
    }

    public HistorikkinnslagTekstlinjeBuilder fraTil(String hva, Kodeverdi fra, Kodeverdi til) {
        return fraTil(hva, fra == null ? null : fra.getNavn(), til.getNavn());
    }

    public HistorikkinnslagTekstlinjeBuilder til(String hva, Number til) {
        return fraTil(hva, null, formatString(til));
    }

    public HistorikkinnslagTekstlinjeBuilder til(String hva, LocalDate til) {
        return fraTil(hva, null, til);
    }

    public HistorikkinnslagTekstlinjeBuilder til(String hva, Kodeverdi til) {
        return fraTil(hva, null, til);
    }

    public HistorikkinnslagTekstlinjeBuilder til(String hva, String til) {
        return fraTil(hva, null, til);
    }

    public HistorikkinnslagTekstlinjeBuilder til(String hva, boolean til) {
        return fraTil(hva, null, til);
    }

    public HistorikkinnslagTekstlinjeBuilder fraTil(String hva, Boolean fra, boolean til) {
        var fraTekst = fra == null ? null : fra ? "Ja" : "Nei";
        var tilTekst = til ? "Ja" : "Nei";
        return fraTil(hva, fraTekst, tilTekst);
    }

    public HistorikkinnslagTekstlinjeBuilder fraTil(String hva, BigDecimal fra, BigDecimal til) {
        var fraTekst = fra == null ? null : fra.toString();
        var tilTekst = til == null ? null : til.toString();
        return fraTil(hva, fraTekst, tilTekst);
    }

    public HistorikkinnslagTekstlinjeBuilder fraTil(String hva, LocalDate fra, LocalDate til) {
        var fraTekst = fra != null ? formatDate(fra) : null;
        var tilTekst = til != null ? formatDate(til) : null;
        return fraTil(hva, fraTekst, tilTekst);
    }

    public HistorikkinnslagTekstlinjeBuilder fraTil(String hva, Integer fra, Integer til) {
        var fraTekst = fra == null ? null : fra.toString();
        var tilTekst = til == null ? null : til.toString();
        return fraTil(hva, fraTekst, tilTekst);
    }

    public static HistorikkinnslagTekstlinjeBuilder fraTilEquals(String hva, Kodeverdi fra, Kodeverdi til) {
        if (Objects.equals(fra, til)) {
            return null;
        }
        return new HistorikkinnslagTekstlinjeBuilder().fraTil(hva, fra, til);
    }

    public static HistorikkinnslagTekstlinjeBuilder fraTilEquals(String hva, String fra, String til) {
        if (Objects.equals(fra, til)) {
            return null;
        }
        return new HistorikkinnslagTekstlinjeBuilder().fraTil(hva, fra, til);
    }

    public static HistorikkinnslagTekstlinjeBuilder fraTilEquals(String hva, Boolean fra, boolean til) {
        if (Objects.equals(fra, til)) {
            return null;
        }
        return new HistorikkinnslagTekstlinjeBuilder().fraTil(hva, fra, til);
    }

    public static HistorikkinnslagTekstlinjeBuilder fraTilEquals(String hva, LocalDate fra, LocalDate til) {
        if (Objects.equals(fra, til)) {
            return null;
        }
        var fraTekst = fra != null ? formatDate(fra) : null;
        var tilTekst = til != null ? formatDate(til) : null;
        return new HistorikkinnslagTekstlinjeBuilder().fraTil(hva, fraTekst, tilTekst);
    }

    public static HistorikkinnslagTekstlinjeBuilder fraTilEquals(String hva, Number fra, Number til) {
        if (Objects.equals(fra, til)) {
            return null;
        }
        return new HistorikkinnslagTekstlinjeBuilder().fraTil(hva, fra != null ? formatString(fra) : null, formatString(til));
    }

    public String build() {
        if (LINJESKIFT.equals(stringBuilder)) {
            return stringBuilder.toString();
        }
        return stringBuilder.delete(0, 1).toString();
    }

    @Override
    public String toString() {
        return "Tekstlinje{" + "tekst='" + "***" + '\'' + '}';
    }

}

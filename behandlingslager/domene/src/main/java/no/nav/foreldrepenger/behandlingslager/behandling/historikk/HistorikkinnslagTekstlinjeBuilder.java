package no.nav.foreldrepenger.behandlingslager.behandling.historikk;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public class HistorikkinnslagTekstlinjeBuilder {

    private final StringBuilder stringBuilder = new StringBuilder();
    private final String LINJESKIFT = "linjeskift";

    public HistorikkinnslagTekstlinjeBuilder b(String b) {
        stringBuilder.append(" __").append(b).append("__");
        return this;
    }

    public HistorikkinnslagTekstlinjeBuilder t(String t) {
        stringBuilder.append(" ").append(t);
        return this;
    }

    public HistorikkinnslagTekstlinjeBuilder linjeskift() {
        stringBuilder.append(LINJESKIFT);
        return this;
    }

    public HistorikkinnslagTekstlinjeBuilder t(LocalDate dato) {
        return t(HistorikkinnslagTekstBuilderFormater.formatDate(dato));
    }

    public HistorikkinnslagTekstlinjeBuilder fraTil(String hva, String fra, String til) {
        if (Objects.equals(fra, til)) {
            throw new IllegalArgumentException("Like verdier " + fra);
        }
        if (fra == null) {
            return b(hva).t("er satt til").b(til);
        }
        if (til == null) {
            //TODO tekst for at noe er fjernet. Trenger vi?
            return b(hva).t("er fjernet");
        }
        return b(hva).t("er endret fra").t(fra).t("til").b(til);
    }

    public HistorikkinnslagTekstlinjeBuilder fraTil(String hva, Kodeverdi fra, Kodeverdi til) {
        return fraTil(hva, fra == null ? null : fra.getNavn(), til.getNavn());
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

    public HistorikkinnslagTekstlinjeBuilder p() {
        stringBuilder.append(".");
        return this;
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

    public String build() {
        return stringBuilder.delete(0, 1).toString();
    }

    @Override
    public String toString() {
        return "Tekstlinje{" + "tekst='" + "***" + '\'' + '}';
    }

}

package no.nav.foreldrepenger.domene.oppdateringresultat;

import static java.util.Collections.singletonList;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningAvklaringsbehovDefinisjon;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningVenteårsak;

public class BeregningAvklaringsbehovResultat {

    private final BeregningAvklaringsbehovDefinisjon beregningAvklaringsbehovDefinisjon;
    private BeregningVenteårsak venteårsak;
    private LocalDateTime ventefrist;

    private BeregningAvklaringsbehovResultat(BeregningAvklaringsbehovDefinisjon aksjonspunktDefinisjon) {
        this.beregningAvklaringsbehovDefinisjon = aksjonspunktDefinisjon;
    }

    private BeregningAvklaringsbehovResultat(BeregningAvklaringsbehovDefinisjon avklaringsbehovDefinisjon, BeregningVenteårsak venteårsak, LocalDateTime ventefrist) {
        this.beregningAvklaringsbehovDefinisjon = avklaringsbehovDefinisjon;
        this.venteårsak = venteårsak;
        this.ventefrist = ventefrist;
    }

    /**
     * Factory-metode direkte basert på {@link BeregningAvklaringsbehovDefinisjon}. Ingen callback for consumer.
     */
    public static BeregningAvklaringsbehovResultat opprettFor(BeregningAvklaringsbehovDefinisjon avklaringsbehovDefinisjon) {
        return new BeregningAvklaringsbehovResultat(avklaringsbehovDefinisjon);
    }

    /**
     * Factory-metode direkte basert på {@link BeregningAvklaringsbehovDefinisjon}, returnerer liste. Ingen callback for consumer.
     */
    public static List<BeregningAvklaringsbehovResultat> opprettListeFor(BeregningAvklaringsbehovDefinisjon avklaringsbehovDefinisjon) {
        return singletonList(new BeregningAvklaringsbehovResultat(avklaringsbehovDefinisjon));
    }

    /**
     * Factory-metode som linker {@link BeregningAvklaringsbehovDefinisjon} sammen med callback for consumer-operasjon.
     */
    public static BeregningAvklaringsbehovResultat opprettMedFristFor(BeregningAvklaringsbehovDefinisjon avklaringsbehovDefinisjon, BeregningVenteårsak venteårsak, LocalDateTime ventefrist) {
        return new BeregningAvklaringsbehovResultat(avklaringsbehovDefinisjon, venteårsak, ventefrist);
    }

    public BeregningAvklaringsbehovDefinisjon getBeregningAvklaringsbehovDefinisjon() {
        return beregningAvklaringsbehovDefinisjon;
    }

    public BeregningVenteårsak getVenteårsak() {
        return venteårsak;
    }

    public LocalDateTime getVentefrist() {
        return ventefrist;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            beregningAvklaringsbehovDefinisjon.getKode() + ":" + beregningAvklaringsbehovDefinisjon.getNavn() +
            ", venteårsak=" + getVenteårsak() +
            ", ventefrist=" + getVentefrist() +
            ">";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof BeregningAvklaringsbehovResultat))
            return false;

        BeregningAvklaringsbehovResultat that = (BeregningAvklaringsbehovResultat) o;

        return beregningAvklaringsbehovDefinisjon.getKode().equals(that.beregningAvklaringsbehovDefinisjon.getKode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(beregningAvklaringsbehovDefinisjon.getKode());
    }

    public boolean harFrist() {
        return null != getVentefrist();
    }
}

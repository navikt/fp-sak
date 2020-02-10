package no.nav.foreldrepenger.behandlingskontroll;

import static java.util.Collections.singletonList;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;

/**
 * Knytter {@link AksjonspunktDefinisjon} med en callback for å modifisere aksjonpunktet som blir opprettet.
 */
public class AksjonspunktResultat {

    private AksjonspunktDefinisjon aksjonspunktDefinisjon;
    private Venteårsak venteårsak;
    private LocalDateTime frist;


    private AksjonspunktResultat(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        this.aksjonspunktDefinisjon = aksjonspunktDefinisjon;
    }

    private AksjonspunktResultat(AksjonspunktDefinisjon aksjonspunktDefinisjon,
                                 Venteårsak venteårsak,
                                 LocalDateTime ventefrist) {
        this.aksjonspunktDefinisjon = aksjonspunktDefinisjon;
        this.venteårsak = venteårsak;
        this.frist = ventefrist;
    }

    /**
     * Factory-metode direkte basert på {@link AksjonspunktDefinisjon}. Ingen frist eller årsak.
     */
    public static AksjonspunktResultat opprettForAksjonspunkt(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return new AksjonspunktResultat(aksjonspunktDefinisjon);
    }

    /**
     * Factory-metode direkte basert på {@link AksjonspunktDefinisjon}, returnerer liste. Ingen frist og årsak.
     */
    public static List<AksjonspunktResultat> opprettListeForAksjonspunkt(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return singletonList(new AksjonspunktResultat(aksjonspunktDefinisjon));
    }

    /**
     * Factory-metode som linker {@link AksjonspunktDefinisjon} sammen med ventefrist og årsak.
     */
    public static AksjonspunktResultat opprettForAksjonspunktMedFrist(AksjonspunktDefinisjon aksjonspunktDefinisjon, Venteårsak venteårsak, LocalDateTime ventefrist) {
        return new AksjonspunktResultat(aksjonspunktDefinisjon, venteårsak, ventefrist);
    }

    public AksjonspunktDefinisjon getAksjonspunktDefinisjon() {
        return aksjonspunktDefinisjon;
    }

    public Venteårsak getVenteårsak() {
        return venteårsak;
    }

    public LocalDateTime getFrist() {
        return frist;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + aksjonspunktDefinisjon.getKode() + ":" + aksjonspunktDefinisjon.getNavn()
            + ", frist=" + getFrist() + ", venteårsak=" + getVenteårsak() + ">";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof AksjonspunktResultat))
            return false;

        AksjonspunktResultat that = (AksjonspunktResultat) o;

        return aksjonspunktDefinisjon.getKode().equals(that.aksjonspunktDefinisjon.getKode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(aksjonspunktDefinisjon.getKode());
    }
}

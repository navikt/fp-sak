package no.nav.foreldrepenger.behandlingskontroll;

import static java.util.Collections.singletonList;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;

/**
 * Knytter {@link AksjonspunktDefinisjon} med en callback for å modifisere
 * aksjonpunktet som blir opprettet.
 */
public class AksjonspunktResultat {

    private final AksjonspunktDefinisjon aksjonspunktDefinisjon;
    private final Venteårsak venteårsak;
    private final LocalDateTime frist;
    private final boolean avbruttTilUtført;

    private AksjonspunktResultat(AksjonspunktDefinisjon aksjonspunktDefinisjon,
                                 Venteårsak venteårsak,
                                 LocalDateTime ventefrist,
                                 boolean avbruttTilUtført) {
        this.aksjonspunktDefinisjon = aksjonspunktDefinisjon;
        this.venteårsak = venteårsak;
        this.frist = ventefrist;
        this.avbruttTilUtført = avbruttTilUtført;
    }

    private AksjonspunktResultat(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        this(aksjonspunktDefinisjon, null, null, false);
    }

    /**
     * Factory-metode direkte basert på {@link AksjonspunktDefinisjon}. Ingen frist
     * eller årsak.
     */
    public static AksjonspunktResultat opprettForAksjonspunkt(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return new AksjonspunktResultat(aksjonspunktDefinisjon);
    }

    /**
     * Factory-metode direkte basert på {@link AksjonspunktDefinisjon}, returnerer
     * liste. Ingen frist og årsak.
     */
    public static List<AksjonspunktResultat> opprettListeForAksjonspunkt(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return singletonList(new AksjonspunktResultat(aksjonspunktDefinisjon));
    }

    /**
     * Factory-metode som linker {@link AksjonspunktDefinisjon} sammen med
     * ventefrist og årsak.
     */
    public static AksjonspunktResultat opprettForAksjonspunktMedFrist(AksjonspunktDefinisjon aksjonspunktDefinisjon, Venteårsak venteårsak,
            LocalDateTime ventefrist) {
        return new AksjonspunktResultat(aksjonspunktDefinisjon, venteårsak, ventefrist, false);
    }

    /**
     * Hvis AP ikke eksisterer - Oppretter AP
     * Hvis AP utført - Gjenåpner AP
     * Hvis AP er avbrutt - AP settes utført, begrunnelse kopieres
     */
    public static AksjonspunktResultat opprettAvbruttTilUtførtForAksjonspunkt(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return new AksjonspunktResultat(aksjonspunktDefinisjon, null, null, true);
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

    public boolean erAvbruttTilUtført() {
        return avbruttTilUtført;
    }

    @Override
    public String toString() {
        return "AksjonspunktResultat{" + "aksjonspunktDefinisjon=" + aksjonspunktDefinisjon + ", venteårsak=" + venteårsak + ", frist=" + frist
            + ", avbruttTilUtført=" + avbruttTilUtført + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AksjonspunktResultat)) {
            return false;
        }

        var that = (AksjonspunktResultat) o;

        return aksjonspunktDefinisjon.getKode().equals(that.aksjonspunktDefinisjon.getKode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(aksjonspunktDefinisjon.getKode());
    }
}

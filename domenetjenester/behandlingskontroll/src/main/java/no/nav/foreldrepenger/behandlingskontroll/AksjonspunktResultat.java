package no.nav.foreldrepenger.behandlingskontroll;

import static java.util.Collections.singletonList;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus.OPPRETTET;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;

/**
 * Knytter {@link AksjonspunktDefinisjon} med en callback for å modifisere
 * aksjonpunktet som blir opprettet.
 */
public class AksjonspunktResultat {

    private final AksjonspunktDefinisjon aksjonspunktDefinisjon;
    private final AksjonspunktStatus målStatus;
    private final Venteårsak venteårsak;
    private final LocalDateTime frist;

    private AksjonspunktResultat(AksjonspunktDefinisjon aksjonspunktDefinisjon,
                                 Venteårsak venteårsak,
                                 LocalDateTime ventefrist,
                                 AksjonspunktStatus målStatus) {
        this.aksjonspunktDefinisjon = aksjonspunktDefinisjon;
        this.venteårsak = venteårsak;
        this.frist = ventefrist;
        this.målStatus = målStatus;
    }

    private AksjonspunktResultat(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        this(aksjonspunktDefinisjon, null, null, OPPRETTET);
    }

    /**
     * Factory-metode direkte basert på {@link AksjonspunktDefinisjon}. Ingen frist
     * eller årsak.
     */
    public static AksjonspunktResultat opprettForAksjonspunkt(AksjonspunktDefinisjon aksjonspunktDefinisjon, AksjonspunktStatus aksjonspunktStatus) {
        return new AksjonspunktResultat(aksjonspunktDefinisjon, null, null, aksjonspunktStatus);
    }

    public static AksjonspunktResultat opprettForAksjonspunkt(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return opprettForAksjonspunkt(aksjonspunktDefinisjon, OPPRETTET);
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
        return new AksjonspunktResultat(aksjonspunktDefinisjon, venteårsak, ventefrist, OPPRETTET);
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

    public AksjonspunktStatus getMålStatus() {
        return målStatus;
    }

    @Override
    public String toString() {
        return "AksjonspunktResultat{" + "aksjonspunktDefinisjon=" + aksjonspunktDefinisjon + ", målStatus=" + målStatus + ", venteårsak="
            + venteårsak + ", frist=" + frist + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof AksjonspunktResultat that && aksjonspunktDefinisjon.equals(that.aksjonspunktDefinisjon);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aksjonspunktDefinisjon);
    }
}

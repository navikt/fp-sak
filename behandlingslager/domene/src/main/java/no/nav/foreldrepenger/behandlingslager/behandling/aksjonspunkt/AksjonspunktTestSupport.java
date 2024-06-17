package no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt;

import java.time.LocalDateTime;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;

/**
 * Skal kun brukes av tester som av en eller annen grunn må tukle
 */
public final class AksjonspunktTestSupport {

    private AksjonspunktTestSupport() {
    }

    public static Aksjonspunkt leggTilAksjonspunkt(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return leggTilAksjonspunkt(behandling, aksjonspunktDefinisjon, null);
    }

    public static Aksjonspunkt leggTilAksjonspunkt(Behandling behandling,
                                                   AksjonspunktDefinisjon aksjonspunktDefinisjon,
                                                   BehandlingStegType behandlingStegType) {
        // sjekk at alle parametere er spesifisert
        Objects.requireNonNull(behandling, "behandling");
        Objects.requireNonNull(aksjonspunktDefinisjon, "aksjonspunktDefinisjon");

        // slå opp for å få riktig konfigurasjon.
        var adBuilder = behandlingStegType != null ? new Aksjonspunkt.Builder(aksjonspunktDefinisjon, behandlingStegType) : new Aksjonspunkt.Builder(
            aksjonspunktDefinisjon);

        if (aksjonspunktDefinisjon.getFristPeriod() != null) {
            adBuilder.medFristTid(LocalDateTime.now().plus(aksjonspunktDefinisjon.getFristPeriod()));
        }
        adBuilder.medVenteårsak(Venteårsak.UDEFINERT);

        return adBuilder.buildFor(behandling);

    }

    public static void setToTrinnsBehandlingKreves(Aksjonspunkt aksjonspunkt) {
        var apDef = aksjonspunkt.getAksjonspunktDefinisjon();
        if (!apDef.kanSetteTotrinnBehandling()) {
            return;
        }
        if (!aksjonspunkt.isToTrinnsBehandling()) {
            if (!aksjonspunkt.erÅpentAksjonspunkt()) {
                setReåpnet(aksjonspunkt);
            }
            aksjonspunkt.setToTrinnsBehandling(true);
        }
    }

    public static void fjernToTrinnsBehandlingKreves(Aksjonspunkt aksjonspunkt) {
        aksjonspunkt.setToTrinnsBehandling(false);
    }

    public static boolean setTilUtført(Aksjonspunkt aksjonspunkt, String begrunnelse) {
        return aksjonspunkt.setStatus(AksjonspunktStatus.UTFØRT, begrunnelse);
    }

    public static void setTilAvbrutt(Aksjonspunkt aksjonspunkt) {
        aksjonspunkt.setStatus(AksjonspunktStatus.AVBRUTT, aksjonspunkt.getBegrunnelse());
    }

    public static void setReåpnet(Aksjonspunkt aksjonspunkt) {
        aksjonspunkt.setStatus(AksjonspunktStatus.OPPRETTET, aksjonspunkt.getBegrunnelse());
    }

    public static void setFrist(Aksjonspunkt ap, LocalDateTime fristTid, Venteårsak venteårsak) {
        ap.setFristTid(fristTid);
        ap.setVenteårsak(venteårsak);
    }
}

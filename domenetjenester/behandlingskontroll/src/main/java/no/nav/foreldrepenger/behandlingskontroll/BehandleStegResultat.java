package no.nav.foreldrepenger.behandlingskontroll;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.TransisjonIdentifikator;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;

public class BehandleStegResultat {
    private final List<AksjonspunktResultat> aksjonspunktListe;
    private final TransisjonIdentifikator transisjon;

    private BehandleStegResultat(TransisjonIdentifikator transisjon, List<AksjonspunktResultat> aksjonspunktListe) {
        this.aksjonspunktListe = aksjonspunktListe;
        this.transisjon = transisjon;
    }

    public List<AksjonspunktDefinisjon> getAksjonspunktListe() {
        return aksjonspunktListe.stream().map(AksjonspunktResultat::getAksjonspunktDefinisjon).collect(toList());
    }

    public List<AksjonspunktResultat> getAksjonspunktResultater() {
        return aksjonspunktListe;
    }

    public TransisjonIdentifikator getTransisjon() {
        return transisjon;
    }

    /**
     * Factory-metode basert på liste av {@link AksjonspunktResultat}, støtter
     * callback for å modifisere {@link Aksjonspunkt}
     */
    public static BehandleStegResultat utførtMedAksjonspunktResultater(List<AksjonspunktResultat> aksjonspunktResultater) {
        return new BehandleStegResultat(FellesTransisjoner.UTFØRT, aksjonspunktResultater);
    }

    public static BehandleStegResultat utførtMedAksjonspunktResultat(AksjonspunktResultat aksjonspunktResultat) {
        return utførtMedAksjonspunktResultater(List.of(aksjonspunktResultat));
    }

    /**
     * Factory-metode for liste av {@link AksjonspunktDefinisjon}. Ingen callback
     * for consumer.
     */
    public static BehandleStegResultat utførtMedAksjonspunkter(List<AksjonspunktDefinisjon> aksjonspunktListe) {
        var aksjonspunktResultater = konverterTilAksjonspunktResultat(aksjonspunktListe);
        return new BehandleStegResultat(FellesTransisjoner.UTFØRT, aksjonspunktResultater);
    }

    public static BehandleStegResultat utførtMedAksjonspunkt(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return utførtMedAksjonspunkter(List.of(aksjonspunktDefinisjon));
    }

    public static BehandleStegResultat utførtUtenAksjonspunkter() {
        return new BehandleStegResultat(FellesTransisjoner.UTFØRT, Collections.emptyList());
    }

    public static BehandleStegResultat settPåVent() {
        return new BehandleStegResultat(FellesTransisjoner.SETT_PÅ_VENT, Collections.emptyList());
    }

    public static BehandleStegResultat tilbakeførtMedAksjonspunkter(List<AksjonspunktDefinisjon> aksjonspunktListe) {
        var aksjonspunktResultater = konverterTilAksjonspunktResultat(aksjonspunktListe);
        return new BehandleStegResultat(FellesTransisjoner.TILBAKEFØRT_TIL_AKSJONSPUNKT, aksjonspunktResultater);
    }

    public static BehandleStegResultat tilbakeførtMedlemskap() { // TODO medlemskap - fjern når alle behandlinger med gamle medl-ap er avsluttet
        return new BehandleStegResultat(FellesTransisjoner.TILBAKEFØRT_TIL_MEDLEMSKAP, Collections.emptyList());
    }

    public static BehandleStegResultat fremoverførtMedAksjonspunkter(TransisjonIdentifikator transisjon,
            List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner) {
        var aksjonspunktResultater = konverterTilAksjonspunktResultat(aksjonspunktDefinisjoner);
        return new BehandleStegResultat(transisjon, aksjonspunktResultater);
    }

    public static BehandleStegResultat fremoverførtMedAksjonspunktResultater(TransisjonIdentifikator transisjon,
            List<AksjonspunktResultat> aksjonspunktResultater) {
        return new BehandleStegResultat(transisjon, aksjonspunktResultater);
    }

    public static BehandleStegResultat fremoverført(TransisjonIdentifikator transisjon) {
        return new BehandleStegResultat(transisjon, Collections.emptyList());
    }

    private static List<AksjonspunktResultat> konverterTilAksjonspunktResultat(List<AksjonspunktDefinisjon> aksjonspunktListe) {
        return aksjonspunktListe.stream()
                .map(AksjonspunktResultat::opprettForAksjonspunkt)
                .collect(toList());
    }

    public static BehandleStegResultat startet() {
        return new BehandleStegResultat(FellesTransisjoner.STARTET, Collections.emptyList());
    }

    /** sett nytt aksjonspunkt spesifikt. returner kopi av denne instansen. */
    public BehandleStegResultat medAksjonspunktResultat(AksjonspunktResultat aksResultat) {
        List<AksjonspunktResultat> liste = new ArrayList<>(this.aksjonspunktListe);
        liste.remove(aksResultat);
        liste.add(aksResultat);

        return new BehandleStegResultat(this.transisjon, liste);
    }

    // Må selv lage historikkinnslag i steget + ingen støtte for kø-håndtering (ikke
    // relevant før INSØK.UT).
    public static BehandleStegResultat henlagtBehandling() {
        return new BehandleStegResultat(FellesTransisjoner.HENLAGT, Collections.emptyList());
    }
}

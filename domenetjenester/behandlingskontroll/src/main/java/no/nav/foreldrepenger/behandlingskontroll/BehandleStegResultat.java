package no.nav.foreldrepenger.behandlingskontroll;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingskontroll.transisjoner.StegTransisjon;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.Transisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;

public class BehandleStegResultat {
    private final List<AksjonspunktResultat> aksjonspunktListe;
    private final Transisjon transisjon;

    private BehandleStegResultat(StegTransisjon transisjon, BehandlingStegType målSteg, List<AksjonspunktResultat> aksjonspunktListe) {
        this(new Transisjon(transisjon, målSteg), aksjonspunktListe);
    }

    private BehandleStegResultat(StegTransisjon transisjon, List<AksjonspunktResultat> aksjonspunktListe) {
        this(new Transisjon(transisjon, null), aksjonspunktListe);
    }

    private BehandleStegResultat(Transisjon transisjon, List<AksjonspunktResultat> aksjonspunktListe) {
        this.transisjon = transisjon;
        this.aksjonspunktListe = Optional.ofNullable(aksjonspunktListe).orElseGet(List::of);
        if (transisjon.stegTransisjon().direkteTilGittDestinasjon() && transisjon.målSteg() == null) {
            throw new IllegalArgumentException("Utviklerfeil mangler målsteg");
        }
        if (transisjon.stegTransisjon().kreverAksjonspunkt() && (aksjonspunktListe == null || aksjonspunktListe.isEmpty())) {
            throw new IllegalArgumentException("Utviklerfeil mangler retur-aksjonspunkt");
        }
    }

    public List<AksjonspunktDefinisjon> getAksjonspunktListe() {
        return aksjonspunktListe.stream().map(AksjonspunktResultat::getAksjonspunktDefinisjon).toList();
    }

    public List<AksjonspunktResultat> getAksjonspunktResultater() {
        return aksjonspunktListe;
    }

    public Transisjon getTransisjon() {
        return transisjon;
    }


    /**
     * Factory-metode basert på liste av {@link AksjonspunktResultat}, støtter
     * callback for å modifisere {@link Aksjonspunkt}
     */
    public static BehandleStegResultat utførtMedAksjonspunktResultater(List<AksjonspunktResultat> aksjonspunktResultater) {
        return new BehandleStegResultat(StegTransisjon.UTFØRT, aksjonspunktResultater);
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
        return new BehandleStegResultat(StegTransisjon.UTFØRT, aksjonspunktResultater);
    }

    public static BehandleStegResultat utførtMedAksjonspunkt(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return utførtMedAksjonspunkter(List.of(aksjonspunktDefinisjon));
    }

    public static BehandleStegResultat utførtUtenAksjonspunkter() {
        return new BehandleStegResultat(StegTransisjon.UTFØRT, List.of());
    }

    public static BehandleStegResultat settPåVent() {
        return new BehandleStegResultat(StegTransisjon.SUSPENDERT, List.of());
    }

    public static BehandleStegResultat tilbakeførtMedAksjonspunkter(List<AksjonspunktDefinisjon> aksjonspunktListe) {
        var aksjonspunktResultater = konverterTilAksjonspunktResultat(aksjonspunktListe);
        return new BehandleStegResultat(StegTransisjon.RETURNER, aksjonspunktResultater);
    }

    public static BehandleStegResultat fremoverførtMedAksjonspunkter(BehandlingStegType målSteg, List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner) {
        var aksjonspunktResultater = konverterTilAksjonspunktResultat(aksjonspunktDefinisjoner);
        return new BehandleStegResultat(StegTransisjon.HOPPOVER, målSteg, aksjonspunktResultater);
    }

    public static BehandleStegResultat fremoverførtMedAksjonspunktResultater(BehandlingStegType målSteg, List<AksjonspunktResultat> aksjonspunktResultater) {
        return new BehandleStegResultat(StegTransisjon.HOPPOVER, målSteg, aksjonspunktResultater);
    }

    public static BehandleStegResultat fremoverført(BehandlingStegType målSteg) {
        return new BehandleStegResultat(StegTransisjon.HOPPOVER, målSteg, List.of());
    }

    public static BehandleStegResultat langhoppMedAksjonspunktResultater(BehandlingStegType målSteg, List<AksjonspunktResultat> aksjonspunktResultater) {
        return new BehandleStegResultat(StegTransisjon.FLYOVER, målSteg, aksjonspunktResultater);
    }

    public static BehandleStegResultat langhopp(BehandlingStegType målSteg) {
        return new BehandleStegResultat(StegTransisjon.FLYOVER, målSteg, List.of());
    }

    private static List<AksjonspunktResultat> konverterTilAksjonspunktResultat(List<AksjonspunktDefinisjon> aksjonspunktListe) {
        return aksjonspunktListe.stream()
                .map(AksjonspunktResultat::opprettForAksjonspunkt)
                .collect(toList());
    }

    public static BehandleStegResultat startet() {
        return new BehandleStegResultat(StegTransisjon.STARTET, List.of());
    }

    // Må selv sette behandlingsresultat og lage historikkinnslag i steget. Ved behov også bestille brev.
    public static BehandleStegResultat henlagtBehandling() {
        return new BehandleStegResultat(StegTransisjon.HENLEGG, List.of());
    }
}

package no.nav.foreldrepenger.behandlingskontroll.transisjoner;

public class FellesTransisjoner {

    public static final String FREMHOPP_PREFIX = "fremhopp-til-";
    public static final String SPOLFREM_PREFIX = "revurdering-fremoverhopp-til-";
    private static final String TILBAKEFØR_PREFIX = "tilbakeført-til-";

    public static final TransisjonIdentifikator UTFØRT = TransisjonIdentifikator.forId("utført");
    public static final TransisjonIdentifikator STARTET = TransisjonIdentifikator.forId("startet");
    public static final TransisjonIdentifikator HENLAGT = TransisjonIdentifikator.forId("henlagt");
    public static final TransisjonIdentifikator SETT_PÅ_VENT = TransisjonIdentifikator.forId("sett-på-vent");
    public static final TransisjonIdentifikator TILBAKEFØRT_TIL_AKSJONSPUNKT = TransisjonIdentifikator.forId(TILBAKEFØR_PREFIX + "aksjonspunkt");
    public static final TransisjonIdentifikator TILBAKEFØRT_TIL_MEDLEMSKAP = TransisjonIdentifikator.forId(TILBAKEFØR_PREFIX + "medlemskap");
    public static final TransisjonIdentifikator FREMHOPP_TIL_FATTE_VEDTAK = TransisjonIdentifikator.forId(FREMHOPP_PREFIX + "fatte-vedtak");
    public static final TransisjonIdentifikator FREMHOPP_TIL_FORESLÅ_VEDTAK = TransisjonIdentifikator.forId(FREMHOPP_PREFIX + "foreslå-vedtak");
    public static final TransisjonIdentifikator FREMHOPP_TIL_FORESLÅ_BEHANDLINGSRESULTAT = TransisjonIdentifikator
            .forId(FREMHOPP_PREFIX + "foreslå-behandlingsresultat");
    public static final TransisjonIdentifikator FREMHOPP_TIL_KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT = TransisjonIdentifikator
            .forId(FREMHOPP_PREFIX + "kontroller-søkers-opplysningsplikt");
    public static final TransisjonIdentifikator FREMHOPP_TIL_UTTAKSPLAN = TransisjonIdentifikator.forId(FREMHOPP_PREFIX + "uttaksplan");
    public static final TransisjonIdentifikator FREMHOPP_TIL_BEREGN_YTELSE = TransisjonIdentifikator.forId(FREMHOPP_PREFIX + "beregn-ytelse");
    public static final TransisjonIdentifikator FREMHOPP_TIL_IVERKSETT_VEDTAK = TransisjonIdentifikator.forId(FREMHOPP_PREFIX + "iverksett-vedtak");
    // Proxy-transisjon. Mappes til en av de over avhengig av tilstand på fagsaken.
    // Brukes PT i aksjonspunktoppdaterer + tilsvarende finnes ved
    // inngangsvilkårsteg
    public static final TransisjonIdentifikator FREMHOPP_VED_AVSLAG_VILKÅR = TransisjonIdentifikator.forId(FREMHOPP_PREFIX + "avslag-vilkår");

    private FellesTransisjoner() {
        // hindrer instansiering
    }

    public static boolean erFremhoppTransisjon(TransisjonIdentifikator transisjonIdentifikator) {
        return transisjonIdentifikator.getId().startsWith(FREMHOPP_PREFIX);
    }

    public static boolean erSpolfremTransisjon(TransisjonIdentifikator transisjonIdentifikator) {
        return transisjonIdentifikator.getId().startsWith(SPOLFREM_PREFIX);
    }
}

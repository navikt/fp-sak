package no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt;



import java.util.Map;

// Midlertidig klasse. Legges inn i Venteårsak i senere runde
public final class VenteGruppe {

    // AUTO_MANUELT_SATT_PÅ_VENT og AUTO_VENTER_PÅ_KOMPLETT_SØKNAD delegeres til venteårsak
    private static final Map<AksjonspunktDefinisjon, VenteKategori> GRUPPERING_AUTOPUNKT = Map.ofEntries(
        Map.entry(AksjonspunktDefinisjon.AUTO_VENT_PÅ_FØDSELREGISTRERING, VenteKategori.VENT_RAPPORTERING),
        Map.entry(AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_REVURDERING, VenteKategori.VENT_BRUKER),
        Map.entry(AksjonspunktDefinisjon.VENT_PÅ_SCANNING, VenteKategori.VENT_SAKSBEHANDLING),
        Map.entry(AksjonspunktDefinisjon.VENT_PGA_FOR_TIDLIG_SØKNAD, VenteKategori.VENT_TIDLIG),
        Map.entry(AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING, VenteKategori.VENT_SAKSBEHANDLING),
        Map.entry(AksjonspunktDefinisjon.VENT_PÅ_SØKNAD, VenteKategori.VENT_SØKNAD),
        Map.entry(AksjonspunktDefinisjon.AUTO_VENT_PÅ_INNTEKT_RAPPORTERINGSFRIST, VenteKategori.VENT_RAPPORTERING),
        Map.entry(AksjonspunktDefinisjon.AUTO_VENT_PÅ_SISTE_AAP_ELLER_DP_MELDEKORT, VenteKategori.VENT_RAPPORTERING),
        Map.entry(AksjonspunktDefinisjon.AUTO_VENT_ETTERLYST_INNTEKTSMELDING, VenteKategori.VENT_ARBEIDSGIVER),
        Map.entry(AksjonspunktDefinisjon.AUTO_VENT_PÅ_SYKEMELDING, VenteKategori.VENT_RAPPORTERING),

        Map.entry(AksjonspunktDefinisjon.AUTO_VENT_PÅ_KABAL_KLAGE, VenteKategori.VENT_SAKSBEHANDLING),
        Map.entry(AksjonspunktDefinisjon.AUTO_VENT_PÅ_KABAL_ANKE, VenteKategori.VENT_SAKSBEHANDLING),
        Map.entry(AksjonspunktDefinisjon.AUTO_VENT_ANKE_OVERSENDT_TIL_TRYGDERETTEN, VenteKategori.VENT_TRYGDERETT)
    );

    private static final Map<Venteårsak, VenteKategori> GRUPPERING_VENTEÅRSAK = Map.ofEntries(
        Map.entry(Venteårsak.AVV_DOK, VenteKategori.VENT_BRUKER),
        Map.entry(Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING, VenteKategori.VENT_BRUKER),
        Map.entry(Venteårsak.ANKE_VENTER_PAA_MERKNADER_FRA_BRUKER, VenteKategori.VENT_BRUKER),  // Run-off
        Map.entry(Venteårsak.AVV_RESPONS_REVURDERING, VenteKategori.VENT_BRUKER),   // Run-off
        Map.entry(Venteårsak.VENT_PÅ_SISTE_AAP_ELLER_DP_MELDEKORT, VenteKategori.VENT_RAPPORTERING),
        Map.entry(Venteårsak.VENT_INNTEKT_RAPPORTERINGSFRIST, VenteKategori.VENT_RAPPORTERING),
        Map.entry(Venteårsak.VENT_OPPTJENING_OPPLYSNINGER, VenteKategori.VENT_RAPPORTERING),
        Map.entry(Venteårsak.VENT_MANGLENDE_SYKEMELDING, VenteKategori.VENT_RAPPORTERING),
        Map.entry(Venteårsak.VENT_OPDT_INNTEKTSMELDING, VenteKategori.VENT_ARBEIDSGIVER),
        Map.entry(Venteårsak.FOR_TIDLIG_SOKNAD, VenteKategori.VENT_TIDLIG),
        Map.entry(Venteårsak.VENT_UTLAND_TRYGD, VenteKategori.VENT_UTLAND_TRYGD),
        Map.entry(Venteårsak.VENT_SØKNAD_SENDT_INFORMASJONSBREV, VenteKategori.VENT_SØKNAD)
    );

    public enum VenteKategori {
        VENT_TIDLIG,
        VENT_SØKNAD,
        VENT_BRUKER,
        VENT_ARBEIDSGIVER,
        VENT_UTLAND_TRYGD,
        VENT_TRYGDERETT,
        VENT_SAKSBEHANDLING,
        VENT_RAPPORTERING
    }

    public static VenteKategori getKategoriFor(Aksjonspunkt aksjonspunkt) {
        if (!aksjonspunkt.erOpprettet() || !aksjonspunkt.erAutopunkt()) {
            return null;
        }
        return GRUPPERING_AUTOPUNKT.getOrDefault(aksjonspunkt.getAksjonspunktDefinisjon(), getKategoriFor(aksjonspunkt.getVenteårsak()));
    }

    private static VenteKategori getKategoriFor(Venteårsak venteårsak) {
        if (venteårsak == null) {
            return VenteKategori.VENT_SAKSBEHANDLING;
        }
        return GRUPPERING_VENTEÅRSAK.getOrDefault(venteårsak, VenteKategori.VENT_SAKSBEHANDLING);
    }

}

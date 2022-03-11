package no.nav.foreldrepenger.dokumentbestiller;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum DokumentMalType implements Kodeverdi {

    FRITEKSTBREV("FRITEK", "Fritekstbrev"),
    ENGANGSSTØNAD_INNVILGELSE("INNVES", "Innvilget engangsstønad"),
    ENGANGSSTØNAD_AVSLAG("AVSLES", "Avslag engangsstønad"),
    FORELDREPENGER_INNVILGELSE("INVFOR", "Innvilgelsesbrev Foreldrepenger"),
    FORELDREPENGER_AVSLAG("AVSFOR", "Avslagsbrev Foreldrepenger"),
    FORELDREPENGER_OPPHØR("OPPFOR", "Opphør Foreldrepenger"),
    FORELDREPENGER_ANNULLERT("ANUFOR", "Annullering av Foreldrepenger"),
    FORELDREPENGER_INFO_TIL_ANNEN_FORELDER("INFOAF", "Informasjonsbrev til den andre forelderen"),
    SVANGERSKAPSPENGER_INNVILGELSE("INVSVP", "Innvilgelsesbrev svangerskapspenger"),
    SVANGERSKAPSPENGER_OPPHØR("OPPSVP", "Opphør svangerskapspenger"),
    SVANGERSKAPSPENGER_AVSLAG("AVSSVP", "Avslag svangerskapspenger"),
    INNHENTE_OPPLYSNINGER ("INNOPP", "Innhente opplysninger"),
    VARSEL_OM_REVURDERING("VARREV", "Varsel om revurdering"),
    INFO_OM_HENLEGGELSE("IOHENL", "Behandling henlagt"),
    INNSYN_SVAR( "INNSYN", "Svar på innsynskrav"),
    IKKE_SØKT("IKKESO", "Ikke mottatt søknad"),
    INGEN_ENDRING("INGEND", "Uendret utfall"),
    FORLENGET_SAKSBEHANDLINGSTID("FORSAK", "Forlenget saksbehandlingstid"),
    FORLENGET_SAKSBEHANDLINGSTID_MEDL("FORMED", "Forlenget saksbehandlingstid - medlemskap"),
    FORLENGET_SAKSBEHANDLINGSTID_TIDLIG("FORTID", "Forlenget saksbehandlingstid - Tidlig søknad"),
    KLAGE_AVVIST("KGEAVV", "Vedtak om avvist klage"),
    KLAGE_HJEMSENDT("KGEHJE", "Klage hjemsendt/opphevet"),
    KLAGE_OMGJORT("KGEOMG", "Vedtak om omgjøring av klage"),
    KLAGE_OVERSENDT("KGEOVE", "Klage oversendt til klageinstans"),
    KLAGE_STADFESTET("KGESTA", "Vedtak om stadfestelse"),
    ANKE_OMGJORT("ANKOMG", "Vedtak om omgjøring i ankesak"),
    ANKE_OPPHEVET("ANKOPP", "Ankebrev om beslutning om oppheving"),
    ETTERLYS_INNTEKTSMELDING("ELYSIM", "Etterlys inntektsmelding"),

    // Disse brevene er utgåtte, men beholdes her grunnet historisk bruk i databasen:
    @Deprecated
    FRITEKSTBREV_DOK("FRITKS", "Fritekstbrev"),
    @Deprecated
    ENGANGSSTØNAD_INNVILGELSE_DOK("POSVED", "Positivt vedtaksbrev"),
    @Deprecated
    ENGANGSSTØNAD_AVSLAG_DOK("AVSLAG", "Avslagsbrev"),
    @Deprecated
    FORELDREPENGER_INNVILGELSE_DOK("INNVFP", "Innvilgelsesbrev Foreldrepenger"),
    @Deprecated
    FORELDREPENGER_AVSLAG_DOK("AVSLFP", "Avslagsbrev Foreldrepenger"),
    @Deprecated
    FORELDREPENGER_OPPHØR_DOK("OPPHOR", "Opphør brev"),
    @Deprecated
    FORELDREPENGER_INFOBREV_TIL_ANNEN_FORELDER_DOK("INAFOR", "Informasjonsbrev til den andre forelderen"),
    @Deprecated
    SVANGERSKAPSPENGER_INNVILGELSE_FRITEKST("INNSVP", "Innvilgelsesbrev svangerskapspenger"),
    @Deprecated
    INNHENTE_OPPLYSNINGER_DOK("INNHEN", "Innhent dokumentasjon"),
    @Deprecated
    VARSEL_OM_REVURDERING_DOK("REVURD", "Varsel om revurdering"),
    @Deprecated
    INFO_OM_HENLEGGELSE_DOK("HENLEG", "Behandling henlagt"),
    @Deprecated
    INNSYN_SVAR_DOK("INSSKR", "Svar på innsynskrav"),
    @Deprecated
    IKKE_SØKT_DOK("INNTID", "Ikke mottatt søknad"),
    @Deprecated
    INGEN_ENDRING_DOK("UENDRE", "Uendret utfall"),
    @Deprecated
    FORLENGET_SAKSBEHANDLINGSTID_DOK("FORLEN", "Forlenget saksbehandlingstid"),
    @Deprecated
    FORLENGET_SAKSBEHANDLINGSTID_MEDL_DOK("FORLME", "Forlenget saksbehandlingstid - medlemskap"),
    @Deprecated
    FORLENGET_SAKSBEHANDLINGSTID_TIDLIG_DOK("FORLTS", "Forlenget saksbehandlingstid - Tidlig søknad"),
    @Deprecated
    KLAGE_AVVIST_DOK("KLAGAV", "Vedtak om avvist klage"),
    @Deprecated
    KLAGE_AVVIST_FRITEKST("KAVVIS", "Vedtak om avvist klage"),
    @Deprecated
    KLAGE_HJEMSENDT_DOK("KLAGNY", "Vedtak opphevet, sendt til ny behandling"),
    @Deprecated
    KLAGE_HJEMSENDT_FRITEKST("KHJEMS", "Klage hjemsendt/opphevet"),
    @Deprecated
    KLAGE_OMGJORT_DOK("VEDMED", "Vedtak om medhold"),
    @Deprecated
    KLAGE_OMGJORT_FRITEKST("KOMGJO", "Vedtak om omgjøring av klage"),
    @Deprecated
    KLAGE_OVERSENDT_DOK("KLAGOV", "Overføring til NAV Klageinstans"),
    @Deprecated
    KLAGE_OVERSENDT_FRITEKST("KOVKLA", "Klage oversendt til klageinstans"),
    @Deprecated
    KLAGE_STADFESTET_DOK("KLAGVE", "Vedtak om stadfestelse"),
    @Deprecated
    KLAGE_STADFESTET_FRITEKST("KSTADF", "Vedtak om stadfestelse"),
    @Deprecated
    ANKE_OMGJORT_FRITEKST("VEDOGA", "Vedtak om omgjøring i ankesak"),
    @Deprecated
    ANKE_OPPHEVET_FRITEKST("ANKEBO", "Ankebrev om beslutning om oppheving"),
    @Deprecated
    ETTERLYS_INNTEKTSMELDING_FRITEKST("INNLYS", "Etterlys inntektsmelding"),
    ;

    private static final Map<String, DokumentMalType> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @JsonIgnore
    private String navn;

    @JsonValue
    private String kode;

    DokumentMalType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getKodeverk() {
        return "DOKUMENT_MAL_TYPE";
    }

    @Override
    public String getNavn() {
        return navn;
    }

    public static DokumentMalType fraKode(String kode) {
        var ad = Optional.ofNullable(KODER.get(kode));
        if (ad.isEmpty()) {
            throw new IllegalArgumentException("Ukjent DokumentMalType: " + kode);
        }
        return ad.get();
    }

    public static boolean erVedtaksBrev(DokumentMalType brev) {
        return Set.of(ENGANGSSTØNAD_INNVILGELSE, ENGANGSSTØNAD_AVSLAG,
            FORELDREPENGER_INNVILGELSE, FORELDREPENGER_AVSLAG, FORELDREPENGER_OPPHØR,
            SVANGERSKAPSPENGER_INNVILGELSE, SVANGERSKAPSPENGER_AVSLAG, SVANGERSKAPSPENGER_OPPHØR).contains(brev);
    }

    public static boolean erKlageVedtaksBrev(DokumentMalType brev) {
        return Set.of(KLAGE_STADFESTET, KLAGE_AVVIST, KLAGE_OMGJORT).contains(brev);
    }

}

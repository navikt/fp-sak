package no.nav.foreldrepenger.dokumentbestiller;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum DokumentMalType implements Kodeverdi {

    FRITEKSTBREV("FRITEK"),
    ENGANGSSTØNAD_INNVILGELSE("INNVES"),
    ENGANGSSTØNAD_AVSLAG("AVSLES"),
    FORELDREPENGER_INNVILGELSE("INVFOR"),
    FORELDREPENGER_AVSLAG("AVSFOR"),
    FORELDREPENGER_OPPHØR("OPPFOR"),
    FORELDREPENGER_ANNULLERT("ANUFOR"),
    FORELDREPENGER_INFO_TIL_ANNEN_FORELDER("INFOAF"),
    SVANGERSKAPSPENGER_INNVILGELSE("INVSVP"),
    SVANGERSKAPSPENGER_OPPHØR("OPPSVP"),
    SVANGERSKAPSPENGER_AVSLAG("AVSSVP"),
    INNHENTE_OPPLYSNINGER("INNOPP"),
    VARSEL_OM_REVURDERING("VARREV"),
    INFO_OM_HENLEGGELSE("IOHENL"),
    INNSYN_SVAR("INNSYN"),
    IKKE_SØKT("IKKESO"),
    INGEN_ENDRING("INGEND"),
    FORLENGET_SAKSBEHANDLINGSTID("FORSAK"),
    FORLENGET_SAKSBEHANDLINGSTID_MEDL("FORMED"),
    FORLENGET_SAKSBEHANDLINGSTID_TIDLIG("FORTID"),
    KLAGE_AVVIST("KGEAVV"),
    KLAGE_HJEMSENDT("KGEHJE"),
    KLAGE_OMGJORT("KGEOMG"),
    KLAGE_OVERSENDT("KGEOVE"),
    KLAGE_STADFESTET("KGESTA"),
    ANKE_OMGJORT("ANKOMG"),
    ANKE_OPPHEVET("ANKOPP"),
    ETTERLYS_INNTEKTSMELDING("ELYSIM")
    ;

    public static final Set<DokumentMalType> VEDTAKSBREV = Set.of(ENGANGSSTØNAD_INNVILGELSE, ENGANGSSTØNAD_AVSLAG, FORELDREPENGER_INNVILGELSE,
        FORELDREPENGER_AVSLAG, FORELDREPENGER_OPPHØR, SVANGERSKAPSPENGER_INNVILGELSE, SVANGERSKAPSPENGER_AVSLAG, SVANGERSKAPSPENGER_OPPHØR);

    public static final Set<DokumentMalType> KLAGE_VEDTAKSBREV = Set.of(KLAGE_STADFESTET, KLAGE_AVVIST, KLAGE_HJEMSENDT, KLAGE_OMGJORT);

    private static final Map<String, DokumentMalType> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @JsonValue
    private String kode;

    DokumentMalType(String kode) {
        this.kode = kode;
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
        return utledDokumentTittel(this);
    }

    public static DokumentMalType fraKode(String kode) {
        var ad = Optional.ofNullable(KODER.get(kode));
        if (ad.isEmpty()) {
            throw new IllegalArgumentException("Ukjent DokumentMalType: " + kode);
        }
        return ad.get();
    }

    public static boolean erVedtaksBrev(DokumentMalType brev) {
        return VEDTAKSBREV.contains(brev);
    }

    public static boolean erKlageVedtaksBrev(DokumentMalType brev) {
        return KLAGE_VEDTAKSBREV.contains(brev);
    }

    public static String utledDokumentTittel(String malKode) {
        Objects.requireNonNull(malKode);
        return utledDokumentTittel(fraKode(malKode));
    }

    private static String utledDokumentTittel(DokumentMalType mal) {
        return switch (mal) {
            case FRITEKSTBREV -> "Fritekstbrev";
            case ENGANGSSTØNAD_INNVILGELSE -> "Innvilget engangsstønad";
            case ENGANGSSTØNAD_AVSLAG -> "Avslag engangsstønad";
            case FORELDREPENGER_INNVILGELSE -> "Innvilgelsesbrev foreldrepenger";
            case FORELDREPENGER_AVSLAG -> "Avslagsbrev foreldrepenger";
            case FORELDREPENGER_OPPHØR -> "Opphør foreldrepenger";
            case FORELDREPENGER_ANNULLERT -> "Annullering av foreldrepenger";
            case FORELDREPENGER_INFO_TIL_ANNEN_FORELDER -> "Informasjonsbrev til den andre forelderen";
            case SVANGERSKAPSPENGER_INNVILGELSE -> "Innvilgelsesbrev svangerskapspenger";
            case SVANGERSKAPSPENGER_AVSLAG -> "Avslag svangerskapspenger";
            case SVANGERSKAPSPENGER_OPPHØR -> "Opphør svangerskapspenger";
            case INNHENTE_OPPLYSNINGER -> "Innhente opplysninger";
            case VARSEL_OM_REVURDERING -> "Varsel om revurdering";
            case INFO_OM_HENLEGGELSE -> "Behandling henlagt";
            case INNSYN_SVAR -> "Svar på innsynskrav";
            case IKKE_SØKT -> "Ikke mottatt søknad";
            case INGEN_ENDRING -> "Uendret utfall";
            case FORLENGET_SAKSBEHANDLINGSTID -> "Forlenget saksbehandlingstid";
            case FORLENGET_SAKSBEHANDLINGSTID_MEDL -> "Forlenget saksbehandlingstid - medlemskap";
            case FORLENGET_SAKSBEHANDLINGSTID_TIDLIG -> "Forlenget saksbehandlingstid - tidlig søknad";
            case KLAGE_AVVIST -> "Vedtak om avvist klage";
            case KLAGE_HJEMSENDT -> "Klage hjemsendt/opphevet";
            case KLAGE_OMGJORT -> "Vedtak om omgjøring av klage";
            case KLAGE_OVERSENDT -> "Klage oversendt til klageinstans";
            case KLAGE_STADFESTET -> "Vedtak om stadfestelse";
            case ANKE_OMGJORT -> "Vedtak om omgjøring i ankesak";
            case ANKE_OPPHEVET -> "Ankebrev om beslutning om oppheving";
            case ETTERLYS_INNTEKTSMELDING -> "Etterlys inntektsmelding";
        };
    }
}

package no.nav.foreldrepenger.dokumentbestiller;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public enum DokumentMalType implements Kodeverdi {

    FRITEKSTBREV("FRITEK"),
    ENGANGSSTØNAD_INNVILGELSE("INNVES"),
    ENGANGSSTØNAD_AVSLAG("AVSLES"),
    FORELDREPENGER_INNVILGELSE("INVFOR"),
    FORELDREPENGER_AVSLAG("AVSFOR"),
    FORELDREPENGER_OPPHØR("OPPFOR"),
    FORELDREPENGER_ANNULLERT("ANUFOR"),
    SVANGERSKAPSPENGER_INNVILGELSE("INVSVP"),
    SVANGERSKAPSPENGER_OPPHØR("OPPSVP"),
    SVANGERSKAPSPENGER_AVSLAG("AVSSVP"),
    FORELDREPENGER_INFO_TIL_ANNEN_FORELDER("INFOAF"),
    INNHENTE_OPPLYSNINGER("INNOPP"),
    VARSEL_OM_REVURDERING("VARREV"),
    INFO_OM_HENLEGGELSE("IOHENL"),
    INNSYN_SVAR("INNSYN"),
    IKKE_SØKT("IKKESO"),
    INGEN_ENDRING("INGEND"),
    FORLENGET_SAKSBEHANDLINGSTID("FORSAK"),
    FORLENGET_SAKSBEHANDLINGSTID_MEDL("FORMED"),
    FORLENGET_SAKSBEHANDLINGSTID_MEDL_FORUTGÅENDE("FORMEF"),
    FORLENGET_SAKSBEHANDLINGSTID_TIDLIG("FORTID"),
    KLAGE_AVVIST("KGEAVV"),
    KLAGE_OMGJORT("KGEOMG"),
    KLAGE_OVERSENDT("KGEOVE"),
    ETTERLYS_INNTEKTSMELDING("ELYSIM"),
    ENDRING_UTBETALING("ENDUTB"), // Denne brukes kun for å utlede tittel når overstyrer vedtaksbrev pga fordeling av ytelsen
    FORELDREPENGER_FEIL_PRAKSIS_UTSETTELSE_INFOBREV("INFOPU"),
    FORELDREPENGER_FEIL_PRAKSIS_UTSETTELSE_FORLENGET_SAKSBEHANDLINGSTID("FORPUS"),

    // Må gjeninnføre for å flytte anker
    @Deprecated KLAGE_AVVIST_DOK("KLAGAV"),
    @Deprecated KLAGE_AVVIST_FRITEKST("KAVVIS"),
    @Deprecated KLAGE_HJEMSENDT_DOK("KLAGNY"),
    @Deprecated KLAGE_HJEMSENDT_FRITEKST("KHJEMS"),
    @Deprecated KLAGE_OMGJORT_DOK("VEDMED"),
    @Deprecated KLAGE_OMGJORT_FRITEKST("KOMGJO"),
    @Deprecated KLAGE_OVERSENDT_DOK("KLAGOV"),
    @Deprecated KLAGE_OVERSENDT_FRITEKST("KOVKLA"),
    @Deprecated KLAGE_STADFESTET_DOK("KLAGVE"),
    @Deprecated KLAGE_STADFESTET_FRITEKST("KSTADF"),
    @Deprecated ANKE_OMGJORT_FRITEKST("VEDOGA"),
    @Deprecated ANKE_OPPHEVET_FRITEKST("ANKEBO"),
    @Deprecated ANKE_OMGJORT("ANKOMG"),
    @Deprecated ANKE_OPPHEVET("ANKOPP"),
    @Deprecated KLAGE_STADFESTET("KGESTA"),
    @Deprecated KLAGE_HJEMSENDT("KGEHJE");

    public static final Set<DokumentMalType> VEDTAKSBREV = Set.of(ENGANGSSTØNAD_INNVILGELSE, ENGANGSSTØNAD_AVSLAG, FORELDREPENGER_INNVILGELSE,
        FORELDREPENGER_AVSLAG, FORELDREPENGER_OPPHØR, FORELDREPENGER_ANNULLERT, SVANGERSKAPSPENGER_INNVILGELSE, SVANGERSKAPSPENGER_AVSLAG,
        SVANGERSKAPSPENGER_OPPHØR);

    public static final Set<DokumentMalType> KLAGE_VEDTAKSBREV = Set.of(KLAGE_STADFESTET, KLAGE_AVVIST, KLAGE_HJEMSENDT, KLAGE_OMGJORT,
        KLAGE_AVVIST_DOK, KLAGE_AVVIST_FRITEKST, KLAGE_HJEMSENDT_DOK, KLAGE_HJEMSENDT_FRITEKST, KLAGE_OMGJORT_DOK, KLAGE_OMGJORT_FRITEKST,
        KLAGE_STADFESTET_DOK, KLAGE_STADFESTET_FRITEKST);

    public static final Set<DokumentMalType> MANUELLE_BREV = Set.of(INNHENTE_OPPLYSNINGER, VARSEL_OM_REVURDERING, FORLENGET_SAKSBEHANDLINGSTID_MEDL,
        FORLENGET_SAKSBEHANDLINGSTID_MEDL_FORUTGÅENDE, FORLENGET_SAKSBEHANDLINGSTID, ETTERLYS_INNTEKTSMELDING);

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

    public static Set<DokumentMalType> forlengetSaksbehandlingstidMedlemskap() {
        return Set.of(FORLENGET_SAKSBEHANDLINGSTID_MEDL, FORLENGET_SAKSBEHANDLINGSTID_MEDL_FORUTGÅENDE);
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

    public static boolean erOversendelsesBrev(DokumentMalType brev) {
        return Set.of(DokumentMalType.KLAGE_OVERSENDT, KLAGE_OVERSENDT_DOK, KLAGE_OVERSENDT_FRITEKST).contains(brev);
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
            case FORLENGET_SAKSBEHANDLINGSTID_MEDL_FORUTGÅENDE -> "Forlenget saksbehandlingstid - forutgående medlemskap";
            case FORLENGET_SAKSBEHANDLINGSTID_TIDLIG -> "Forlenget saksbehandlingstid - tidlig søknad";
            case KLAGE_AVVIST, KLAGE_AVVIST_DOK, KLAGE_AVVIST_FRITEKST -> "Vedtak om avvist klage";
            case KLAGE_HJEMSENDT -> "Klage hjemsendt/opphevet";
            case KLAGE_OMGJORT -> "Vedtak om omgjøring av klage";
            case KLAGE_OVERSENDT -> "Klage oversendt til klageinstans";
            case ETTERLYS_INNTEKTSMELDING -> "Etterlys inntektsmelding";
            case KLAGE_HJEMSENDT_DOK -> "Vedtak opphevet, sendt til ny behandling";
            case KLAGE_HJEMSENDT_FRITEKST -> "Klage hjemsendt/opphevet";
            case KLAGE_OMGJORT_DOK -> "Vedtak om medhold";
            case KLAGE_OMGJORT_FRITEKST -> "Vedtak om omgjøring av klage";
            case KLAGE_OVERSENDT_DOK -> "Overføring til Nav klageinstans";
            case KLAGE_OVERSENDT_FRITEKST -> "Klage oversendt til klageinstans";
            case KLAGE_STADFESTET, KLAGE_STADFESTET_DOK, KLAGE_STADFESTET_FRITEKST -> "Vedtak om stadfestelse";
            case ANKE_OMGJORT_FRITEKST -> "Vedtak om omgjøring i ankesak";
            case ANKE_OPPHEVET_FRITEKST -> "Ankebrev om beslutning om oppheving";
            case ANKE_OMGJORT -> "Vedtak om omgjøring i ankesak";
            case ANKE_OPPHEVET -> "Ankebrev om beslutning om oppheving";
            case ENDRING_UTBETALING -> "Endring i fordeling av ytelsen";
            case FORELDREPENGER_FEIL_PRAKSIS_UTSETTELSE_INFOBREV -> "Melding om ny vurdering av tidligere avslag";
            case FORELDREPENGER_FEIL_PRAKSIS_UTSETTELSE_FORLENGET_SAKSBEHANDLINGSTID -> "Forlenget saksbehandlingstid - Fedrekvotesaken";
        };
    }
}

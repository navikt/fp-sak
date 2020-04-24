package no.nav.foreldrepenger.dokumentbestiller;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum DokumentMalType implements Kodeverdi {

    POSITIVT_VEDTAK_DOK("POSVED", "Positivt vedtaksbrev"),
    INNHENT_DOK("INNHEN", "Innhent dokumentasjon"),
    HENLEGG_BEHANDLING_DOK("HENLEG", "Behandling henlagt"),
    AVSLAGSVEDTAK_DOK("AVSLAG", "Avslagsbrev"),
    UENDRETUTFALL_DOK("UENDRE", "Uendret utfall"),
    FORLENGET_DOK("FORLEN", "Forlenget saksbehandlingstid"),
    FORLENGET_MEDL_DOK("FORLME", "Forlenget saksbehandlingstid - medlemskap"),
    FORLENGET_TIDLIG_SOK("FORLTS", "Forlenget saksbehandlingstid - Tidlig søknad"),
    FORLENGET_OPPTJENING("FOROPT", "Forlenget saksbehandlingstid - Venter Opptjening"),
    REVURDERING_DOK("REVURD", "Varsel om revurdering"),
    KLAGE_AVVIST_DOK("KLAGAV", "Vedtak om avvist klage"),
    KLAGE_YTELSESVEDTAK_STADFESTET_DOK("KLAGVE", "Vedtak om stadfestelse"),
    KLAGE_YTELSESVEDTAK_OPPHEVET_DOK("KLAGNY", "Vedtak opphevet, sendt til ny behandling"),
    KLAGE_OVERSENDT_KLAGEINSTANS_DOK("KLAGOV", "Overføring til NAV Klageinstans"),
    INNSYNSKRAV_SVAR("INSSKR", "Svar på innsynskrav"),
    INNVILGELSE_FORELDREPENGER_DOK("INNVFP", "Innvilgelsesbrev Foreldrepenger"),
    OPPHØR_DOK("OPPHOR", "Opphør brev"),
    INNTEKTSMELDING_FOR_TIDLIG_DOK("INNTID", "Ikke mottatt søknad"),
    AVSLAG_FORELDREPENGER_DOK("AVSLFP", "Avslagsbrev Foreldrepenger"),
    FRITEKST_DOK("FRITKS", "Fritekstbrev"),
    VEDTAK_MEDHOLD("VEDMED", "Vedtak om medhold"),
    ETTERLYS_INNTEKTSMELDING_DOK("INNLYS", "Etterlys inntektsmelding"),
    INFO_TIL_ANNEN_FORELDER_DOK("INAFOR", "Informasjonsbrev til den andre forelderen"),
    INNVILGELSE_SVANGERSKAPSPENGER_DOK("INNSVP", "Innvilgelsesbrev svangerskapspenger"),
    ANKEBREV_BESLUTNING_OM_OPPHEVING("ANKEBO", "Ankebrev om beslutning om oppheving"),
    VEDTAK_OMGJORING_ANKE_DOK("VEDOGA", "Vedtak om omgjøring i ankesak"),
    KLAGE_STADFESTET("KSTADF", "Vedtak om stadfestelse"),
    KLAGE_AVVIST("KAVVIS", "Vedtak om avvist klage"),
    KLAGE_OMGJØRING("KOMGJO", "Vedtak om omgjøring av klage"), // medhold
    KLAGE_OVERSENDT_KLAGEINSTANS("KOVKLA", "Klage oversendt til klageinstans"),
    KLAGE_HJEMSENDT("KHJEMS", "Klage hjemsendt/opphevet"),

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

    private String kode;

    private DokumentMalType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

    @JsonProperty
    @Override
    public String getKodeverk() {
        return "DOKUMENT_MAL_TYPE";
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @JsonCreator
    public static DokumentMalType fraKode(@JsonProperty("kode") String kode) {
        var ad = Optional.ofNullable(KODER.get(kode));
        if (ad.isEmpty()) {
            throw new IllegalArgumentException("Ukjent DokumentMalType: " + kode);
        }
        return ad.get();
    }

}

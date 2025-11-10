package no.nav.foreldrepenger.dokumentarkiv;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public enum NAVSkjema {

    SKJEMA_SVANGERSKAPSPENGER("SSVPA", "NAV 14-04.10", "Søknad om svangerskapspenger for arbeidstakere"),
    SKJEMA_SVANGERSKAPSPENGER_SN("SSVPS", "NAV 14-04.10", "Søknad om svangerskapspenger til selvstendig næringsdrivende og frilanser"),
    SKJEMA_FORELDREPENGER_ADOPSJON("SFPA", "NAV 14-05.06", "Søknad om foreldrepenger ved adopsjon"),
    SKJEMA_ENGANGSSTØNAD_FØDSEL("SESF", "NAV 14-05.07", "Søknad om engangsstønad ved fødsel"),
    SKJEMA_ENGANGSSTØNAD_ADOPSJON("SESA", "NAV 14-05.08", "Søknad om engangsstønad ved adopsjon"),
    SKJEMA_FORELDREPENGER_FØDSEL("SFPF", "NAV 14-05.09", "Søknad om foreldrepenger ved fødsel"),
    SKJEMA_FLEKSIBELT_UTTAK("SFUT", "NAV 14-16.05", "Søknad om endring eller nytt uttak av foreldrepenger"),
    SKJEMA_INNTEKTSOPPLYSNING_SELVSTENDIG("SIOS", "NAV 14-35.01", "Inntektsopplysninger for selvstendig næringsdrivende og frilansere som skal ha foreldrepenger eller svangerskapspenger"),
    SKJEMA_INNTEKTSOPPLYSNINGER("SIOP", "NAV 08-30.01", "Inntektsopplysninger for arbeidstaker som skal ha sykepenger foreldrepenger svangerskapspenger pleie-/opplæringspenger og omsorgspenger"),
    SKJEMA_KLAGE_DOKUMENT("SKLAGE", "NAV 90-00.08", "Klage/anke"),
    SKJEMA_FORELDREPENGER_ENDRING("SEND", "NAV 14-05.10", "Søknad om endring av uttak av foreldrepenger eller overføring av kvote"),

    SKJEMAE_KLAGE("SEKLAG", "NAVe 90-00.08", "Ettersendelse klage/anke"),

    SKJEMA_ANNEN_POST("SANP", "NAV 00-03.00", "Annen post"),

    // Altinn-skjemakode
    SKJEMA_INNTEKTSMELDING("INNTEKTSMELDING", "4936", "Inntektsmelding"),

    // Arbeidstilsynet-skjemakode
    SKJEMA_TILRETTELEGGING_B("SSVPT", "AT-474B", "Tilrettelegging/omplassering ved graviditet"),
    SKJEMA_TILRETTELEGGING_N("SSVPN", "AT-474N", "LIKT SOM SKJEMA_TILRETTELEGGING_B"),

    // ANNET
    FORSIDE_SVP_GAMMEL("SSVPG", "AT-474B", "Tilrettelegging/omplassering pga graviditet / Søknad om svangerskapspenger til arbeidstaker"),

    UDEFINERT(Kodeverdi.STANDARDKODE_UDEFINERT, null, "Ukjent type dokument");

    private static final Map<String, NAVSkjema> KODER = new LinkedHashMap<>();
    private static final Map<String, NAVSkjema> OFFISIELLE_KODER = new LinkedHashMap<>();
    private static final Map<String, NAVSkjema> TERMNAVN_KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
            if (v.offisiellKode != null) {
                OFFISIELLE_KODER.putIfAbsent(v.offisiellKode, v);
            }
            if (v.termnavn != null) {
                TERMNAVN_KODER.putIfAbsent(v.termnavn, v);
            }
        }
    }

    private String kode;

    private String offisiellKode;

    private String termnavn;

    NAVSkjema(String kode, String offisiellKode, String termnavn) {
        this.kode = kode;
        this.offisiellKode = offisiellKode;
        this.termnavn = termnavn;
    }

    public static NAVSkjema fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Tema: " + kode);
        }
        return ad;
    }

    public static NAVSkjema fraOffisiellKode(String kode) {
        return Optional.ofNullable(kode).map(OFFISIELLE_KODER::get).orElse(UDEFINERT);
    }

    public static NAVSkjema fraTermNavn(String navn) {
        return Optional.ofNullable(navn).map(TERMNAVN_KODER::get).orElse(UDEFINERT);
    }

    public String getKode() {
        return kode;
    }

    public String getOffisiellKode() {
        return offisiellKode;
    }

    public String getTermNavn() {
        return termnavn;
    }


}

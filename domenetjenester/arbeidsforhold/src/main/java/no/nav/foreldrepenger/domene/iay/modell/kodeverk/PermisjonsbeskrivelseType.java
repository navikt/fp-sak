package no.nav.foreldrepenger.domene.iay.modell.kodeverk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.MedOffisiellKode;

public enum PermisjonsbeskrivelseType implements Kodeverdi, MedOffisiellKode {

    UDEFINERT("-", "Ikke definert", null),
    PERMISJON("PERMISJON", "Permisjon", "permisjon"),
    UTDANNINGSPERMISJON("UTDANNINGSPERMISJON", "Utdanningspermisjon", "utdanningspermisjon"),
    VELFERDSPERMISJON("VELFERDSPERMISJON", "Velferdspermisjon", "velferdspermisjon"),
    PERMISJON_MED_FORELDREPENGER("PERMISJON_MED_FORELDREPENGER", "Permisjon med foreldrepenger", "permisjonMedForeldrepenger"),
    PERMITTERING("PERMITTERING", "Permittering", "permittering"),
    PERMISJON_VED_MILITÆRTJENESTE("PERMISJON_VED_MILITÆRTJENESTE", "Permisjon ved militærtjeneste", "permisjonVedMilitaertjeneste"),
    ;

    private static final Set<PermisjonsbeskrivelseType> PERMISJON_IKKE_RELEVANT_FOR_AVKLAR_ARBEIDSFORHOLD = Set.of(
            PermisjonsbeskrivelseType.UTDANNINGSPERMISJON,
            PermisjonsbeskrivelseType.PERMISJON_MED_FORELDREPENGER);

    private static final Map<String, PermisjonsbeskrivelseType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "PERMISJONSBESKRIVELSE_TYPE";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;

    @JsonValue
    private String kode;
    private String offisiellKode;

    PermisjonsbeskrivelseType(String kode) {
        this.kode = kode;
    }

    PermisjonsbeskrivelseType(String kode, String navn, String offisiellKode) {
        this.kode = kode;
        this.navn = navn;
        this.offisiellKode = offisiellKode;
    }

    public static PermisjonsbeskrivelseType fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent PermisjonsbeskrivelseType: " + kode);
        }
        return ad;
    }

    public static Map<String, PermisjonsbeskrivelseType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return offisiellKode;
    }

    public boolean erRelevantForAvklarArbeidsforhold() {
        return !PERMISJON_IKKE_RELEVANT_FOR_AVKLAR_ARBEIDSFORHOLD.contains(this);
    }

}

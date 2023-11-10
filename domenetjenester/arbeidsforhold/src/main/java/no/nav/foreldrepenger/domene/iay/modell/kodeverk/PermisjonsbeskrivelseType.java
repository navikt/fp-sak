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
    UTDANNINGSPERMISJON("UTDANNINGSPERMISJON", "Utdanningspermisjon", "utdanningspermisjon"), // Utgår 31/12-2022
    UTDANNINGSPERMISJON_IKKE_LOVFESTET("UTDANNINGSPERMISJON_IKKE_LOVFESTET", "Utdanningspermisjon (Ikke lovfestet)", "utdanningspermisjonIkkeLovfestet"),
    UTDANNINGSPERMISJON_LOVFESTET("UTDANNINGSPERMISJON_LOVFESTET", "Utdanningspermisjon (Lovfestet)", "utdanningspermisjonLovfestet"),
    VELFERDSPERMISJON("VELFERDSPERMISJON", "Velferdspermisjon", "velferdspermisjon"), // Utgår 31/12-2022
    ANNEN_PERMISJON_IKKE_LOVFESTET("ANNEN_PERMISJON_IKKE_LOVFESTET", "Andre ikke-lovfestede permisjoner", "andreIkkeLovfestedePermisjoner"),
    ANNEN_PERMISJON_LOVFESTET("ANNEN_PERMISJON_LOVFESTET", "Andre lovfestede permisjoner", "andreLovfestedePermisjoner"),PERMISJON_MED_FORELDREPENGER("PERMISJON_MED_FORELDREPENGER", "Permisjon med foreldrepenger", "permisjonMedForeldrepenger"),
    PERMITTERING("PERMITTERING", "Permittering", "permittering"),
    PERMISJON_VED_MILITÆRTJENESTE("PERMISJON_VED_MILITÆRTJENESTE", "Permisjon ved militærtjeneste", "permisjonVedMilitaertjeneste"),
    ;

    public static final Set<PermisjonsbeskrivelseType> VELFERDSPERMISJONER = Set.of(
        PermisjonsbeskrivelseType.VELFERDSPERMISJON,
        PermisjonsbeskrivelseType.ANNEN_PERMISJON_IKKE_LOVFESTET,
        PermisjonsbeskrivelseType.ANNEN_PERMISJON_LOVFESTET
    );

    private static final Map<String, PermisjonsbeskrivelseType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "PERMISJONSBESKRIVELSE_TYPE";

    private static final Set<PermisjonsbeskrivelseType> PERMISJON_IKKE_RELEVANT_FOR_ARBEIDSFORHOLD_ELLER_BEREGNING = Set.of(
        PermisjonsbeskrivelseType.UTDANNINGSPERMISJON,
        PermisjonsbeskrivelseType.UTDANNINGSPERMISJON_IKKE_LOVFESTET,
        PermisjonsbeskrivelseType.UTDANNINGSPERMISJON_LOVFESTET,
        PermisjonsbeskrivelseType.PERMISJON_MED_FORELDREPENGER);



    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private final String navn;

    @JsonValue
    private final String kode;
    private final String offisiellKode;

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

    public boolean erRelevantForBeregningEllerArbeidsforhold() {
        return !PERMISJON_IKKE_RELEVANT_FOR_ARBEIDSFORHOLD_ELLER_BEREGNING.contains(this);
    }

}

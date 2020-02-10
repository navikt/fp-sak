package no.nav.foreldrepenger.domene.iay.modell.kodeverk;

/**
 * <p>
 * Definerer statuser for bekreftet permisjoner
 * </p>
 */
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum BekreftetPermisjonStatus implements Kodeverdi {

    UDEFINERT("-", "UDEFINERT"),
    BRUK_PERMISJON("BRUK_PERMISJON", "Bruk permisjonen til arbeidsforholdet"),
    IKKE_BRUK_PERMISJON("IKKE_BRUK_PERMISJON", "Ikke bruk permisjonen til arbeidsforholdet"),
    UGYLDIGE_PERIODER("UGYLDIGE_PERIODER", "Arbeidsforholdet inneholder permisjoner med ugyldige perioder"),
    ;

    private static final Map<String, BekreftetPermisjonStatus> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "BEKREFTET_PERMISJON_STATUS";

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

    private BekreftetPermisjonStatus(String kode) {
        this.kode = kode;
    }

    private BekreftetPermisjonStatus(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static BekreftetPermisjonStatus fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent BekreftetPermisjonStatus: " + kode);
        }
        return ad;
    }

    public static Map<String, BekreftetPermisjonStatus> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @JsonProperty
    @Override
    public String getKodeverk() {
        return KODEVERK;
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
}

package no.nav.foreldrepenger.behandlingslager.behandling.søknad;

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

/** @deprecated: Er et tynt subsett av RelasjonRolleType, men kodene er i bruk i SøknadXML så kan ikke bare slettes. */
@Deprecated
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum ForeldreType implements Kodeverdi {

    MOR("MOR", "Mor"),
    FAR("FAR", "Far"),
    MEDMOR("MEDMOR", "Medmor"),
    ANDRE("ANDRE", "Andre"),
    UDEFINERT("-", "Ikke definert"),
    ;

    private static final Map<String, ForeldreType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "FORELDRE_TYPE";

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

    private ForeldreType(String kode) {
        this.kode = kode;
    }

    private ForeldreType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static ForeldreType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent ForeldreType: " + kode);
        }
        return ad;
    }

    public static Map<String, ForeldreType> kodeMap() {
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
    
    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }

}

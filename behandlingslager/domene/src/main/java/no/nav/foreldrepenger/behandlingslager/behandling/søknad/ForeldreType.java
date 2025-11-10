package no.nav.foreldrepenger.behandlingslager.behandling.søknad;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

/** @deprecated: Er et tynt subsett av RelasjonRolleType, men kodene er i bruk i SøknadXML så kan ikke bare slettes. */
@Deprecated
public enum ForeldreType implements Kodeverdi {

    MOR("MOR", "Mor"),
    FAR("FAR", "Far"),
    MEDMOR("MEDMOR", "Medmor"),
    ANDRE("ANDRE", "Andre"),
    UDEFINERT(STANDARDKODE_UDEFINERT, "Ikke definert"),
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

    private final String navn;

    @JsonValue
    private final String kode;

    ForeldreType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Map<String, ForeldreType> kodeMap() {
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

}

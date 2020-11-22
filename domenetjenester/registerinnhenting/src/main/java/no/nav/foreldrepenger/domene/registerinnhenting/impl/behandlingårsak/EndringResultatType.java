package no.nav.foreldrepenger.domene.registerinnhenting.impl.behandlingårsak;

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
public enum EndringResultatType implements Kodeverdi {

    REGISTEROPPLYSNING("REGISTEROPPLYSNING", "Nye registeropplysninger"),
    OPPLYSNING_OM_YTELSER("YTELSEOPPLYSNING", "Nye opplysninger om ytelse"),
    OPPLYSNING_OM_DØD("DØDSOPPLYSNING", "Nye opplysninger om dødsfall"),
    INNTEKTSMELDING("INNTEKTSMELDING", "Nye inntektsmeldinger"),

    UDEFINERT("-", "Ikke definert"),

    ;

    public static final String KODEVERK = "ENDRING_RESULTAT"; //$NON-NLS-1$

    private static final Map<String, EndringResultatType> KODER = new LinkedHashMap<>();

    @JsonIgnore
    private String navn;

    private String kode;

    private EndringResultatType(String kode) {
        this.kode = kode;
    }

    private EndringResultatType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static EndringResultatType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent BehandlingÅrsakType: " + kode);
        }
        return ad;
    }

    public static Map<String, EndringResultatType> kodeMap() {
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

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

}

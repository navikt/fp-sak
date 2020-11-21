package no.nav.foreldrepenger.behandlingslager.behandling.anke;

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
public enum AnkeAvvistÅrsak implements Kodeverdi {

    ANKE_FOR_SENT("ANKE_FOR_SENT", "Bruker har anket for sent"),
    ANKE_UGYLDIG("ANKE_UGYLDIG", "Anke er ugyldig"),
    ANKE_IKKE_PAANKET_VEDTAK("ANKE_IKKE_PAANKET_VEDTAK", "Ikke påanket et vedtak"),
    ANKE_IKKE_PART("ANKE_IKKE_PART", "Anke er ikke part"),
    ANKE_IKKE_KONKRET("ANKE_IKKE_KONKRET", "Anke er ikke konkret"),
    ANKE_IKKE_SIGNERT("ANKE_IKKE_SIGNERT", "Anke er ikke signert"),
    UDEFINERT("-", "Udefinert"),
    ;

    private static final Map<String, AnkeAvvistÅrsak> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "ANKE_AVVIST_AARSAK";

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

    private AnkeAvvistÅrsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static AnkeAvvistÅrsak fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent AnkeAvvistÅrsak: " + kode);
        }
        return ad;
    }

    public static Map<String, AnkeAvvistÅrsak> kodeMap() {
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

}

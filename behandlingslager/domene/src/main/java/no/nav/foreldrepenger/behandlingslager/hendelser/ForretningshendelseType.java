package no.nav.foreldrepenger.behandlingslager.hendelser;

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
public enum ForretningshendelseType implements Kodeverdi {
    
    INGEN_HENDELSE("INGEN_HENDELSE", "Ingen hendelse"),
    FØDSEL("FØDSEL", "Fødsel"),
    DØD("DØD", "Død"),
    DØDFØDSEL("DØDFØDSEL", "Dødfødsel"),
    YTELSE_INNVILGET("YTELSE_INNVILGET", "Ytelse innvilget"),
    YTELSE_ENDRET("YTELSE_ENDRET", "Ytelse endret"),
    YTELSE_OPPHØRT("YTELSE_OPPHØRT", "Ytelse opphørt"),
    YTELSE_ANNULERT("YTELSE_ANNULERT", "Ytelse anullert"),

    UDEFINERT("-", "Ikke definert"),
    ;

    private static final Map<String, ForretningshendelseType> KODER = new LinkedHashMap<>();
    
    public static final String KODEVERK = "FORRETNINGSHENDELSE_TYPE";

    @JsonIgnore
    private String navn;

    private String kode;

    private ForretningshendelseType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static boolean erYtelseHendelseType(ForretningshendelseType forretningshendelseType) {
        return YTELSE_INNVILGET.equals(forretningshendelseType)
            || YTELSE_ENDRET.equals(forretningshendelseType)
            || YTELSE_OPPHØRT.equals(forretningshendelseType)
            || YTELSE_ANNULERT.equals(forretningshendelseType);
    }
    
    @JsonCreator
    public static ForretningshendelseType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent ForretningshendelseType: " + kode);
        }
        return ad;
    }

    public static Map<String, ForretningshendelseType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }
    
    @Override
    public String getNavn() {
        return navn;
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
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
    
    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }
    

}

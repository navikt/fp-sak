package no.nav.foreldrepenger.dokumentarkiv;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum ArkivFilType implements Kodeverdi {

    PDF ("PDF"),
    PDFA ("PDFA"),
    XML ("XML"),
    AFP ("AFP"),
    AXML ("AXML"),
    DLF ("DLF"),
    DOC ("DOC"),
    DOCX ("DOCX"),
    JPEG ("JPEG"),
    RTF ("RTF"),
    TIFF ("TIFF"),
    XLS ("XLS"),
    XLSX ("XLSX"),

    
    UDEFINERT ("-"),

    ;
    private static final String KODEVERK = "ARKIV_FILTYPE";
    
    private static final Map<String, ArkivFilType> KODER = new LinkedHashMap<>();
    
    private String kode;


    private ArkivFilType(String kode) {
        this.kode = kode;
    }
    
    @JsonCreator
    public static ArkivFilType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent ArkivFilType: " + kode);
        }
        return ad;
    }

    public static Map<String, ArkivFilType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }
    
    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }
    
    @Override
    public String getNavn() {
        return getKode();
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
   
    public static ArkivFilType finnForKodeverkEiersKode(String offisiellDokumentType) {
        return List.of(values()).stream().filter(k -> Objects.equals(k.getOffisiellKode(), offisiellDokumentType)).findFirst().orElse(UDEFINERT);
    }
}

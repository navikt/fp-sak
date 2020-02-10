package no.nav.foreldrepenger.behandlingslager.behandling;

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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum VariantFormat implements Kodeverdi{

    SLADDET("SLADD", "Sladdet format", "SLADDET"),
    SKANNING_META("SKANM", "Skanning metadata", "SKANNING_META"),
    PRODUKSJON("PROD", "Produksjonsformat", "PRODUKSJON"),
    PRODUKSJON_DLF("PRDLF", "Produksjonsformat DLF", "PRODUKSJON_DLF"),
    ORIGINAL("ORIG", "Originalformat", "ORIGINAL"),
    FULLVERSJON("FULL", "Versjon med infotekster", "FULLVERSJON"),
    BREVBESTILLING("BREVB", "Brevbestilling data", "BREVBESTILLING"),
    ARKIV("ARKIV", "Arkivformat", "ARKIV"),
    UDEFINERT("-", "Ikke definert", null),
    
    ;
    private static final String KODEVERK = "VARIANT_FORMAT";
    
    private static final Map<String, VariantFormat> KODER = new LinkedHashMap<>();
    
    
    @JsonIgnore
    private String navn;
    
    @JsonIgnore
    private String offisiellKode;

    private String kode;

    private VariantFormat(String kode, String navn, String offisiellKode) {
        this.kode = kode;
        this.navn = navn;
        this.offisiellKode = offisiellKode;
    }
    

    @JsonCreator
    public static VariantFormat fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent VariantFormat: " + kode);
        }
        return ad;
    }

    public static Map<String, VariantFormat> kodeMap() {
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
        return offisiellKode;
    }
    
    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }
    
    public static VariantFormat finnForKodeverkEiersKode(String offisiellDokumentType) {
        return List.of(values()).stream().filter(k -> Objects.equals(k.offisiellKode, offisiellDokumentType)).findFirst().orElse(UDEFINERT);
    }
}

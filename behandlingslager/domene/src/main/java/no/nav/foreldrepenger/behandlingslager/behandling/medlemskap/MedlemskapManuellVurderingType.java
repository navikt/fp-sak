package no.nav.foreldrepenger.behandlingslager.behandling.medlemskap;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

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
public enum MedlemskapManuellVurderingType implements Kodeverdi {

    UDEFINERT("-", "Ikke definert", false),
    MEDLEM("MEDLEM", "Periode med medlemskap", true),
    UNNTAK("UNNTAK", "Periode med unntak fra medlemskap", true),
    IKKE_RELEVANT("IKKE_RELEVANT", "Ikke relevant periode", true),
    SAKSBEHANDLER_SETTER_OPPHØR_AV_MEDL_PGA_ENDRINGER_I_TPS("OPPHOR_PGA_ENDRING_I_TPS", "Opphør av medlemskap på grunn av endringer i tps", false),
    
    ;
    
    private static final Map<String, MedlemskapManuellVurderingType> KODER = new LinkedHashMap<>();
    
    public static final String KODEVERK = "MEDLEMSKAP_MANUELL_VURD";

    @JsonIgnore
    private String navn;

    @JsonIgnore
    private boolean visForGui;

    private String kode;

    private MedlemskapManuellVurderingType(String kode) {
        this.kode = kode;
    }

    private MedlemskapManuellVurderingType(String kode, String navn, boolean visGui) {
        this.kode = kode;
        this.navn = navn;
        this.visForGui = visGui;
    }
    
    @JsonCreator
    public static MedlemskapManuellVurderingType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent MedlemskapManuellVurderingType: " + kode);
        }
        return ad;
    }
    
    public static Map<String, MedlemskapManuellVurderingType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }

    public boolean visesPåKlient() {
        return visForGui;
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
    
    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<MedlemskapManuellVurderingType, String> {
        @Override
        public String convertToDatabaseColumn(MedlemskapManuellVurderingType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public MedlemskapManuellVurderingType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}

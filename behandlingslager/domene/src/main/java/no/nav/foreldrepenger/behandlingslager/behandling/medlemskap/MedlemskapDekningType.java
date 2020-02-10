package no.nav.foreldrepenger.behandlingslager.behandling.medlemskap;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
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
public enum MedlemskapDekningType implements Kodeverdi {

    FTL_2_6("FTL_2_6", "Folketrygdloven § 2-6"),
    FTL_2_7_a("FTL_2_7_a", "Folketrygdloven § 2-7, 3.ledd bokstav a"),
    FTL_2_7_b("FTL_2_7_b", "Folketrygdloven § 2-7, 3. ledd bokstav b"),
    FTL_2_9_1_a("FTL_2_9_1_a", "Folketrygdloven § 2-9, 1.ledd bokstav a"),
    FTL_2_9_1_b("FTL_2_9_1_b", "Folketrygdloven § 2-9, 1.ledd bokstav b"),
    FTL_2_9_1_c("FTL_2_9_1_c", "Folketrygdloven § 2-9, 1.ledd bokstav c"),
    FTL_2_9_2_a("FTL_2_9_2_a", "Folketrygdloven § 2-9, annet ledd, jfr. 1.ledd bokstav a"),
    FTL_2_9_2_c("FTL_2_9_2_c", "Folketrygdloven § 2-9, annet ledd, jf. 1. ledd bokstav c"),
    FULL("FULL", "Full"),
    IHT_AVTALE("IHT_AVTALE", "I henhold til avtale"),
    OPPHOR("OPPHOR", "Opphør"),
    UNNTATT("UNNTATT", "Unntatt"),
    
    UDEFINERT("-", "Ikke definert"),
    ;

    
    public static final List<MedlemskapDekningType> DEKNINGSTYPER = unmodifiableList(asList(
        FTL_2_6,
        FTL_2_7_a,
        FTL_2_7_b,
        FTL_2_9_1_a,
        FTL_2_9_1_b,
        FTL_2_9_1_c,
        FTL_2_9_2_a,
        FTL_2_9_2_c,
        FULL,
        UNNTATT));

    public static final List<MedlemskapDekningType> DEKNINGSTYPE_ER_FRIVILLIG_MEDLEM = unmodifiableList(asList(
        FTL_2_7_a,
        FTL_2_7_b,
        FTL_2_9_1_a,
        FTL_2_9_1_c,
        FTL_2_9_2_a,
        FTL_2_9_2_c,
        FULL
            ));

    public static final List<MedlemskapDekningType> DEKNINGSTYPE_ER_MEDLEM_UNNTATT = unmodifiableList(singletonList(
        UNNTATT));

    public static final List<MedlemskapDekningType> DEKNINGSTYPE_ER_IKKE_MEDLEM = unmodifiableList(asList(
        FTL_2_6,
        FTL_2_9_1_b
            ));

    public static final List<MedlemskapDekningType> DEKNINGSTYPE_ER_UAVKLART = unmodifiableList(asList(
        IHT_AVTALE,
        OPPHOR));

    
    private static final Map<String, MedlemskapDekningType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "MEDLEMSKAP_DEKNING";

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

    private MedlemskapDekningType(String kode) {
        this.kode = kode;
    }

    private MedlemskapDekningType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static MedlemskapDekningType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent MedlemskapDekningType: " + kode);
        }
        return ad;
    }

    public static Map<String, MedlemskapDekningType> kodeMap() {
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

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<MedlemskapDekningType, String> {
        @Override
        public String convertToDatabaseColumn(MedlemskapDekningType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public MedlemskapDekningType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}

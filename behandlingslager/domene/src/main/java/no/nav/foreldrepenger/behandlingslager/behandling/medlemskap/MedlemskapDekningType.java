package no.nav.foreldrepenger.behandlingslager.behandling.medlemskap;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum MedlemskapDekningType implements Kodeverdi {

    FTL_2_6("FTL_2_6", "Folketrygdloven § 2-6"),
    FTL_2_7_A("FTL_2_7_a", "Folketrygdloven § 2-7 tredje ledd bokstav a"),
    FTL_2_7_B("FTL_2_7_b", "Folketrygdloven § 2-7 tredje ledd bokstav b"),
    FTL_2_9_1_A("FTL_2_9_1_a", "Folketrygdloven § 2-9 første ledd bokstav a"),
    FTL_2_9_1_B("FTL_2_9_1_b", "Folketrygdloven § 2-9 første ledd bokstav b"),
    FTL_2_9_1_C("FTL_2_9_1_c", "Folketrygdloven § 2-9 første ledd bokstav c"),
    FTL_2_9_2_A("FTL_2_9_2_a", "Folketrygdloven § 2-9 andre ledd, jf. § 2-9 første ledd bokstav a"),
    FTL_2_9_2_C("FTL_2_9_2_c", "Folketrygdloven § 2-9 andre ledd, jf. § 2-9 første ledd bokstav c"),
    FULL("FULL", "Full"),
    IHT_AVTALE("IHT_AVTALE", "I henhold til avtale"),
    OPPHOR("OPPHOR", "Opphør"),
    UNNTATT("UNNTATT", "Unntatt"),

    UDEFINERT(STANDARDKODE_UDEFINERT, "Ikke definert"),
    ;

    public static final List<MedlemskapDekningType> DEKNINGSTYPER = unmodifiableList(asList(
        FTL_2_6, FTL_2_7_A, FTL_2_7_B, FTL_2_9_1_A, FTL_2_9_1_B, FTL_2_9_1_C, FTL_2_9_2_A, FTL_2_9_2_C,
        FULL,
        UNNTATT));

    public static final List<MedlemskapDekningType> DEKNINGSTYPE_ER_FRIVILLIG_MEDLEM = List.of(FTL_2_7_A, FTL_2_7_B, FTL_2_9_1_A, FTL_2_9_1_C,
        FTL_2_9_2_A, FTL_2_9_2_C,
        FULL);

    public static final List<MedlemskapDekningType> DEKNINGSTYPE_ER_MEDLEM_UNNTATT = List.of(UNNTATT);

    public static final List<MedlemskapDekningType> DEKNINGSTYPE_ER_IKKE_MEDLEM = List.of(FTL_2_6, FTL_2_9_1_B);

    public static final List<MedlemskapDekningType> DEKNINGSTYPE_ER_UAVKLART = List.of(IHT_AVTALE, OPPHOR);

    private static final Map<String, MedlemskapDekningType> KODER = new LinkedHashMap<>();

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

    MedlemskapDekningType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKode() {
        return kode;
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

        private static MedlemskapDekningType fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent MedlemskapDekningType: " + kode);
            }
            return ad;
        }
    }
}

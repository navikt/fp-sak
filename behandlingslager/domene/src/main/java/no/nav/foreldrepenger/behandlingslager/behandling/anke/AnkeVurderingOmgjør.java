package no.nav.foreldrepenger.behandlingslager.behandling.anke;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;


public enum AnkeVurderingOmgjør implements Kodeverdi {

    ANKE_TIL_GUNST("ANKE_TIL_GUNST", "Gunst omgjør i anke"),
    ANKE_DELVIS_OMGJOERING_TIL_GUNST("ANKE_DELVIS_OMGJOERING_TIL_GUNST", "Delvis omgjøring, til gunst i anke"),
    ANKE_TIL_UGUNST("ANKE_TIL_UGUNST", "Ugunst omgjør i anke"),
    UDEFINERT(STANDARDKODE_UDEFINERT, "Udefinert"),
    ;
    private static final Map<String, AnkeVurderingOmgjør> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "ANKE_VURDERING_OMGJOER";

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

    AnkeVurderingOmgjør(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Map<String, AnkeVurderingOmgjør> kodeMap() {
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

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<AnkeVurderingOmgjør, String> {
        @Override
        public String convertToDatabaseColumn(AnkeVurderingOmgjør attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public AnkeVurderingOmgjør convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static AnkeVurderingOmgjør fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent AnkeVurderingOmgjør: " + kode);
            }
            return ad;
        }
    }
}

package no.nav.foreldrepenger.behandlingslager.behandling.klage;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum KlageVurderingOmgjør implements Kodeverdi {

    GUNST_MEDHOLD_I_KLAGE("GUNST_MEDHOLD_I_KLAGE", "Gunst medhold i klage"),
    DELVIS_MEDHOLD_I_KLAGE("DELVIS_MEDHOLD_I_KLAGE", "Delvis medhold i klage"),
    UGUNST_MEDHOLD_I_KLAGE("UGUNST_MEDHOLD_I_KLAGE", "Ugunst medhold i klage"),
    UDEFINERT("-", "Udefinert"),
    ;

    private static final Map<String, KlageVurderingOmgjør> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "KLAGE_VURDERING_OMGJOER";

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

    KlageVurderingOmgjør(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Map<String, KlageVurderingOmgjør> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<KlageVurderingOmgjør, String> {
        @Override
        public String convertToDatabaseColumn(KlageVurderingOmgjør attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public KlageVurderingOmgjør convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static KlageVurderingOmgjør fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent KlageVurderingOmgjør: " + kode);
            }
            return ad;
        }
    }
}

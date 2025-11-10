package no.nav.foreldrepenger.behandlingslager.behandling.klage;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum KlageVurdering implements Kodeverdi {

    OPPHEVE_YTELSESVEDTAK("OPPHEVE_YTELSESVEDTAK", "Ytelsesvedtaket oppheves"),
    STADFESTE_YTELSESVEDTAK("STADFESTE_YTELSESVEDTAK", "Ytelsesvedtaket stadfestes"),
    MEDHOLD_I_KLAGE("MEDHOLD_I_KLAGE", "Medhold"),
    AVVIS_KLAGE("AVVIS_KLAGE", "Klagen avvises"),
    HJEMSENDE_UTEN_Å_OPPHEVE("HJEMSENDE_UTEN_Å_OPPHEVE", "Hjemsende uten å oppheve"),
    UDEFINERT(STANDARDKODE_UDEFINERT, "Udefinert"),
    ;

    private static final Map<String, KlageVurdering> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "KLAGEVURDERING";

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

    KlageVurdering(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Map<String, KlageVurdering> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<KlageVurdering, String> {
        @Override
        public String convertToDatabaseColumn(KlageVurdering attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public KlageVurdering convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static KlageVurdering fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent KlageVurdering: " + kode);
            }
            return ad;
        }

    }

}

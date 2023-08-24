package no.nav.foreldrepenger.behandlingslager.behandling.opptjening;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum OpptjeningAktivitetKlassifisering implements Kodeverdi {

    BEKREFTET_GODKJENT("BEKREFTET_GODKJENT", "Bekreftet godkjent"),
    BEKREFTET_AVVIST("BEKREFTET_AVVIST", "Bekreftet avvist"),
    ANTATT_GODKJENT("ANTATT_GODKJENT", "Antatt godkjent"),
    MELLOMLIGGENDE_PERIODE("MELLOMLIGGENDE_PERIODE", "Mellomliggende periode"),
    UDEFINERT("-", "UDEFINERT"),
    ;

    private static final Map<String, OpptjeningAktivitetKlassifisering> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "OPPTJENING_AKTIVITET_KLASSIFISERING";

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

    OpptjeningAktivitetKlassifisering(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Map<String, OpptjeningAktivitetKlassifisering> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<OpptjeningAktivitetKlassifisering, String> {
        @Override
        public String convertToDatabaseColumn(OpptjeningAktivitetKlassifisering attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public OpptjeningAktivitetKlassifisering convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static OpptjeningAktivitetKlassifisering fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent OpptjeningAktivitetKlassifisering: " + kode);
            }
            return ad;
        }
    }
}

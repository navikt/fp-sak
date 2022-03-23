package no.nav.foreldrepenger.behandlingslager.risikoklassifisering;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum Kontrollresultat implements Kodeverdi {

    HØY("HOY", "Kontrollresultatet er HØY"),
    IKKE_HØY("IKKE_HOY", "Kontrollresultatet er IKKE_HØY"),
    IKKE_KLASSIFISERT("IKKE_KLASSIFISERT", "Behandlingen er ikke blitt klassifisert"),
    UDEFINERT("-", "Udefinert"),
    ;

    private static final Map<String, Kontrollresultat> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "KONTROLLRESULTAT";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;
    @JsonValue
    private String kode;

    Kontrollresultat(String kode) {
        this.kode = kode;
    }

    Kontrollresultat(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Map<String, Kontrollresultat> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<Kontrollresultat, String> {
        @Override
        public String convertToDatabaseColumn(Kontrollresultat attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public Kontrollresultat convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static Kontrollresultat fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent Kontrollresultat: " + kode);
            }
            return ad;
        }
    }
}

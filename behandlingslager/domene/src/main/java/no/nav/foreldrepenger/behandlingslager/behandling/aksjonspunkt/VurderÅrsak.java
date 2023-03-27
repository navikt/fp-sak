package no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum VurderÅrsak implements Kodeverdi {

    FEIL_FAKTA("FEIL_FAKTA", "Feil fakta"),
    FEIL_LOV("FEIL_LOV", "Feil lov-/regelanvendelse"),
    FEIL_REGEL("FEIL_REGEL", "Feil regelforståelse"), // UTGÅTT, beholdes pga historikk  
    SKJØNN("SKJØNN", "Skjønn"),
    UTREDNING("UTREDNING", "Utredning"),
    ANNET("ANNET", "Annet"),
    UDEFINERT("-", "Ikke definert"),

    ;

    private static final Map<String, VurderÅrsak> KODER = new LinkedHashMap<>();
    public static final String KODEVERK = "VURDER_AARSAK";

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

    VurderÅrsak() {
    }

    VurderÅrsak(String kode) {
        this.kode = kode;
    }

    VurderÅrsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static VurderÅrsak fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent VurderÅrsak: " + kode);
        }
        return ad;
    }

    public static Map<String, VurderÅrsak> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<VurderÅrsak, String> {
        @Override
        public String convertToDatabaseColumn(VurderÅrsak attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public VurderÅrsak convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }

}

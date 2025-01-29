package no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum VurderÅrsak implements Kodeverdi {

    FEIL_FAKTA("FEIL_FAKTA", "Fakta"),
    FEIL_LOV("FEIL_LOV", "Regel-/lovanvendelse"),
    SKJØNN("SKJØNN", "Skjønn"),
    UTREDNING("UTREDNING", "Utredning"),
    SAKSFLYT("SAKSFLYT", "Saksflyt"),
    BEGRUNNELSE("BEGRUNNELSE", "Begrunnelse"),
    UDEFINERT("-", "Ikke definert"),

    @Deprecated
    ANNET("ANNET", "Annet"), // UTGÅTT, beholdes pga historikk
    @Deprecated
    FEIL_REGEL("FEIL_REGEL", "Feil regelforståelse"), // UTGÅTT, beholdes pga historikk
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

    private final String navn;

    @JsonValue
    private final String kode;

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

package no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public enum AksjonspunktType implements Kodeverdi {

    AUTOPUNKT("AUTO", "Autopunkt"),
    MANUELL("MANU", "Manuell"),
    OVERSTYRING("OVST", "Overstyring"),
    SAKSBEHANDLEROVERSTYRING("SAOV", "Saksbehandleroverstyring"),
    UDEFINERT("-", "Ikke definert"),
    ;

    private static final Map<String, AksjonspunktType> KODER = new LinkedHashMap<>();
    public static final String KODEVERK = "AKSJONSPUNKT_TYPE";

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

    AksjonspunktType(String kode, String navn) {
        this.kode = kode;
        /* merkelig nok har navn blit brukt som offisiell kode bla. mot Pip/ABAC. */
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

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    public static Map<String, AksjonspunktType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    public boolean erAutopunkt() {
        return Objects.equals(this, AUTOPUNKT);
    }

    public boolean erOverstyringpunkt() {
        return Objects.equals(this, OVERSTYRING) || Objects.equals(this, SAKSBEHANDLEROVERSTYRING);
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<AksjonspunktType, String> {
        @Override
        public String convertToDatabaseColumn(AksjonspunktType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public AksjonspunktType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static AksjonspunktType fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent AksjonspunktType: " + kode);
            }
            return ad;
        }
    }

}

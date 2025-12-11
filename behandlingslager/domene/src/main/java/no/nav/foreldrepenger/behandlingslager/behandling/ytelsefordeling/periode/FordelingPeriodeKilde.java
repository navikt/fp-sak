package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum FordelingPeriodeKilde implements Kodeverdi {

    SØKNAD("SØKNAD", "Søknad"),
    TIDLIGERE_VEDTAK("TIDLIGERE_VEDTAK", "Vedtak"),
    ANDRE_NAV_VEDTAK("ANDRE_NAV_VEDTAK", "Vedtak andre ytelser"),
    SAKSBEHANDLER("SAKSBEHANDLER", "Saksbehandler")
    ;
    private static final Map<String, FordelingPeriodeKilde> KODER = new LinkedHashMap<>();

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

    FordelingPeriodeKilde(String kode, String navn) {
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
    public static class KodeverdiConverter implements AttributeConverter<FordelingPeriodeKilde, String> {
        @Override
        public String convertToDatabaseColumn(FordelingPeriodeKilde attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public FordelingPeriodeKilde convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static FordelingPeriodeKilde fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent FordelingPeriodeKilde: " + kode);
            }
            return ad;
        }
    }
}

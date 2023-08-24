package no.nav.foreldrepenger.behandlingslager.uttak.fp;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;


public enum UttakUtsettelseType implements Kodeverdi {

    ARBEID("ARBEID", "Arbeid"),
    FERIE("FERIE", "Lovbestemt ferie"),
    SYKDOM_SKADE("SYKDOM_SKADE", "Avhengig av hjelp grunnet sykdom"),
    SØKER_INNLAGT("SØKER_INNLAGT", "Søker er innlagt i helseinstitusjon"),
    BARN_INNLAGT("BARN_INNLAGT", "Barn er innlagt i helseinstitusjon"),
    HV_OVELSE("HV_OVELSE", "Heimevernet"),
    NAV_TILTAK("NAV_TILTAK", "Tiltak i regi av NAV"),
    FRI("FRI", "Fri utsettelse fom høst 2021"),
    UDEFINERT("-", "Ikke satt eller valgt kode"),
    ;
    private static final Map<String, UttakUtsettelseType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "UTTAK_UTSETTELSE_TYPE";

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

    UttakUtsettelseType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Map<String, UttakUtsettelseType> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<UttakUtsettelseType, String> {
        @Override
        public String convertToDatabaseColumn(UttakUtsettelseType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public UttakUtsettelseType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static UttakUtsettelseType fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent UttakUtsettelseType: " + kode);
            }
            return ad;
        }
    }
}

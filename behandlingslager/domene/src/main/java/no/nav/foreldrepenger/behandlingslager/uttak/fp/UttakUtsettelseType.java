package no.nav.foreldrepenger.behandlingslager.uttak.fp;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;


@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum UttakUtsettelseType implements Kodeverdi {

    ARBEID("ARBEID", "Arbeid"),
    FERIE("FERIE", "Lovbestemt ferie"),
    SYKDOM_SKADE("SYKDOM_SKADE", "Avhengig av hjelp grunnet sykdom"),
    SØKER_INNLAGT("SØKER_INNLAGT", "Søker er innlagt i helseinstitusjon"),
    BARN_INNLAGT("BARN_INNLAGT", "Barn er innlagt i helseinstitusjon"),
    HV_OVELSE("HV_OVELSE", "Heimevernet"),
    NAV_TILTAK("NAV_TILTAK", "Tiltak i regi av NAV"),
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

    @JsonIgnore
    private String navn;

    private String kode;

    UttakUtsettelseType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static UttakUtsettelseType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent UttakUtsettelseType: " + kode);
        }
        return ad;
    }
    public static Map<String, UttakUtsettelseType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @JsonProperty
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return this.getKode();
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
    }
}

package no.nav.foreldrepenger.behandlingslager.uttak;

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
public enum UttakArbeidType implements Kodeverdi {

    ORDINÆRT_ARBEID("ORDINÆRT_ARBEID", "Ordinært arbeid"),
    SELVSTENDIG_NÆRINGSDRIVENDE("SELVSTENDIG_NÆRINGSDRIVENDE", "Selvstendig næringsdrivende"),
    FRILANS("FRILANS", "Frilans"),
    ANNET("ANNET", "Annet"),
    ;
    private static final Map<String, UttakArbeidType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "UTTAK_ARBEID_TYPE";

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

    UttakArbeidType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static UttakArbeidType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent UttakArbeidType: " + kode);
        }
        return ad;
    }
    public static Map<String, UttakArbeidType> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<UttakArbeidType, String> {
        @Override
        public String convertToDatabaseColumn(UttakArbeidType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public UttakArbeidType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }

    public boolean erArbeidstakerEllerFrilans() {
        return ORDINÆRT_ARBEID.equals(this) || FRILANS.equals(this);
    }
}

package no.nav.foreldrepenger.behandlingslager.behandling.historikk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum HistorikkOpplysningType implements Kodeverdi {

    UDEFINIERT("-", "Ikke definert"),
    ANTALL_BARN("ANTALL_BARN", "Antall barn"),
    TPS_ANTALL_BARN("TPS_ANTALL_BARN", "Antall barn"),
    FODSELSDATO("FODSELSDATO", "Fødselsdato"),
    UTTAK_PERIODE_FOM("UTTAK_PERIODE_FOM", "Fradato uttaksperiode"),
    UTTAK_PERIODE_TOM("UTTAK_PERIODE_TOM", "Tildato uttaksperiode"),
    ;

    private static final Map<String, HistorikkOpplysningType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "HISTORIKK_OPPLYSNING_TYPE";

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

    private HistorikkOpplysningType(String kode) {
        this.kode = kode;
    }

    private HistorikkOpplysningType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static HistorikkOpplysningType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent HistorikkOpplysningType: " + kode);
        }
        return ad;
    }

    public static Map<String, HistorikkOpplysningType> kodeMap() {
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
        return getKode();
    }
    
    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<HistorikkOpplysningType, String> {
        @Override
        public String convertToDatabaseColumn(HistorikkOpplysningType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public HistorikkOpplysningType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}

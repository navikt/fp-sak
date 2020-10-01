package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak;

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

import no.nav.foreldrepenger.behandlingslager.kodeverk.TempAvledeKode;


@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum OppholdÅrsak implements Årsak {

    UDEFINERT("-", "Ikke satt eller valgt kode"),
    INGEN("INGEN", "Ingen årsak."),
    MØDREKVOTE_ANNEN_FORELDER("UTTAK_MØDREKVOTE_ANNEN_FORELDER", "Annen forelder har uttak av Mødrekvote"),
    FEDREKVOTE_ANNEN_FORELDER("UTTAK_FEDREKVOTE_ANNEN_FORELDER", "Annen forelder har uttak av Fedrekvote"),
    KVOTE_FELLESPERIODE_ANNEN_FORELDER("UTTAK_FELLESP_ANNEN_FORELDER", "Annen forelder har uttak av Fellesperiode"),
    KVOTE_FORELDREPENGER_ANNEN_FORELDER("UTTAK_FORELDREPENGER_ANNEN_FORELDER", "Annen forelder har uttak av Foreldrepenger"),
    ;
    private static final Map<String, OppholdÅrsak> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "OPPHOLD_AARSAK_TYPE";

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

    OppholdÅrsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static OppholdÅrsak fraKode(@JsonProperty(value = "kode") Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(OppholdÅrsak.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent OppholdÅrsak: " + kode);
        }
        return ad;
    }
    public static Map<String, OppholdÅrsak> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<OppholdÅrsak, String> {
        @Override
        public String convertToDatabaseColumn(OppholdÅrsak attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public OppholdÅrsak convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}

package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling;

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
public enum MorsAktivitet implements Kodeverdi {

    UDEFINERT("-", "Ikke satt eller valgt kode"),
    ARBEID("ARBEID", "Er i arbeid"),
    UTDANNING("UTDANNING", "Tar utdanning på heltid"),
    SAMTIDIGUTTAK("SAMTIDIGUTTAK", "Samtidig uttak flerbarnsfødsel"),
    KVALPROG("KVALPROG", "Deltar i kvalifiseringsprogrammet"),
    INTROPROG("INTROPROG", "Deltar i introduksjonsprogram for nykomne innvandrere"),
    TRENGER_HJELP("TRENGER_HJELP", "Er avhengig av hjelp til å ta seg av barnet"),
    INNLAGT("INNLAGT", "Er innlagt på institusjon"),
    ARBEID_OG_UTDANNING("ARBEID_OG_UTDANNING", "Er i arbeid og utdanning"),
    UFØRE("UFØRE", "Mor mottar uføretrygd"),
    ;
    private static final Map<String, MorsAktivitet> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "MORS_AKTIVITET";

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

    MorsAktivitet(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static MorsAktivitet fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent MorsAktivitet: " + kode);
        }
        return ad;
    }
    public static Map<String, MorsAktivitet> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<MorsAktivitet, String> {
        @Override
        public String convertToDatabaseColumn(MorsAktivitet attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public MorsAktivitet convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}

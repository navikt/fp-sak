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
public enum UtsettelseÅrsak implements Årsak {

    ARBEID("ARBEID", "Arbeid"),
    FERIE("LOVBESTEMT_FERIE", "Lovbestemt ferie"),
    SYKDOM("SYKDOM", "Avhengig av hjelp grunnet sykdom"),
    INSTITUSJON_SØKER("INSTITUSJONSOPPHOLD_SØKER", "Søker er innlagt i helseinstitusjon"),
    INSTITUSJON_BARN("INSTITUSJONSOPPHOLD_BARNET", "Barn er innlagt i helseinstitusjon"),
    HV_OVELSE("HV_OVELSE", "Heimevernet"),
    NAV_TILTAK("NAV_TILTAK", "Tiltak i regi av NAV"),
    UDEFINERT("-", "Ikke satt eller valgt kode"),
    ;
    private static final Map<String, UtsettelseÅrsak> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "UTSETTELSE_AARSAK_TYPE";

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

    UtsettelseÅrsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static UtsettelseÅrsak fraKode(@JsonProperty(value = "kode") Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(UtsettelseÅrsak.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent UtsettelseÅrsak: " + kode);
        }
        return ad;
    }
    public static Map<String, UtsettelseÅrsak> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<UtsettelseÅrsak, String> {
        @Override
        public String convertToDatabaseColumn(UtsettelseÅrsak attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public UtsettelseÅrsak convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}

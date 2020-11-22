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
public enum OverføringÅrsak implements Årsak {

    INSTITUSJONSOPPHOLD_ANNEN_FORELDER("INSTITUSJONSOPPHOLD_ANNEN_FORELDER", "Den andre foreldren er innlagt i helseinstitusjon"),
    SYKDOM_ANNEN_FORELDER("SYKDOM_ANNEN_FORELDER", "Den andre foreldren er pga sykdom avhengig av hjelp for å ta seg av barnet/barna"),
    IKKE_RETT_ANNEN_FORELDER("IKKE_RETT_ANNEN_FORELDER", "Den andre foreldren har ikke rett på foreldrepenger"),
    ALENEOMSORG("ALENEOMSORG", "Aleneomsorg for barnet/barna"),
    UDEFINERT("-", "Ikke satt eller valgt kode"),
    ;
    private static final Map<String, OverføringÅrsak> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "OVERFOERING_AARSAK_TYPE";

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

    OverføringÅrsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static OverføringÅrsak fraKode(@JsonProperty(value = "kode") Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(OverføringÅrsak.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent OverføringÅrsak: " + kode);
        }
        return ad;
    }
    public static Map<String, OverføringÅrsak> kodeMap() {
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

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<OverføringÅrsak, String> {
        @Override
        public String convertToDatabaseColumn(OverføringÅrsak attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public OverføringÅrsak convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}

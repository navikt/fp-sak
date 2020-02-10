package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode;

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
public enum FordelingPeriodeKilde implements Kodeverdi {

    SØKNAD("SØKNAD", "Kilde er søknad"),
    TIDLIGERE_VEDTAK("TIDLIGERE_VEDTAK", "Kilde er tidligere vedtatt behandling."),
    ;
    private static final Map<String, FordelingPeriodeKilde> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "FORDELING_PERIODE_KILDE";

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

    FordelingPeriodeKilde(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static FordelingPeriodeKilde fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent FordelingPeriodeKilde: " + kode);
        }
        return ad;
    }
    public static Map<String, FordelingPeriodeKilde> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<FordelingPeriodeKilde, String> {
        @Override
        public String convertToDatabaseColumn(FordelingPeriodeKilde attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public FordelingPeriodeKilde convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}

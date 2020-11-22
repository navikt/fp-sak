package no.nav.foreldrepenger.behandlingslager.behandling.klage;

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
import no.nav.foreldrepenger.behandlingslager.kodeverk.TempAvledeKode;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum KlageVurderingOmgjør implements Kodeverdi {

    GUNST_MEDHOLD_I_KLAGE("GUNST_MEDHOLD_I_KLAGE", "Gunst medhold i klage"),
    DELVIS_MEDHOLD_I_KLAGE("DELVIS_MEDHOLD_I_KLAGE", "Delvis medhold i klage"),
    UGUNST_MEDHOLD_I_KLAGE("UGUNST_MEDHOLD_I_KLAGE", "Ugunst medhold i klage"),
    UDEFINERT("-", "Udefinert"),
    ;

    private static final Map<String, KlageVurderingOmgjør> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "KLAGE_VURDERING_OMGJOER";

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

    private KlageVurderingOmgjør(String kode) {
        this.kode = kode;
    }

    private KlageVurderingOmgjør(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static KlageVurderingOmgjør fraKode(@JsonProperty(value = "kode") Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(KlageVurderingOmgjør.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent KlageVurderingOmgjør: " + kode);
        }
        return ad;
    }

    public static Map<String, KlageVurderingOmgjør> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<KlageVurderingOmgjør, String> {
        @Override
        public String convertToDatabaseColumn(KlageVurderingOmgjør attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public KlageVurderingOmgjør convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}

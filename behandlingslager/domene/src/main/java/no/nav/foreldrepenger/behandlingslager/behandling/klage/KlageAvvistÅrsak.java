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

import no.nav.foreldrepenger.behandlingslager.behandling.ÅrsakskodeMedLovreferanse;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.TempAvledeKode;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum KlageAvvistÅrsak implements Kodeverdi, ÅrsakskodeMedLovreferanse {

    KLAGET_FOR_SENT("KLAGET_FOR_SENT", "Bruker har klaget for sent",
            "{\"klageAvvistAarsak\":{\"NFP\": {\"lovreferanser\": [\"31\", \"33\"]},\"KA\": {\"lovreferanser\": [\"31\", \"34\"]}}}"),
    KLAGE_UGYLDIG("KLAGE_UGYLDIG", "Klage er ugyldig", null),
    IKKE_PAKLAGD_VEDTAK("IKKE_PAKLAGD_VEDTAK", "Ikke påklagd et vedtak",
            "{\"klageAvvistAarsak\":{\"NFP\": {\"lovreferanser\": [\"28\", \"33\"]},\"KA\": {\"lovreferanser\": [\"28\", \"34\"]}}}"),
    KLAGER_IKKE_PART("KLAGER_IKKE_PART", "Klager er ikke part",
            "{\"klageAvvistAarsak\":{\"NFP\": {\"lovreferanser\": [\"28\", \"33\"]},\"KA\": {\"lovreferanser\": [\"28\", \"34\"]}}}"),
    IKKE_KONKRET("IKKE_KONKRET", "Klagen er ikke konkret",
            "{\"klageAvvistAarsak\":{\"NFP\": {\"lovreferanser\": [\"32\", \"33\"]},\"KA\": {\"lovreferanser\": [\"32\", \"34\"]}}}"),
    IKKE_SIGNERT("IKKE_SIGNERT", "Klagen er ikke signert",
            "{\"klageAvvistAarsak\":{\"NFP\": {\"lovreferanser\": [\"31\", \"33\"]},\"KA\": {\"lovreferanser\": [\"31\", \"34\"]}}}"),
    UDEFINERT("-", "Ikke definert", null),
    ;

    private static final Map<String, KlageAvvistÅrsak> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "KLAGE_AVVIST_AARSAK";

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
    @JsonIgnore
    private String lovHjemmel;

    private KlageAvvistÅrsak(String kode) {
        this.kode = kode;
    }

    private KlageAvvistÅrsak(String kode, String navn, String lovHjemmel) {
        this.kode = kode;
        this.navn = navn;
        this.lovHjemmel = lovHjemmel;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static KlageAvvistÅrsak fraKode(@JsonProperty(value = "kode") Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(KlageAvvistÅrsak.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent KlageAvvistÅrsak: " + kode);
        }
        return ad;
    }

    public static Map<String, KlageAvvistÅrsak> kodeMap() {
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
    public String getLovHjemmelData() {
        return lovHjemmel;
    }

    @Override
    public String getOffisiellKode() {
        return getKode();
    }



    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<KlageAvvistÅrsak, String> {
        @Override
        public String convertToDatabaseColumn(KlageAvvistÅrsak attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public KlageAvvistÅrsak convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }

}

package no.nav.foreldrepenger.behandlingslager.behandling.klage;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.ÅrsakskodeMedLovreferanse;

public enum KlageAvvistÅrsak implements Kodeverdi, ÅrsakskodeMedLovreferanse {

    KLAGET_FOR_SENT("KLAGET_FOR_SENT", "Bruker har klaget for sent",
            "{\"klageAvvistAarsak\":{\"NFP\": {\"lovreferanser\": [\"31\", \"33\"]},\"KA\": {\"lovreferanser\": [\"31\", \"34\"]}}}"),
    KLAGE_UGYLDIG("KLAGE_UGYLDIG", "Klagen er ugyldig", null),
    IKKE_PAKLAGD_VEDTAK("IKKE_PAKLAGD_VEDTAK", "Ikke påklagd et vedtak",
            "{\"klageAvvistAarsak\":{\"NFP\": {\"lovreferanser\": [\"28\", \"33\"]},\"KA\": {\"lovreferanser\": [\"28\", \"34\"]}}}"),
    KLAGER_IKKE_PART("KLAGER_IKKE_PART", "Klager er ikke part",
            "{\"klageAvvistAarsak\":{\"NFP\": {\"lovreferanser\": [\"28\", \"33\"]},\"KA\": {\"lovreferanser\": [\"28\", \"34\"]}}}"),
    IKKE_KONKRET("IKKE_KONKRET", "Klagen er ikke konkret",
            "{\"klageAvvistAarsak\":{\"NFP\": {\"lovreferanser\": [\"32\", \"33\"]},\"KA\": {\"lovreferanser\": [\"32\", \"34\"]}}}"),
    IKKE_SIGNERT("IKKE_SIGNERT", "Klagen er ikke signert",
            "{\"klageAvvistAarsak\":{\"NFP\": {\"lovreferanser\": [\"32\", \"33\"]},\"KA\": {\"lovreferanser\": [\"31\", \"34\"]}}}"),
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


    private final String navn;

    @JsonValue
    private final String kode;

    private final String lovHjemmel;

    KlageAvvistÅrsak(String kode, String navn, String lovHjemmel) {
        this.kode = kode;
        this.navn = navn;
        this.lovHjemmel = lovHjemmel;
    }

    public static Map<String, KlageAvvistÅrsak> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getLovHjemmelData() {
        return lovHjemmel;
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

        private static KlageAvvistÅrsak fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent KlageAvvistÅrsak: " + kode);
            }
            return ad;
        }
    }

}

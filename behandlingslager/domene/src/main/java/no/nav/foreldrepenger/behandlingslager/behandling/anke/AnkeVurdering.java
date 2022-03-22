package no.nav.foreldrepenger.behandlingslager.behandling.anke;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum AnkeVurdering implements Kodeverdi {

    ANKE_STADFESTE_YTELSESVEDTAK("ANKE_STADFESTE_YTELSESVEDTAK", "Ytelsesvedtaket stadfestes"),
    ANKE_HJEMSEND_UTEN_OPPHEV("ANKE_HJEMSENDE_UTEN_OPPHEV", "Hjemsende uten å oppheve"),
    ANKE_OPPHEVE_OG_HJEMSENDE("ANKE_OPPHEVE_OG_HJEMSENDE", "Ytelsesvedtaket oppheves og hjemsendes"),
    ANKE_OMGJOER("ANKE_OMGJOER", "Anken omgjøres"),
    ANKE_AVVIS("ANKE_AVVIS", "Anken avvises"),
    UDEFINERT("-", "Udefinert"),
    ;

    private static final Map<String, AnkeVurdering> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "ANKEVURDERING";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;

    @JsonValue
    private String kode;

    AnkeVurdering(String kode) {
        this.kode = kode;
    }

    AnkeVurdering(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Map<String, AnkeVurdering> kodeMap() {
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

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<AnkeVurdering, String> {
        @Override
        public String convertToDatabaseColumn(AnkeVurdering attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public AnkeVurdering convertToEntityAttribute(String dbData) {
            return Optional.ofNullable(dbData).map(AnkeVurdering.KodeverdiConverter::fraKode).orElse(null);
        }

        private static AnkeVurdering fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent AnkeVurdering: " + kode);
            }
            return ad;
        }
    }
}

package no.nav.foreldrepenger.behandlingslager.uttak.svp;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum ArbeidsforholdIkkeOppfyltÅrsak implements Kodeverdi {

    // Se også Periode ikke oppfylt - der brukes 8304-83011 + 8313+8314
    INGEN(STANDARDKODE_UDEFINERT, "Ikke definert"),
    HELE_UTTAKET_ER_ETTER_3_UKER_FØR_TERMINDATO("8301", "Hele uttaket er etter 3 uker før termindato"),
    UTTAK_KUN_PÅ_HELG("8302", "Uttak kun på helg"),
    ARBEIDSGIVER_KAN_TILRETTELEGGE("8303", "Arbeidsgiver kan tilrettelegge"),
    ARBEIDSGIVER_KAN_TILRETTELEGGE_FREM_TIL_3_UKER_FØR_TERMIN("8312", "Arbeidsgiver kan tilrettelegge frem til 3 uker før termin"),
    ;

    private static final Map<String, ArbeidsforholdIkkeOppfyltÅrsak> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "SVP_ARBEIDSFORHOLD_IKKE_OPPFYLT_AARSAK";

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

    ArbeidsforholdIkkeOppfyltÅrsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static ArbeidsforholdIkkeOppfyltÅrsak fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent ArbeidsforholdIkkeOppfyltÅrsak: " + kode);
        }
        return ad;
    }

    public static Map<String, ArbeidsforholdIkkeOppfyltÅrsak> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<ArbeidsforholdIkkeOppfyltÅrsak, String> {
        @Override
        public String convertToDatabaseColumn(ArbeidsforholdIkkeOppfyltÅrsak attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public ArbeidsforholdIkkeOppfyltÅrsak convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}

package no.nav.foreldrepenger.behandlingslager.behandling.beregning;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum Inntektskategori implements Kodeverdi {

    ARBEIDSTAKER("ARBEIDSTAKER", "Arbeidstaker"),
    FRILANSER("FRILANSER", "Frilanser"),
    SELVSTENDIG_NÆRINGSDRIVENDE("SELVSTENDIG_NÆRINGSDRIVENDE", "Selvstendig næringsdrivende"),
    DAGPENGER("DAGPENGER", "Dagpenger"),
    ARBEIDSAVKLARINGSPENGER("ARBEIDSAVKLARINGSPENGER", "Arbeidsavklaringspenger"),
    SJØMANN("SJØMANN", "Arbeidstaker - Sjømann"),
    DAGMAMMA("DAGMAMMA", "Selvstendig næringsdrivende - dagmamma"),
    JORDBRUKER("JORDBRUKER", "Selvstendig næringsdrivende - jordbruker"),
    FISKER("FISKER", "Selvstendig næringsdrivende - fisker"),
    ARBEIDSTAKER_UTEN_FERIEPENGER("ARBEIDSTAKER_UTEN_FERIEPENGER", "Arbeidstaker uten feriepenger"),
    UDEFINERT(STANDARDKODE_UDEFINERT, "Ingen inntektskategori (default)"),
    ;
    private static final Map<String, Inntektskategori> KODER = new LinkedHashMap<>();
    private static final Set<Inntektskategori> GIR_FERIEPENGER = Set.of(ARBEIDSTAKER, SJØMANN);

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

    Inntektskategori(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Set<Inntektskategori> girFeriepenger() {
        return GIR_FERIEPENGER;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<Inntektskategori, String> {
        @Override
        public String convertToDatabaseColumn(Inntektskategori attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public Inntektskategori convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static Inntektskategori fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent Inntektskategori: " + kode);
            }
            return ad;
        }

    }
}

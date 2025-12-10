package no.nav.foreldrepenger.behandlingslager.geografisk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.MedOffisiellKode;

public enum Språkkode implements Kodeverdi, MedOffisiellKode {

    /**
     * Konstanter for å skrive ned kodeverdi.
     */
    NB("NB", "Norsk", "NB"),
    NN("NN", "Nynorsk", "NN"),
    EN("EN", "Engelsk", "EN"),

    UDEFINERT(STANDARDKODE_UDEFINERT, "Ikke definert", null),
    ;

    private static final Map<String, Språkkode> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;

    private String offisiellKode;

    @JsonValue
    private String kode;

    Språkkode(String kode, String navn, String offisiellKode) {
        this.kode = kode;
        this.navn = navn;
        this.offisiellKode = offisiellKode;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return offisiellKode;
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<Språkkode, String> {
        @Override
        public String convertToDatabaseColumn(Språkkode attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public Språkkode convertToEntityAttribute(String dbData) {
            return dbData == null ? null : defaultNorsk(dbData);
        }
    }

    public static Språkkode finnForKodeverkEiersKode(String offisiellSpråkkode) {
        var kode = offisiellSpråkkode == null ? null : offisiellSpråkkode.toUpperCase();
        return finnSpråkIgnoreCase(kode).orElse(Språkkode.NB);
    }

    public static Språkkode defaultNorsk(String kode) {
        return finnSpråkIgnoreCase(kode).orElse(Språkkode.NB);
    }

    private static Optional<Språkkode> finnSpråkIgnoreCase(String kode) {
        if (kode == null) {
            return Optional.empty();
        }
        return Stream.of(NB, NN, EN).filter(sp -> kode.equalsIgnoreCase(sp.getKode())).findFirst();
    }
}

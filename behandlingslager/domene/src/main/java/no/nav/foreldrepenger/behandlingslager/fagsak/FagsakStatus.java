package no.nav.foreldrepenger.behandlingslager.fagsak;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum FagsakStatus implements Kodeverdi {

    OPPRETTET("OPPR", "Opprettet"),
    UNDER_BEHANDLING("UBEH", "Under behandling"),
    LØPENDE("LOP", "Løpende"),
    AVSLUTTET("AVSLU", "Avsluttet"),
    ;

    public static final FagsakStatus DEFAULT = OPPRETTET;
    private static final Map<String, FagsakStatus> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "FAGSAK_STATUS";

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

    FagsakStatus(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Map<String, FagsakStatus> kodeMap() {
        return Collections.unmodifiableMap(KODER);
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
    public String getKodeverk() {
        return KODEVERK;
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<FagsakStatus, String> {
        @Override
        public String convertToDatabaseColumn(FagsakStatus attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public FagsakStatus convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static FagsakStatus fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent FagsakStatus: " + kode);
            }
            return ad;
        }


    }

}

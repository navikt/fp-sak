package no.nav.foreldrepenger.domene.modell.kodeverk;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;


public enum AndelKilde implements Kodeverdi {

    SAKSBEHANDLER_KOFAKBER("SAKSBEHANDLER_KOFAKBER", "Saksbehandler i steg kontroller fakta beregning"),
    PROSESS_BESTEBEREGNING("PROSESS_BESTEBEREGNING", "Prosess for besteberegning"),
    SAKSBEHANDLER_FORDELING("SAKSBEHANDLER_FORDELING", "Saksbehandler i steg for fordeling"),
    PROSESS_PERIODISERING("PROSESS_PERIODISERING", "Prosess for periodisering grunnet refusjon/gradering/utbetalingsgrad"),
    PROSESS_OMFORDELING("PROSESS_OMFORDELING", "Prosess for automatisk omfordeling"),
    PROSESS_START("PROSESS_START", "Start av beregning"),
    ;
    private static final Map<String, AndelKilde> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "ANDEL_KILDE";

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

    AndelKilde(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static AndelKilde fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent AndelKilde: " + kode);
        }
        return ad;
    }
    public static Map<String, AndelKilde> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<AndelKilde, String> {
        @Override
        public String convertToDatabaseColumn(AndelKilde attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public AndelKilde convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

    }
}

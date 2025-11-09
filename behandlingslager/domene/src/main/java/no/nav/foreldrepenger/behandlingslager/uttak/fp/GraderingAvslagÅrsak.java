package no.nav.foreldrepenger.behandlingslager.uttak.fp;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum GraderingAvslagÅrsak implements Kodeverdi {

    //MERK: Lovhjemler til brev hentes fra navn, se pattern i UttakHjemmelUtleder

    UKJENT(STANDARDKODE_UDEFINERT, "Ikke definert"),
    GRADERING_FØR_UKE_7("4504", "§14-16 andre ledd: Avslag gradering - gradering før uke 7"),
    FOR_SEN_SØKNAD("4501", "§14-16: Ikke gradering pga. for sen søknad"),
    MANGLENDE_GRADERINGSAVTALE("4502", "§14-16 femte ledd, jf §21-3: Avslag graderingsavtale mangler - ikke dokumentert"),
    MOR_OPPFYLLER_IKKE_AKTIVITETSKRAV("4503",
        "§14-16 fjerde ledd: Avslag gradering – ikke rett til gradert uttak pga. redusert oppfylt aktivitetskrav på mor"),
    AVSLAG_PGA_100_PROSENT_ARBEID("4523", "§14-16 første ledd: Avslag gradering - arbeid 100% eller mer"),
    ;

    private static final Map<String, GraderingAvslagÅrsak> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "GRADERING_AVSLAG_AARSAK";

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

    GraderingAvslagÅrsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Map<String, GraderingAvslagÅrsak> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<GraderingAvslagÅrsak, String> {
        @Override
        public String convertToDatabaseColumn(GraderingAvslagÅrsak attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public GraderingAvslagÅrsak convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static GraderingAvslagÅrsak fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent GraderingAvslagÅrsak: " + kode);
            }
            return ad;
        }
    }

}

package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

public enum OverføringÅrsak implements Årsak {

    INSTITUSJONSOPPHOLD_ANNEN_FORELDER("INSTITUSJONSOPPHOLD_ANNEN_FORELDER", "Den andre foreldren er innlagt i helseinstitusjon"),
    SYKDOM_ANNEN_FORELDER("SYKDOM_ANNEN_FORELDER", "Den andre foreldren er pga sykdom avhengig av hjelp for å ta seg av barnet/barna"),
    IKKE_RETT_ANNEN_FORELDER("IKKE_RETT_ANNEN_FORELDER", "Den andre foreldren har ikke rett på foreldrepenger"),
    ALENEOMSORG("ALENEOMSORG", "Aleneomsorg for barnet/barna"),
    UDEFINERT(STANDARDKODE_UDEFINERT, "Ikke satt eller valgt kode"),
    ;
    private static final Map<String, OverføringÅrsak> KODER = new LinkedHashMap<>();

    public static final String DISKRIMINATOR = "OVERFOERING_AARSAK_TYPE";

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

    OverføringÅrsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static OverføringÅrsak fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent OverføringÅrsak: " + kode);
        }
        return ad;
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
    public String getDiskriminator() {
        return DISKRIMINATOR;
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<OverføringÅrsak, String> {
        @Override
        public String convertToDatabaseColumn(OverføringÅrsak attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public OverføringÅrsak convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}

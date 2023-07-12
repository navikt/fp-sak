package no.nav.foreldrepenger.behandlingslager.behandling.historikk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum HistorikkOpplysningType implements Kodeverdi {

    UDEFINIERT("-", "Ikke definert"),
    ANTALL_BARN("ANTALL_BARN", "Antall barn"),
    TPS_ANTALL_BARN("TPS_ANTALL_BARN", "Antall barn"),
    FODSELSDATO("FODSELSDATO", "Fødselsdato"),
    UTTAK_PERIODE_FOM("UTTAK_PERIODE_FOM", "Fradato uttaksperiode"),
    UTTAK_PERIODE_TOM("UTTAK_PERIODE_TOM", "Tildato uttaksperiode"),
    ;

    private static final Map<String, HistorikkOpplysningType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "HISTORIKK_OPPLYSNING_TYPE";

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

    HistorikkOpplysningType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static HistorikkOpplysningType fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent HistorikkOpplysningType: " + kode);
        }
        return ad;
    }

    public static Map<String, HistorikkOpplysningType> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<HistorikkOpplysningType, String> {
        @Override
        public String convertToDatabaseColumn(HistorikkOpplysningType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public HistorikkOpplysningType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}

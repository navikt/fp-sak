package no.nav.foreldrepenger.behandlingslager.behandling.medlemskap;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum MedlemskapKildeType implements Kodeverdi {

    E500("E500", "E-500"),
    INFOTR("INFOTR", "Infotrygd"),
    AVGSYS("AVGSYS", "Avgiftsystemet"),
    APPBRK("APPBRK", "Applikasjonsbruker"),
    PP01("PP01", "Pensjon"),
    FS22("FS22", "Gosys"),
    SRVGOSYS("srvgosys", "Gosys, ikke standard"),
    SRVMELOSYS("srvmelosys", "Melosys, ikke standard"),
    MEDL("MEDL", "MEDL"),
    TPS("TPS", "TPS"),
    TP("TP", "TP"),
    LAANEKASSEN("LAANEKASSEN", "Laanekassen"),
    ANNEN("ANNEN", "Annen"),
    UDEFINERT(STANDARDKODE_UDEFINERT, "Ikke definert"),
    ;

    private static final Map<String, MedlemskapKildeType> KODER = new LinkedHashMap<>();

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

    MedlemskapKildeType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static MedlemskapKildeType fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent MedlemskapKildeType: " + kode);
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

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<MedlemskapKildeType, String> {
        @Override
        public String convertToDatabaseColumn(MedlemskapKildeType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public MedlemskapKildeType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}

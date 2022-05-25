package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;


public enum UttakDokumentasjonType implements Kodeverdi {

    UTEN_OMSORG("UTEN_OMSORG", "Søker har ikke omsorg for barnet"),
    ALENEOMSORG("ALENEOMSORG", "Søker har aleneomsorg for barnet"),
    ANNEN_FORELDER_HAR_RETT("ANNEN_FORELDER_HAR_RETT", "Annenforelder har rett for barnet"),
    MOR_STØNAD_EØS("MOR_STONAD_EOS", "Annenforelder har foreldrepenger fra EØS"),
    SYK_SØKER("SYK_SOKER", "Søker er syk eller skadet"),
    INNLAGT_SØKER("INNLAGT_SOKER", "Søker er innlagt i institusjon"),
    INNLAGT_BARN("INNLAGT_BARN", "Barn er innlagt i institusjon"),
    UTEN_DOKUMENTASJON("UTEN_DOKUMENTASJON", "Søkt periode er ikke dokumentert"),
    INSTITUSJONSOPPHOLD_ANNEN_FORELDRE("INSTITUSJONSOPPHOLD_ANNEN_FORELDRE", "Annen forelder er innlagt i institusjon"),
    SYKDOM_ANNEN_FORELDER("SYKDOM_ANNEN_FORELDER", "Annen forelder er syk eller skadet"),
    IKKE_RETT_ANNEN_FORELDER("IKKE_RETT_ANNEN_FORELDER", "Annen forelder har ikke rett for barnet"),
    ALENEOMSORG_OVERFØRING("ALENEOMSORG_OVERFØRING", "Annen forelder har ikke omsorg for barnet"),
    HV_OVELSE("HV_OVELSE", "Søker er i tjeneste eller øvelse i heimevernet"),
    NAV_TILTAK("NAV_TILTAK", "Søker er i tiltak i regi av NAV"),
    MOR_AKTIVITET("MOR_AKTIVITET", "Mor er i aktivitet"),
    ;
    private static final Map<String, UttakDokumentasjonType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "UTTAK_DOKUMENTASJON_TYPE";

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

    UttakDokumentasjonType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Map<String, UttakDokumentasjonType> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<UttakDokumentasjonType, String> {
        @Override
        public String convertToDatabaseColumn(UttakDokumentasjonType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public UttakDokumentasjonType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static UttakDokumentasjonType fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent UttakDokumentasjonType: " + kode);
            }
            return ad;
        }
    }
}

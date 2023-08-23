package no.nav.foreldrepenger.behandlingslager.behandling.historikk;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum HistorikkinnslagFeltType implements Kodeverdi {

    UDEFINIERT("-", "Ikke definert"),
    AARSAK("AARSAK", "aarsak"),
    BEGRUNNELSE("BEGRUNNELSE", "begrunnelse"),
    HENDELSE("HENDELSE", "hendelse"),
    RESULTAT("RESULTAT", "resultat"),
    OPPLYSNINGER("OPPLYSNINGER", "opplysninger"),
    ENDRET_FELT("ENDRET_FELT", "endredeFelter"),
    SKJERMLENKE("SKJERMLENKE", "skjermlenke"),
    GJELDENDE_FRA("GJELDENDE_FRA", "Gjeldende fra"),
    AKSJONSPUNKT_BEGRUNNELSE("AKSJONSPUNKT_BEGRUNNELSE", "aksjonspunktBegrunnelse"),
    AKSJONSPUNKT_GODKJENT("AKSJONSPUNKT_GODKJENT", "aksjonspunktGodkjent"),
    AKSJONSPUNKT_KODE("AKSJONSPUNKT_KODE", "aksjonspunktKode"),
    AVKLART_SOEKNADSPERIODE("AVKLART_SOEKNADSPERIODE", "Avklart soeknadsperiode"),
    ANGÅR_TEMA("ANGÅR_TEMA", "Angår tema"),
    ;

    private static final Map<String, HistorikkinnslagFeltType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "HISTORIKKINNSLAG_FELT_TYPE";

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

    HistorikkinnslagFeltType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Map<String, HistorikkinnslagFeltType> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<HistorikkinnslagFeltType, String> {
        @Override
        public String convertToDatabaseColumn(HistorikkinnslagFeltType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public HistorikkinnslagFeltType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static HistorikkinnslagFeltType fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent HistorikkinnslagFeltType: " + kode);
            }
            return ad;
        }
    }
}

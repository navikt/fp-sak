package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;


public enum UtsettelseÅrsak implements Årsak {

    ARBEID("ARBEID", "Arbeid"),
    FERIE("LOVBESTEMT_FERIE", "Lovbestemt ferie"),
    SYKDOM("SYKDOM", "Avhengig av hjelp grunnet sykdom"),
    INSTITUSJON_SØKER("INSTITUSJONSOPPHOLD_SØKER", "Søker er innlagt i helseinstitusjon"),
    INSTITUSJON_BARN("INSTITUSJONSOPPHOLD_BARNET", "Barn er innlagt i helseinstitusjon"),
    HV_OVELSE("HV_OVELSE", "Heimevernet"),
    NAV_TILTAK("NAV_TILTAK", "Tiltak i regi av NAV"),
    FRI("FRI", "Fri utsettelse"),
    UDEFINERT("-", "Ikke satt eller valgt kode"),
    ;
    private static final Map<String, UtsettelseÅrsak> KODER = new LinkedHashMap<>();
    private static final Map<String, UtsettelseÅrsak> KODER_EKSTERN = new LinkedHashMap<>();

    public static final String KODEVERK = "UTSETTELSE_AARSAK_TYPE";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
            KODER_EKSTERN.putIfAbsent(v.kode, v);
        }
        // TODO vurder om behov for å manuelt opprette fri utsettelse fra GUI + finne diskrimininator (saksavhengig)
        KODER_EKSTERN.remove(FRI.getKode());
    }

    private String navn;

    @JsonValue
    private String kode;

    UtsettelseÅrsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static UtsettelseÅrsak fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent UtsettelseÅrsak: " + kode);
        }
        return ad;
    }
    public static Map<String, UtsettelseÅrsak> kodeMap() {
        return Collections.unmodifiableMap(KODER_EKSTERN);
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
    public static class KodeverdiConverter implements AttributeConverter<UtsettelseÅrsak, String> {
        @Override
        public String convertToDatabaseColumn(UtsettelseÅrsak attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public UtsettelseÅrsak convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}

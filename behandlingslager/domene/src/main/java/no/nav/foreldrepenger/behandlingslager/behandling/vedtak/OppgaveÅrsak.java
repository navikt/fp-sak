package no.nav.foreldrepenger.behandlingslager.behandling.vedtak;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.Årsak;

public enum OppgaveÅrsak implements Årsak {

    UDEFINERT("-", "Ikke satt eller valgt kode"),
    VUR_KONS_YTE("VUR_KONS_YTE", "Vurder konsekvens for ytelse"),
    VUR("VUR", "Vurder dokument");

    public static final String KODEVERK = "OPPGAVE_AARSAK_TYPE";
    private static final Map<String, OppgaveÅrsak> KODER = new LinkedHashMap<>();

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

    OppgaveÅrsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static OppgaveÅrsak fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent OppgaveÅrsak: " + kode);
        }
        return ad;
    }

    public static Map<String, OppgaveÅrsak> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<OppgaveÅrsak, String> {
        @Override
        public String convertToDatabaseColumn(OppgaveÅrsak attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public OppgaveÅrsak convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}

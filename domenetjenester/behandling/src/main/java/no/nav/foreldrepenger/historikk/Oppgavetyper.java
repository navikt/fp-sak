package no.nav.foreldrepenger.historikk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.MedOffisiellKode;

public enum Oppgavetyper implements Kodeverdi, MedOffisiellKode {

    BEHANDLE_SAK_VL("BEH_SAK_VL", "Behandle sak i VL"),
    REVURDER_VL("RV_VL", "Revurdere i VL"),
    GODKJENN_VEDTAK_VL("GOD_VED_VL", "Godkjenne vedtak i VL"),
    REG_SOKNAD_VL("REG_SOK_VL", "Registrere søknad i VL"),
    VURDER_KONSEKVENS_YTELSE("VUR_KONS_YTE", "Vurder konsekvens for ytelse"),
    VURDER_DOKUMENT_VL("VUR_VL", "Vurder dokument i VL"),
    FEILUTBETALING("FEILUTBET", "Feilutbetalingsvedtak"),
    INNHENT_DOK("INNH_DOK", "Innhent dokumentasjon"),
    SETTVENT("SETTVENT", "Sett utbetaling på vent"),
    BEHANDLE_SAK_IT("BEH_SAK", "Behandle sak");

    public static final String KODEVERK = "OPPGAVETYPER";

    private static final Map<String, Oppgavetyper> KODER = new LinkedHashMap<>();

    private String navn;

    @JsonValue
    private String kode;

    Oppgavetyper(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Map<String, Oppgavetyper> kodeMap() {
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

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<Oppgavetyper, String> {
        @Override
        public String convertToDatabaseColumn(Oppgavetyper attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public Oppgavetyper convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static Oppgavetyper fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent OppgaveÅrsak: " + kode);
            }
            return ad;
        }
    }

}

package no.nav.foreldrepenger.historikk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum OppgaveÅrsak implements Kodeverdi {

    BEHANDLE_SAK("BEH_SAK_VL", "Behandle sak i VL"),
    BEHANDLE_SAK_INFOTRYGD("BEH_SAK_FOR", "Behandle sak i Infotrygd"),
    SETT_ARENA_UTBET_VENT("SETTVENT_STO", "Sett Arena utbetaling på vent"),
    REGISTRER_SØKNAD("REG_SOK_VL", "Registrere søknad i VL"),
    GODKJENNE_VEDTAK("GOD_VED_VL", "Godkjenne vedtak i VL"),
    REVURDER("RV_VL", "Revurdere i VL"),
    VURDER_DOKUMENT("VUR_VL", "Vurder dokument i VL"),
    VURDER_KONS_FOR_YTELSE("VUR_KONS_YTE_FOR", "Vurder konsekvens for ytelse foreldrepenger"),
    INNHENT_DOKUMENTASJON("INNHENT_DOK", "Innhent dokumentasjon"),
    UDEFINERT("-", "Ikke definert"),
    ;

    public static final String KODEVERK = "OPPGAVE_AARSAK";

    private static final Map<String, OppgaveÅrsak> KODER = new LinkedHashMap<>();


    private String navn;

    @JsonValue
    private String kode;

    OppgaveÅrsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
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

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

}

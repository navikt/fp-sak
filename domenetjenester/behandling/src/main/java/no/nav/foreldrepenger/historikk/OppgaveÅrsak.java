package no.nav.foreldrepenger.historikk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
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

    @JsonIgnore
    private String navn;

    private String kode;

    private OppgaveÅrsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static OppgaveÅrsak fraKode(@JsonProperty("kode") String kode) {
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

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }

    @JsonProperty
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @JsonProperty
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

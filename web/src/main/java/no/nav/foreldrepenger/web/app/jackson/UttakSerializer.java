package no.nav.foreldrepenger.web.app.jackson;

import java.io.IOException;
import java.util.Set;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeUtfallÅrsak;

/**
 * Enkel serialisering av KodeverkTabell klasser, uten at disse trenger @JsonIgnore eller lignende. Deserialisering går
 * av seg selv normalt (får null for andre felter).
 */
public class UttakSerializer extends StdSerializer<PeriodeUtfallÅrsak> {

    private boolean serialiserKodelisteNavn;

    public UttakSerializer(boolean serialiserKodelisteNavn) {
        super(PeriodeUtfallÅrsak.class);
        this.serialiserKodelisteNavn = serialiserKodelisteNavn;
    }

    @Override
    public void serialize(PeriodeUtfallÅrsak value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        /*
         * Midlertidig til vi helt skiller vanlig serialisering (JsonValue) fra custom kodemapserialisering
         */
        if (!serialiserKodelisteNavn) {
            jgen.writeString(value.getKode());
            return;
        }

        jgen.writeStartObject();

        jgen.writeStringField("kode", value.getKode());
        jgen.writeStringField("navn", value.getNavn());
        jgen.writeStringField("kodeverk", value.getKodeverk());
        if (value.getUtfallType() != null) {
            jgen.writeStringField("utfallType", value.getUtfallType().name());
        }
        writeArray(jgen, value.getGyldigForLovendringer(), "gyldigForLovendringer");
        writeArray(jgen, value.getUttakTyper(), "uttakTyper");
        writeArray(jgen, value.getValgbarForKonto(), "valgbarForKonto");

        jgen.writeEndObject();
    }

    private void writeArray(JsonGenerator jgen, Set<?> set, String fieldName) throws IOException {
        jgen.writeFieldName(fieldName);
        jgen.writeStartArray();
        for (Object item : set) {
            jgen.writeString(item.toString());
        }
        jgen.writeEndArray();
    }

}

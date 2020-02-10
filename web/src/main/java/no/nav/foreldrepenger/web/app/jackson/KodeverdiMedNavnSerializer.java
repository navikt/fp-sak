package no.nav.foreldrepenger.web.app.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

/**
 * Enkel serialisering av KodeverkTabell klasser, uten at disse trenger @JsonIgnore eller lignende. Deserialisering går
 * av seg selv normalt (får null for andre felter).
 */
public class KodeverdiMedNavnSerializer extends StdSerializer<Kodeverdi> {

    private boolean serialiserKodelisteNavn;

    public KodeverdiMedNavnSerializer(boolean serialiserKodelisteNavn) {
        super(Kodeverdi.class);
        this.serialiserKodelisteNavn = serialiserKodelisteNavn;
    }

    @Override
    public void serialize(Kodeverdi value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();
        jgen.writeStringField("kode", value.getKode());

        if (serialiserKodelisteNavn) {
            jgen.writeStringField("navn", value.getNavn());
        }
        jgen.writeStringField("kodeverk", value.getKodeverk());
        jgen.writeEndObject();
    }

}

package no.nav.foreldrepenger.web.app.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

/**
 * Enkel serialisering av KodeverkTabell klasser, uten at disse trenger @JsonIgnore eller lignende.
 * Deserialisering går av seg selv normalt (får null for andre felter).
 *
 * TODO: Flytt til web kodeverk KodeverRestTjeneste når all normal (De)Ser av Kodeverdi skjer med JsonValue
 */
public class KodeverdiSerializer extends StdSerializer<Kodeverdi> {

    public KodeverdiSerializer() {
        super(Kodeverdi.class);
    }

    @Override
    public void serialize(Kodeverdi value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        /*
         * Midlertidig til vi helt skiller vanlig serialisering (JsonValue) fra custom kodemapserialisering
         */
        jgen.writeString(value.getKode());
    }

}

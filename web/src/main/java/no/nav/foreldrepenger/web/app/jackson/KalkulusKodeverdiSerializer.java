package no.nav.foreldrepenger.web.app.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import no.nav.folketrygdloven.kalkulus.kodeverk.Kodeverdi;


/**
 * Enkel serialisering av Kodeverk som stammer fra ftkalkulus kontrakt/kodeverk
 * Deserialisering går av seg selv normalt
 *
 * TODO: rydde i kalkulus-kodeverk/kontrakt slik at man fortrinnsvis bruker plain enum og null isf UDEFINERT
 */
public class KalkulusKodeverdiSerializer extends StdSerializer<Kodeverdi> {

    public KalkulusKodeverdiSerializer() {
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

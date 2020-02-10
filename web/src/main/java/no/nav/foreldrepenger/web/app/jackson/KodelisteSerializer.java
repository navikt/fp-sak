package no.nav.foreldrepenger.web.app.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import no.nav.foreldrepenger.behandlingslager.kodeverk.BasisKodeverdi;

/**
 * Enkel serialisering av KodeverkTabell klasser, uten at disse trenger @JsonIgnore eller lignende. Deserialisering går
 * av seg selv normalt (får null for andre felter).
 */
public class KodelisteSerializer extends StdSerializer<BasisKodeverdi> {

    /** dropper navn hvis false (trenger da ikke refreshe navn fra db.). Default false */
    private boolean serialiserKodelisteNavn;

    public KodelisteSerializer() {
        this(false);
    }

    public KodelisteSerializer(boolean serialiserKodelisteNavn) {
        super(BasisKodeverdi.class);
        this.serialiserKodelisteNavn = serialiserKodelisteNavn;
    }

    @Override
    public void serialize(BasisKodeverdi value, JsonGenerator jgen, SerializerProvider provider) throws IOException {

        jgen.writeStartObject();

        jgen.writeStringField("kode", value.getKode());
        
        if (serialiserKodelisteNavn) {
            jgen.writeStringField("navn", value.getNavn());
        }
        
        jgen.writeStringField("kodeverk", value.getKodeverk());

        jgen.writeEndObject();
    }

}

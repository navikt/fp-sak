package no.nav.foreldrepenger.web.app.tjenester.kodeverk.app;

import java.io.IOException;
import java.util.Set;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.MedGyldighetsperiode;
import no.nav.foreldrepenger.behandlingslager.kodeverk.MedLovHjemmel;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.MedUttakParametre;

/**
 * Enkel serialisering av Kodevedi'er for å sende en stor map mot navn, etc til frontend.
 */
public class KodeverdiSerializer extends StdSerializer<Kodeverdi> {

    public KodeverdiSerializer() {
        super(Kodeverdi.class);
    }

    @Override
    public void serialize(Kodeverdi value, JsonGenerator jgen, SerializerProvider provider) throws IOException {

        jgen.writeStartObject();

        jgen.writeStringField("kode", value.getKode());
        jgen.writeStringField("kodeverk", value.getKodeverk());
        jgen.writeStringField("navn", value.getNavn());

        if (value instanceof MedGyldighetsperiode g) {
            jgen.writeStringField("gyldigFom", g.getGyldigFraOgMed().toString());
            jgen.writeStringField("gyldigTom", g.getGyldigTilOgMed().toString());
        }
        if (value instanceof MedLovHjemmel l) {
            jgen.writeStringField("lovHjemmel", l.getLovHjemmel());
        }
        if (value instanceof MedUttakParametre uttakParametre) {
            writeArray(jgen, uttakParametre.getGyldigForLovendringer(), "gyldigForLovendringer");
            writeArray(jgen, uttakParametre.getUttakTyper(), "uttakTyper");
            writeArray(jgen, uttakParametre.getValgbarForKonto(), "valgbarForKonto");
        }

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

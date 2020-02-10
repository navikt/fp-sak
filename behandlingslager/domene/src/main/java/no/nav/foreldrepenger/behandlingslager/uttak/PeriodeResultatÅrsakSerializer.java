package no.nav.foreldrepenger.behandlingslager.uttak;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatÅrsak;

/**
 * Enkel serialisering av KodeverkTabell klass PeriodeResultatÅrsak, uten at disse trenger @JsonIgnore eller lignende. Deserialisering går
 * av seg selv normalt (får null for andre felter).
 */
public class PeriodeResultatÅrsakSerializer<V extends PeriodeResultatÅrsak> extends StdSerializer<V> {

    public PeriodeResultatÅrsakSerializer(Class<V> targetCls) {
        super(targetCls);
    }

    @Override
    public void serialize(V value, JsonGenerator jgen, SerializerProvider provider) throws IOException {

        jgen.writeStartObject();

        jgen.writeStringField("kode", value.getKode());
        jgen.writeStringField("navn", value.getNavn());
        jgen.writeStringField("kodeverk", value.getKodeverk());
        jgen.writeStringField("gyldigFom", value.getGyldigFraOgMed().toString());
        jgen.writeStringField("gyldigTom", value.getGyldigTilOgMed().toString());

        jgen.writeEndObject();
    }

}

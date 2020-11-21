package no.nav.foreldrepenger.behandlingslager.uttak.fp;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import no.nav.foreldrepenger.behandlingslager.behandling.ÅrsakskodeMedLovreferanse;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak.MyPeriodeResultatÅrsakSerializer;
import no.nav.vedtak.konfig.Tid;

@JsonDeserialize(using = PeriodeResultatÅrsakDeserializer.class)
@JsonSerialize(using = MyPeriodeResultatÅrsakSerializer.class)
public interface PeriodeResultatÅrsak extends ÅrsakskodeMedLovreferanse, Kodeverdi {

    PeriodeResultatÅrsak UKJENT = new PeriodeResultatÅrsak() {

        @Override
        public Set<UttakType> getUttakTyper() {
            return Set.of();
        }

        @Override
        public Set<StønadskontoType> getValgbarForKonto() {
            return Set.of();
        }

        @Override
        public String getNavn() {
            return "Ikke definert";
        }

        @Override
        public String getKodeverk() {
            return "PERIODE_RESULTAT_AARSAK";
        }

        @Override
        public String getKode() {
            return "-";
        }

        @Override
        public String getLovHjemmelData() {
            return null;
        }
    };

    @JsonProperty("gyldigFom")
    default LocalDate getGyldigFraOgMed() {
        return LocalDate.of(2001, 01, 01);
    }

    @JsonProperty("gyldigTom")
    default LocalDate getGyldigTilOgMed() {
        return Tid.TIDENES_ENDE;
    }

    Set<UttakType> getUttakTyper();

    Set<StønadskontoType> getValgbarForKonto();

    /**
     * Enkel serialisering av KodeverkTabell klass PeriodeResultatÅrsak, uten at disse trenger @JsonIgnore eller lignende. Deserialisering går
     * av seg selv normalt (får null for andre felter).
     */
    class PeriodeResultatÅrsakSerializer<V extends PeriodeResultatÅrsak> extends StdSerializer<V> {


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

    class MyPeriodeResultatÅrsakSerializer extends PeriodeResultatÅrsakSerializer<PeriodeResultatÅrsak> {
        public MyPeriodeResultatÅrsakSerializer() {
            super(PeriodeResultatÅrsak.class);
        }
    }
}

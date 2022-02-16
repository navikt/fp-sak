package no.nav.foreldrepenger.familiehendelse.rest;

import java.io.IOException;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.familiehendelse.rest.SøknadType.SøknadTypeDeserializer;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonDeserialize(using = SøknadTypeDeserializer.class)
public enum SøknadType {
    FØDSEL("ST-001"), //$NON-NLS-1$
    ADOPSJON("ST-002"), //$NON-NLS-1$
    ;

    private final String kode;

    SøknadType(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }

    public static SøknadType fra(String kode) {
        for (var st : values()) {
            if (Objects.equals(st.kode, kode)) {
                return st;
            }
        }
        throw new IllegalArgumentException("Ukjent " + SøknadType.class.getSimpleName() + ": " + kode); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public static SøknadType fra(FamilieHendelseEntitet type) {
        if (type == null) {
            return null;
        }
        if (type.getGjelderFødsel()) {
            return SøknadType.FØDSEL;
        }
        if (type.getGjelderAdopsjon()) {
            return SøknadType.ADOPSJON;
        }
        throw new IllegalArgumentException("Kan ikke mappe fra familieHendelse" + type + " til SøknadType"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    static class SøknadTypeDeserializer extends StdDeserializer<SøknadType> {

        public SøknadTypeDeserializer() {
            super(SøknadType.class);
        }

        @Override
        public SøknadType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            // [JACKSON-620] Empty String can become null...

            if (p.hasToken(JsonToken.VALUE_STRING)
                    && ctxt.isEnabled(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
                    && p.getText().length() == 0) {
                return null;
            }

            String kode = null;

            if (Objects.equals(p.getCurrentToken(), JsonToken.START_OBJECT)) {
                while (!(JsonToken.END_OBJECT.equals(p.getCurrentToken()))) {
                    p.nextToken();
                    var name = p.getCurrentName();
                    var value = p.getValueAsString();
                    if (Objects.equals("kode", name)  && !Objects.equals("kode", value)) { //$NON-NLS-1$ //$NON-NLS-2$
                        kode = value;
                    }
                }
            }

            return SøknadType.fra(kode);
        }

    }


}


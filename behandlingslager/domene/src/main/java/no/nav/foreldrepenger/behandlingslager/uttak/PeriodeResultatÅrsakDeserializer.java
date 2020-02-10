package no.nav.foreldrepenger.behandlingslager.uttak;

import java.io.IOException;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class PeriodeResultatÅrsakDeserializer extends StdDeserializer<PeriodeResultatÅrsak> {

    public PeriodeResultatÅrsakDeserializer() {
        super(PeriodeResultatÅrsak.class);
    }

    @Override
    public PeriodeResultatÅrsak deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        // [JACKSON-620] Empty String can become null...

        if (p.hasToken(JsonToken.VALUE_STRING)
            && ctxt.isEnabled(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
            && p.getText().length() == 0) {
            return null;
        }

        String kode = null;
        String kodeverk = PeriodeResultatÅrsak.UKJENT.getKodeverk();

        if (Objects.equals(p.getCurrentToken(), JsonToken.START_OBJECT)) {
            while (!(JsonToken.END_OBJECT.equals(p.getCurrentToken()))) {
                p.nextToken();
                String name = p.getCurrentName();
                String value = p.getValueAsString();
                if (Objects.equals("kode", name) && !Objects.equals("kode", value)) { //$NON-NLS-1$ //$NON-NLS-2$
                    kode = value;
                } else if (Objects.equals("kodeverk", name) && !Objects.equals("kodeverk", value)) { //$NON-NLS-1$ //$NON-NLS-2$
                    kodeverk = value;
                }

            }
        }

        if (Objects.equals(InnvilgetÅrsak.KODEVERK, kodeverk)) {
            return InnvilgetÅrsak.fraKode(kode);
        } else if (Objects.equals(IkkeOppfyltÅrsak.KODEVERK, kodeverk)) {
            return IkkeOppfyltÅrsak.fraKode(kode);
        } else {
            return PeriodeResultatÅrsak.UKJENT;
        }
    }

}

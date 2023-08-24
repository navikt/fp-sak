package no.nav.foreldrepenger.web.app.exceptions;

import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsonMappingExceptionMapperTest {

    @Test
    void skal_mappe_InvalidTypeIdException() {
        var resultat = new JsonMappingExceptionMapper().toResponse(new InvalidTypeIdException(null, "Ukjent type-kode", null, "23525"));
        var dto = (FeilDto) resultat.getEntity();
        assertThat(dto.getFeilmelding()).contains("JSON-mapping feil");
        assertThat(dto.getFeltFeil()).isEmpty();
    }
}

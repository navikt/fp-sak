package no.nav.foreldrepenger.web.app.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;

public class JsonMappingExceptionMapperTest {

    @Test
    public void skal_mappe_InvalidTypeIdException() throws Exception {
        var resultat = new JsonMappingExceptionMapper().toResponse(new InvalidTypeIdException(null, "Ukjent type-kode", null, "23525"));
        var dto = (FeilDto) resultat.getEntity();
        assertThat(dto.getFeilmelding()).contains("JSON-mapping feil");
        assertThat(dto.getFeltFeil()).isEmpty();
    }
}

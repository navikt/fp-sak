package no.nav.foreldrepenger.web.app.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import javax.ws.rs.core.Response;

import org.junit.Test;

import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;

public class JsonMappingExceptionMapperTest {

    @Test
    public void skal_mappe_InvalidTypeIdException() throws Exception {
        JsonMappingExceptionMapper mapper = new JsonMappingExceptionMapper();
        @SuppressWarnings("resource")
        Response resultat = mapper.toResponse(new InvalidTypeIdException(null, "Ukjent type-kode", null, "23525"));
        FeilDto dto = (FeilDto) resultat.getEntity();
        assertThat(dto.getFeilmelding()).isEqualTo("JSON-mapping feil");
        assertThat(dto.getFeltFeil()).isNull();
    }
}

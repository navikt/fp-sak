package no.nav.foreldrepenger.web.app.exceptions;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;

@Provider
public class JsonParseExceptionMapper implements ExceptionMapper<JsonParseException> {

    private static final Logger LOG = LoggerFactory.getLogger(JsonParseExceptionMapper.class);

    @Override
    public Response toResponse(JsonParseException exception) {
        var feil = String.format("FP-299955 JSON-parsing feil: %s", exception.getMessage());
        LOG.warn(feil, exception);
        return Response
            .status(Response.Status.BAD_REQUEST)
            .entity(new FeilDto(feil))
            .type(MediaType.APPLICATION_JSON)
            .build();
    }


}

package no.nav.foreldrepenger.web.app.exceptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.vedtak.sikkerhet.ContextPathHolder;

@ExtendWith(MockitoExtension.class)
public class RedirectExceptionMapperTest {

    @Mock
    private GeneralRestExceptionMapper generalRestExceptionMapper;

    private RedirectExceptionMapper exceptionMapper;

    @BeforeEach
    public void setUp() {
        exceptionMapper = new RedirectExceptionMapper("https://erstatter.nav.no", generalRestExceptionMapper);
        ContextPathHolder.instance("/fpsak");
    }

    @Test
    public void skalMappeValideringsfeil() {
        // Arrange
        var generalResponse = Response.status(Response.Status.FORBIDDEN)
                .entity(new FeilDto(FeilType.MANGLER_TILGANG_FEIL, "feilmelding"))
                .type(MediaType.APPLICATION_JSON)
                .build();

        var exception = new WebApplicationException();
        when(generalRestExceptionMapper.toResponse(exception)).thenReturn(generalResponse);

        var response = exceptionMapper.toResponse(exception);

        assertThat(response.getStatus()).isEqualTo(Response.Status.TEMPORARY_REDIRECT.getStatusCode());
        assertThat(response.getMediaType()).isNull();
        assertThat(response.getMetadata().get("Content-Encoding").get(0))
                .isEqualTo("UTF-8");
        assertThat(response.getMetadata().get("Location").get(0).toString())
                .isEqualTo("https://erstatter.nav.no/fpsak/#?errorcode=feilmelding");
    }

}

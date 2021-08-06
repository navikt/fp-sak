package no.nav.foreldrepenger.web.app.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.sikkerhet.ContextPathHolder;

@ExtendWith(MockitoExtension.class)
public class RedirectExceptionMapperTest {


    private RedirectExceptionMapper exceptionMapper;

    @BeforeEach
    public void setUp() {
        exceptionMapper = new RedirectExceptionMapper("https://erstatter.nav.no");
        ContextPathHolder.instance("/fpsak");
    }

    @Test
    public void skalMappeValideringsfeil() {
        // Arrange
        var exception = new ManglerTilgangException("KODE", "MANGLERTILGANG");

        var response = exceptionMapper.toResponse(exception);

        assertThat(response.getStatus()).isEqualTo(Response.Status.TEMPORARY_REDIRECT.getStatusCode());
        assertThat(response.getMediaType()).isNull();
        assertThat(response.getMetadata().get("Content-Encoding").get(0))
                .isEqualTo("UTF-8");
        assertThat(response.getMetadata().get("Location").get(0))
                .hasToString("https://erstatter.nav.no/fpsak/#?errorcode=KODE%3AMANGLERTILGANG");
    }

}

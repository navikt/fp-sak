package no.nav.foreldrepenger.web.app.exceptions;

import java.net.URI;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.owasp.encoder.Encode;
import org.slf4j.MDC;

import no.nav.vedtak.sikkerhet.ContextPathHolder;
import no.nav.foreldrepenger.konfig.Environment;

@Provider
public class RedirectExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Environment ENV = Environment.current();
    private final String loadBalancerUrl;

    public RedirectExceptionMapper() {
        this(ENV.getProperty("loadbalancer.url"));
    }

    RedirectExceptionMapper(String url) {
        this.loadBalancerUrl = url;
    }

    @Override
    public Response toResponse(Throwable exception) {
        try {
            var response = GeneralRestExceptionMapper.handleException(exception);
            var feilmelding = ((FeilDto) response.getEntity()).getFeilmelding();
            var enkodetFeilmelding = Encode.forUriComponent(feilmelding);

            var formattertFeilmelding = String.format("%s/#?errorcode=%s", getBaseUrl(), enkodetFeilmelding);//$NON-NLS-1$
            var responser = Response.temporaryRedirect(URI.create(formattertFeilmelding));
            responser.encoding("UTF-8");
            return responser.build();
        } finally {
            MDC.remove("prosess"); //$NON-NLS-1$
        }
    }

    String getBaseUrl() {
        return loadBalancerUrl + ContextPathHolder.instance().getContextPath();
    }

}

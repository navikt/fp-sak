package no.nav.foreldrepenger.web.app.exceptions;

import java.net.URI;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.spi.ApplicationException;
import org.owasp.encoder.Encode;

import no.nav.vedtak.sikkerhet.ContextPathHolder;
import no.nav.vedtak.util.env.Environment;

@Provider
public class RedirectExceptionMapper implements ExceptionMapper<ApplicationException> {

    private static final Environment ENV = Environment.current();
    private final String loadBalancerUrl;
    private final GeneralRestExceptionMapper generalRestExceptionMapper;

    public RedirectExceptionMapper() {
        this(ENV.getProperty("loadbalancer.url"));
    }

    RedirectExceptionMapper(String url) {
        this(url, new GeneralRestExceptionMapper());
    }

    RedirectExceptionMapper(String url, GeneralRestExceptionMapper mapper) {
        this.generalRestExceptionMapper = mapper;
        this.loadBalancerUrl = url;
    }

    @Override
    public Response toResponse(ApplicationException exception) {
        var response = generalRestExceptionMapper.toResponse(exception);
        var feilmelding = ((FeilDto) response.getEntity()).getFeilmelding();
        var enkodetFeilmelding = Encode.forUriComponent(feilmelding);

        var formattertFeilmelding = String.format("%s/#?errorcode=%s", getBaseUrl(), enkodetFeilmelding);//$NON-NLS-1$
        var responser = Response.temporaryRedirect(URI.create(formattertFeilmelding));
        responser.encoding("UTF-8");
        return responser.build();
    }

    String getBaseUrl() {
        return loadBalancerUrl + ContextPathHolder.instance().getContextPath();
    }

    GeneralRestExceptionMapper getGeneralRestExceptionMapper() {
        return generalRestExceptionMapper;
    }

}

package no.nav.foreldrepenger.web.app.oppgave;

import java.util.Map;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.glassfish.jersey.server.ServerProperties;

import no.nav.foreldrepenger.web.app.exceptions.GeneralRestExceptionMapper;
import no.nav.foreldrepenger.web.app.jackson.JacksonJsonConfig;

@ApplicationPath("oppgaveredirect")
public class OppgaveRedirectApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(OppgaveRedirectTjeneste.class, GeneralRestExceptionMapper.class, JacksonJsonConfig.class);
    }

    @Override
    public Map<String, Object> getProperties() {
        return Map.of(ServerProperties.PROCESSING_RESPONSE_ERRORS_ENABLED, true);
    }
}

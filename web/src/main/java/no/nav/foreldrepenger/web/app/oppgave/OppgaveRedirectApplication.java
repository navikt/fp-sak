package no.nav.foreldrepenger.web.app.oppgave;

import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import no.nav.foreldrepenger.web.app.exceptions.RedirectExceptionMapper;
import no.nav.foreldrepenger.web.app.jackson.JacksonJsonConfig;

@ApplicationPath("oppgaveredirect")
public class OppgaveRedirectApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(OppgaveRedirectTjeneste.class,
                RedirectExceptionMapper.class,
                JacksonJsonConfig.class);
    }
}

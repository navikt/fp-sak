package no.nav.foreldrepenger.web.app.startupinfo;

import javax.enterprise.inject.spi.CDI;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import no.nav.foreldrepenger.web.app.jackson.HealthCheckRestService;

public class AppStartupServletContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        CDI.current().select(HealthCheckRestService.class).get().setIsContextStartupReady(Boolean.TRUE);
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        // ikke noe
    }
}

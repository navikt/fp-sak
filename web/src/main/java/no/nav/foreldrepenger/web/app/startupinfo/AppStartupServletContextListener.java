package no.nav.foreldrepenger.web.app.startupinfo;

import javax.enterprise.inject.spi.CDI;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.web.app.healthchecks.HealthCheckRestService;

public class AppStartupServletContextListener implements ServletContextListener {

    private static final Logger logger = LoggerFactory.getLogger(AppStartupServletContextListener.class);

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {

        // start denne async, logg til slutt når ferdig
        Thread thread = new Thread(this::startupLogging, getClass().getSimpleName() + "-thread");
        thread.setDaemon(true);
        thread.start();

        CDI.current().select(HealthCheckRestService.class).get().setIsReady(Boolean.TRUE);
    }

    private void startupLogging() {
        // Henter dependent instance og destroyer etterpå.
        AppStartupInfoLogger appStartupInfoLogger = null;
        try {
            Thread.sleep(30L * 1000L); // La verden gå litt videre får vi dumper ut
            appStartupInfoLogger = CDI.current().select(AppStartupInfoLogger.class).get();
            appStartupInfoLogger.logAppStartupInfo();
        } catch (Exception e) {
            OppstartFeil.FACTORY.uventetExceptionVedOppstart(e).log(logger);
            // men ikke re-throw - vi ønsker ikke at oppstart skal feile pga. feil i logging
        } finally {
            if (appStartupInfoLogger != null) {
                CDI.current().destroy(appStartupInfoLogger);
            }
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        // ikke noe
    }
}

package no.nav.foreldrepenger.web.app.tjenester;

import jakarta.inject.Inject;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class ServiceStarterListener implements ServletContextListener {

    @Inject
    private ApplicationServiceStarter applicationServiceStarter;  // vil ikke fungere med constructor innjection

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        applicationServiceStarter.startServices();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        applicationServiceStarter.stopServices();
    }

}

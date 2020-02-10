package no.nav.foreldrepenger.web.server.jetty;

import java.io.IOException;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;

import no.nav.vedtak.konfig.PropertiesKonfigVerdiProvider;
import no.nav.vedtak.konfig.StandardPropertySource;

@ApplicationScoped
public class ApplicationPropertiesKonfigVerdiProvider extends PropertiesKonfigVerdiProvider {

    protected ApplicationPropertiesKonfigVerdiProvider() {
        super(initApplicationProperties(), StandardPropertySource.APP_PROPERTIES);
    }

    static Properties initApplicationProperties() {
        String appPropertyFile = "/application.properties";
        try (var is = JettyServer.class.getResourceAsStream(appPropertyFile);) {
            Properties appProps = new Properties();
            appProps.load(is);
            return appProps;
        } catch (IOException e) {
            throw new IllegalStateException("Kunne ikke laste props fra " + appPropertyFile); //$NON-NLS-1$
        }
    }

    @Override
    public int getPrioritet() {
        return 999; // Lav prioritet (enn system props, etc)
    }

}

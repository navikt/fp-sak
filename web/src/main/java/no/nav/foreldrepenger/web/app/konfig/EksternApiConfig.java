package no.nav.foreldrepenger.web.app.konfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.web.app.exceptions.ConstraintViolationMapper;
import no.nav.foreldrepenger.web.app.exceptions.GeneralRestExceptionMapper;
import no.nav.foreldrepenger.web.app.exceptions.JsonMappingExceptionMapper;
import no.nav.foreldrepenger.web.app.exceptions.JsonParseExceptionMapper;
import no.nav.foreldrepenger.web.app.jackson.JacksonJsonConfig;
import no.nav.foreldrepenger.web.app.tjenester.vedtak.ytelseinfo.EksternDelingYtelseInfoRestTjeneste;
import no.nav.foreldrepenger.web.server.abac.EksternPipRestTjeneste;

@ApplicationPath(EksternApiConfig.API_URI)
public class EksternApiConfig extends ResourceConfig {

    private static final Logger LOG = LoggerFactory.getLogger(EksternApiConfig.class);
    public static final String API_URI = "/ekstern/api";

    public EksternApiConfig() {
        LOG.info("Initialiserer: {}", API_URI);
        setApplicationName(EksternApiConfig.class.getSimpleName());
        // Sikkerhet
        register(AuthenticationFilter.class);

        // REST
        registerClasses(getEksternalApplicationClasses());

        registerExceptionMappers();
        register(JacksonJsonConfig.class);

        setProperties(getApplicationProperties());
        LOG.info("Ferdig med initialisering av {}", API_URI);
    }

    void registerExceptionMappers() {
        register(GeneralRestExceptionMapper.class);
        register(ConstraintViolationMapper.class);
        register(JsonMappingExceptionMapper.class);
        register(JsonParseExceptionMapper.class);
    }

    private Set<Class<?>> getEksternalApplicationClasses() {
        // eksponert grensesnitt
        return Set.of(EksternDelingYtelseInfoRestTjeneste.class, EksternPipRestTjeneste.class);
    }

    private Map<String, Object> getApplicationProperties() {
        Map<String, Object> properties = new HashMap<>();
        // Ref Jersey doc
        properties.put(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
        properties.put(ServerProperties.PROCESSING_RESPONSE_ERRORS_ENABLED, true);
        return properties;
    }

}

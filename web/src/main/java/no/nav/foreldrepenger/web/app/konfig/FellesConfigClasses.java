package no.nav.foreldrepenger.web.app.konfig;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import no.nav.foreldrepenger.web.app.exceptions.ConstraintViolationMapper;
import no.nav.foreldrepenger.web.app.exceptions.GeneralRestExceptionMapper;
import no.nav.foreldrepenger.web.app.exceptions.JsonMappingExceptionMapper;
import no.nav.foreldrepenger.web.app.exceptions.JsonParseExceptionMapper;
import no.nav.foreldrepenger.web.app.jackson.JacksonJsonConfig;

public class FellesConfigClasses {

    private FellesConfigClasses() {
    }
    public static Set<Class<?>> getFellesConfigClasses() {
        Set<Class<?>> classes = new HashSet<>();
        // Autentisering
        classes.add(AuthenticationFilter.class);
        // swagger
        classes.add(OpenApiResource.class);

        // (De)Serialisering
        classes.add(JacksonJsonConfig.class);

        // ExceptionMappers pga de som finnes i Jackson+Jersey-media
        classes.add(ConstraintViolationMapper.class);
        classes.add(JsonMappingExceptionMapper.class);
        classes.add(JsonParseExceptionMapper.class);

        // Generell exceptionmapper m/logging for øvrige tilfelle
        classes.add(GeneralRestExceptionMapper.class);
        return Collections.unmodifiableSet(classes);
    }
}

package no.nav.foreldrepenger.web.app.konfig;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;

public class RestApiTester {

    static final List<Class<?>> UNNTATT = Collections.singletonList(OpenApiResource.class);

    static Collection<Method> finnAlleRestMetoder() {
        List<Method> liste = new ArrayList<>();
        for (var klasse : finnAlleRestTjenester()) {
            for (var method : klasse.getDeclaredMethods()) {
                if (Modifier.isPublic(method.getModifiers()) && hasHttpMethodAnnotation(method)) {
                    liste.add(method);
                }
            }
        }
        return liste;
    }

    static Collection<Class<?>> finnAlleRestTjenester() {
        return new ArrayList<>(finnAlleRestTjenester(new ApiConfig()));
    }

    static Collection<Class<?>> finnAlleRestTjenester(Application config) {
        return config.getClasses().stream().filter(c -> c.getAnnotation(Path.class) != null).filter(c -> !UNNTATT.contains(c)).toList();
    }

    static boolean hasHttpMethodAnnotation(Method method) {
        return method.getAnnotation(Path.class) != null || method.getAnnotation(GET.class) != null || method.getAnnotation(POST.class) != null
            || method.getAnnotation(PUT.class) != null || method.getAnnotation(DELETE.class) != null;
    }
}

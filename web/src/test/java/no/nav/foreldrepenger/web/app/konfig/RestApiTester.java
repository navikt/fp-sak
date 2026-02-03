package no.nav.foreldrepenger.web.app.konfig;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;

import com.fasterxml.jackson.annotation.JsonSubTypes;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import no.nav.foreldrepenger.web.app.tjenester.RestImplementationClasses;

public class RestApiTester {

    static final List<Class<?>> UNNTATT = Collections.singletonList(OpenApiResource.class);

    static Collection<Method> finnAlleRestMetoder() {
        List<Method> liste = new ArrayList<>();
        for (var klasse : finnAlleRestTjenester()) {
            for (var method : klasse.getDeclaredMethods()) {
                if (Modifier.isPublic(method.getModifiers())) {
                    liste.add(method);
                }
            }
        }
        return liste;
    }

    static Collection<Class<?>> finnAlleJsonTypeNameClasses() {
        return RestImplementationClasses.allJsonTypeNameClasses();
    }

    static Collection<Class<?>> finnAlleJsonSubTypeClasses(Class<?> klasse) {
        var resultat = new ArrayList<Class<?>>();
        if (klasse.isAnnotationPresent(JsonSubTypes.class)) {
            var jsonSubTypes = klasse.getAnnotation(JsonSubTypes.class);
            for (var subtype : jsonSubTypes.value()) {
                resultat.add(subtype.value());
            }
        }
        return resultat;
    }

    private static Collection<Class<?>> finnAlleRestTjenester() {
        var resultat = new ArrayList<>(finnAlleRestTjenester(new ApiConfig()));
        resultat.addAll(finnAlleRestTjenester(new EksternApiConfig()));
        resultat.addAll(finnAlleRestTjenester(new ForvaltningApiConfig()));
        return resultat;
    }

    private static Collection<Class<?>> finnAlleRestTjenester(Application config) {
        return config.getClasses().stream().filter(c -> c.getAnnotation(Path.class) != null).filter(c -> !UNNTATT.contains(c)).toList();
    }
}

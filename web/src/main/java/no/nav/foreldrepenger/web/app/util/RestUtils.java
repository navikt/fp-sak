package no.nav.foreldrepenger.web.app.util;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.foreldrepenger.web.app.ApplicationConfig;
import no.nav.foreldrepenger.web.app.rest.ResourceLink;
import no.nav.foreldrepenger.web.app.tjenester.RestImplementationClasses;
import no.nav.foreldrepenger.web.server.jetty.JettyWebKonfigurasjon;

public class RestUtils {

    /**
     * If the class have the
     */
    public static String getClassAnnotationValue(Class<?> aClass, @SuppressWarnings("rawtypes") Class annotationClass, String name) {
        @SuppressWarnings("unchecked") var aClassAnnotation = aClass.getAnnotation(annotationClass);
        if (aClassAnnotation != null) {
            var type = aClassAnnotation.annotationType();
            for (var method : type.getDeclaredMethods()) {
                try {
                    var value = method.invoke(aClassAnnotation, new Object[0]);
                    if (method.getName().equals(name)) {
                        return value.toString();
                    }

                } catch (InvocationTargetException e) {

                } catch (IllegalAccessException e) {

                }
            }
        }
        return null;
    }

    public static String getApiPath() {
        var contextPath = JettyWebKonfigurasjon.CONTEXT_PATH;
        var apiUri = ApplicationConfig.API_URI;
        return contextPath + apiUri;
    }

    public static String getApiPath(String segment) {
        return getApiPath() + segment;
    }

    public static Collection<ResourceLink> getRoutes() {
        Set<ResourceLink> routes = new HashSet<>();
        var restClasses = RestImplementationClasses.getImplementationClasses();
        for (var aClass : restClasses) {
            var pathFromClass = getClassAnnotationValue(aClass, Path.class, "value");
            var methods = aClass.getMethods();
            for (var aMethod : methods) {
                ResourceLink.HttpMethod method = null;
                if (aMethod.getAnnotation(POST.class) != null) {
                    method = ResourceLink.HttpMethod.POST;
                }
                if (aMethod.getAnnotation(GET.class) != null) {
                    method = ResourceLink.HttpMethod.GET;
                }
                if (aMethod.getAnnotation(PUT.class) != null) {
                    method = ResourceLink.HttpMethod.PUT;
                }
                if (aMethod.getAnnotation(DELETE.class) != null) {
                    method = ResourceLink.HttpMethod.DELETE;
                }
                if (method != null) {
                    var pathFromMethod = "";
                    if (aMethod.getAnnotation(Path.class) != null) {
                        pathFromMethod = aMethod.getAnnotation(Path.class).value();
                    }
                    var resourceLink = new ResourceLink(getApiPath() + pathFromClass + pathFromMethod, aMethod.getName(), method);
                    routes.add(resourceLink);
                }
            }
        }
        return routes;
    }

    public static String convertObjectToQueryString(Object object) {
        var mapper = new ObjectMapper();
        return mapper.convertValue(object, UriFormat.class).toString();
    }

    static class UriFormat {

        private StringBuilder builder = new StringBuilder();

        @JsonAnySetter
        public void addToUri(String name, Object property) {
            if (builder.length() > 0) {
                builder.append("&");
            }
            builder.append(name).append("=").append(property);
        }

        @Override
        public String toString() {
            return builder.toString();
        }
    }

}


package no.nav.foreldrepenger.web.app.konfig;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import no.nav.vedtak.felles.integrasjon.felles.ws.SoapWebService;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

class SoapApiTester {

    static Collection<Method> finnAlleSoapMetoder() {
        List<Method> liste = new ArrayList<>();
        for (var klasse : finnAlleSoapTjenester()) {
            for (var method : klasse.getDeclaredMethods()) {
                if (method.getAnnotation(BeskyttetRessurs.class) == null && Modifier.isPublic(method.getModifiers())
                        && !method.getName().equals("ping")) {
                    liste.add(method);
                }
            }
        }
        return liste;
    }

    private static List<Class<?>> getAllClasses() {
        var reflections = new Reflections("no.nav.foreldrepenger", new SubTypesScanner(false));
        var classes = reflections.getSubTypesOf(Object.class);
        return new ArrayList<>(classes);
    }

    static List<Class<?>> finnAlleSoapTjenester() {
        var classes = getAllClasses();
        List<Class<?>> classesToReturn = new ArrayList<Class<?>>();

        classes.stream().filter(s -> s.getAnnotation(SoapWebService.class) != null).forEach(classesToReturn::add);
        return classesToReturn;
    }
}

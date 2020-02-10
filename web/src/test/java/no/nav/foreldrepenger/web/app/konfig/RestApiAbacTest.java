package no.nav.foreldrepenger.web.app.konfig;

import static org.assertj.core.api.Fail.fail;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

import no.nav.vedtak.sikkerhet.abac.AbacDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

/**
 * Sjekker at alle REST endepunkt har definert tilgangskontroll konfigurert for ABAC (Attribute Based Access Control).
 */
public class RestApiAbacTest {

    @Test
    public void test_at_alle_restmetoder_er_annotert_med_BeskyttetRessurs() throws Exception {
        for (Method restMethod : RestApiTester.finnAlleRestMetoder()) {
            if (restMethod.getAnnotation(BeskyttetRessurs.class) == null) {
                throw new AssertionError("Mangler @" + BeskyttetRessurs.class.getSimpleName() + "-annotering på " + restMethod);
            }
        }
    }

    @Test
    public void sjekk_at_ingen_metoder_er_annotert_med_dummy_verdier() {
        for (Method metode : RestApiTester.finnAlleRestMetoder()) {
            assertAtIngenBrukerDummyVerdierPåBeskyttetRessurs(metode);
        }
    }

    /**
     * IKKE ignorer denne testen, helper til med at input til tilgangskontroll blir riktig
     * <p>
     * Kontakt Team Humle hvis du trenger hjelp til å endre koden din slik at den går igjennom her     *
     */
    @Test
    public void test_at_alle_input_parametre_til_restmetoder_implementer_AbacDto_eller_spesifiserer_AbacDataSupplier() throws Exception {
        String feilmelding = "Parameter på %s.%s av type %s må implementere " + AbacDto.class.getSimpleName() + ", eller være annotatert med @TilpassetAbacAttributt.\n";
        StringBuilder feilmeldinger = new StringBuilder();

        for (Method restMethode : RestApiTester.finnAlleRestMetoder()) {
            for (Parameter parameter : restMethode.getParameters()) {
                Class<?> parameterType = parameter.getType();
                var parameterAnnotations = restMethode.getParameterAnnotations();
                if (Collection.class.isAssignableFrom(parameterType)) {
                    ParameterizedType type = (ParameterizedType) parameter.getParameterizedType();
                    @SuppressWarnings("rawtypes")
                    Class<?> aClass = (Class) (type.getActualTypeArguments()[0]);
                    if (!harAbacKonfigurasjon(parameterAnnotations[0], aClass)) {
                        feilmeldinger.append(String.format(feilmelding, restMethode.getDeclaringClass().getSimpleName(), restMethode.getName(), aClass.getSimpleName()));
                    }
                } else {
                    if (!harAbacKonfigurasjon(parameterAnnotations[0], parameterType)) {
                        feilmeldinger.append(String.format(feilmelding, restMethode.getDeclaringClass().getSimpleName(), restMethode.getName(), parameterType.getSimpleName()));
                    }
                }
            }
        }
        if (feilmeldinger.length() > 0) {
            throw new AssertionError("Følgende inputparametre til REST-tjenester mangler AbacDto-impl\n" + feilmeldinger);
        }
    }

    private boolean harAbacKonfigurasjon(Annotation[] parameterAnnotations, Class<?> parameterType) {
        var ret = AbacDto.class.isAssignableFrom(parameterType) || IgnorerteInputTyper.ignore(parameterType);
        if(!ret) {
            ret = List.of(parameterAnnotations).stream().anyMatch(a -> TilpassetAbacAttributt.class.equals(a.annotationType()));
        }
        return ret;
    }

    private void assertAtIngenBrukerDummyVerdierPåBeskyttetRessurs(Method metode) {
        Class<?> klasse = metode.getDeclaringClass();
        BeskyttetRessurs annotation = metode.getAnnotation(BeskyttetRessurs.class);
        if (annotation != null && annotation.action() == BeskyttetRessursActionAttributt.DUMMY) {
            fail(klasse.getSimpleName() + "." + metode.getName() + " Ikke bruk DUMMY-verdi for "
                + BeskyttetRessursActionAttributt.class.getSimpleName());
        } else if (annotation != null && annotation.ressurs() == BeskyttetRessursResourceAttributt.DUMMY) {
            fail(klasse.getSimpleName() + "." + metode.getName() + " Ikke bruk DUMMY-verdi for "
                + BeskyttetRessursResourceAttributt.class.getSimpleName());
        }
    }

    /**
     * Disse typene slipper naturligvis krav om impl av {@link AbacDto}
     */
    enum IgnorerteInputTyper {
        BOOLEAN(Boolean.class.getName()),
        SERVLET(HttpServletRequest.class.getName());

        private String className;

        IgnorerteInputTyper(String className) {
            this.className = className;
        }

        static boolean ignore(Class<?> klasse) {
            return Arrays.stream(IgnorerteInputTyper.values()).anyMatch(e -> e.className.equals(klasse.getName()));
        }
    }

}

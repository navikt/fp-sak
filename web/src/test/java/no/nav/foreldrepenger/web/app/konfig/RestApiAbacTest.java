package no.nav.foreldrepenger.web.app.konfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;

import no.nav.vedtak.sikkerhet.abac.AbacDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

/**
 * Sjekker at alle REST endepunkt har definert tilgangskontroll konfigurert for
 * ABAC (Attribute Based Access Control).
 */
class RestApiAbacTest {

    @Test
    void test_at_alle_restmetoder_er_annotert_med_BeskyttetRessurs() {
        for (var restMethod : RestApiTester.finnAlleRestMetoder()) {
            assertThat(restMethod.getAnnotation(BeskyttetRessurs.class))
                .withFailMessage("Mangler @" + BeskyttetRessurs.class.getSimpleName() + "-annotering på " + restMethod)
                .isNotNull();
        }
    }

    @Test
    void sjekk_at_ingen_metoder_er_annotert_med_dummy_verdier() {
        for (var metode : RestApiTester.finnAlleRestMetoder()) {
            assertAtIngenBrukerDummyVerdierPåBeskyttetRessurs(metode);
        }
    }

    /**
     * IKKE ignorer denne testen, helper til med at input til tilgangskontroll blir
     * riktig
     * <p>
     * Kontakt Team Humle hvis du trenger hjelp til å endre koden din slik at den
     * går igjennom her *
     */
    @Test
    void test_at_alle_input_parametre_til_restmetoder_implementer_AbacDto_eller_spesifiserer_AbacDataSupplier() {
        var feilmelding = "Parameter på %s.%s av type %s må implementere " + AbacDto.class.getSimpleName()
                + ", eller være annotatert med @TilpassetAbacAttributt.\n";
        var feilmeldinger = new StringBuilder();

        for (var restMethode : RestApiTester.finnAlleRestMetoder()) {
            var i = 0;
            for (var parameter : restMethode.getParameters()) {
                var parameterType = parameter.getType();
                var parameterAnnotations = restMethode.getParameterAnnotations();
                if (Collection.class.isAssignableFrom(parameterType)) {
                    var type = (ParameterizedType) parameter.getParameterizedType();
                    @SuppressWarnings("rawtypes") Class<?> aClass = (Class) type.getActualTypeArguments()[0];
                    if (!harAbacKonfigurasjon(parameterAnnotations[0], aClass)) {
                        feilmeldinger.append(String.format(feilmelding, restMethode.getDeclaringClass().getSimpleName(), restMethode.getName(),
                                aClass.getSimpleName()));
                    }
                } else {
                    if (!harAbacKonfigurasjon(parameterAnnotations[i], parameterType)) {
                        feilmeldinger.append(String.format(feilmelding, restMethode.getDeclaringClass().getSimpleName(), restMethode.getName(),
                                parameterType.getSimpleName()));
                    }
                }
                i++;
            }
        }
        assertThat(feilmeldinger.length())
            .withFailMessage("Følgende inputparametre til REST-tjenester mangler AbacDto-impl\n" + feilmeldinger)
            .isNotPositive();
    }

    private boolean harAbacKonfigurasjon(Annotation[] parameterAnnotations, Class<?> parameterType) {
        var ret = AbacDto.class.isAssignableFrom(parameterType) || IgnorerteInputTyper.ignore(parameterType);
        if (!ret) {
            ret = Stream.of(parameterAnnotations).anyMatch(a -> TilpassetAbacAttributt.class.equals(a.annotationType()));
        }
        return ret;
    }

    private void assertAtIngenBrukerDummyVerdierPåBeskyttetRessurs(Method metode) {
        var klasse = metode.getDeclaringClass();
        var annotation = metode.getAnnotation(BeskyttetRessurs.class);
        if (annotation != null && annotation.actionType() == ActionType.DUMMY) {
            fail(klasse.getSimpleName() + "." + metode.getName() + " Ikke bruk DUMMY-verdi for " + ActionType.class.getSimpleName());
        } else if (annotation != null && annotation.resourceType() == ResourceType.DUMMY) {
            fail(klasse.getSimpleName() + "." + metode.getName() + " En verdi for resource må være satt!");
        }
    }

    /**
     * Disse typene slipper naturligvis krav om impl av {@link AbacDto}
     */
    enum IgnorerteInputTyper {
        BOOLEAN(Boolean.class.getName()),
        SERVLET(HttpServletRequest.class.getName());

        private final String className;

        IgnorerteInputTyper(String className) {
            this.className = className;
        }

        static boolean ignore(Class<?> klasse) {
            return Arrays.stream(IgnorerteInputTyper.values()).anyMatch(e -> e.className.equals(klasse.getName()));
        }
    }

}

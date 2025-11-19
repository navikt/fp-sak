package no.nav.foreldrepenger.web.app.konfig;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import jakarta.validation.Valid;
import jakarta.ws.rs.core.Context;

import org.junit.jupiter.api.Test;

class RestApiOppdragInputValideringAnnoteringTest extends RestApiTester {

    private Function<Method, String> printKlasseOgMetodeNavn = method -> String.format("%s.%s", method.getDeclaringClass(), method.getName());

    /**
     * IKKE ignorer eller fjern denne testen, den sørger for at inputvalidering er i
     * orden for REST-grensesnittene
     * <p>
     * Kontakt Team Humle hvis du trenger hjelp til å endre koden din slik at den
     * går igjennom her
     */
    @Test
    void alle_felter_i_objekter_som_brukes_som_inputDTO_skal_enten_ha_valideringsannotering_eller_være_av_godkjent_type() {
        for (var method : finnAlleRestMetoder()) {
            for (var i = 0; i < method.getParameterCount(); i++) {
                assertThat(method.getParameterTypes()[i].isAssignableFrom(String.class)).as(
                        "REST-metoder skal ikke har parameter som er String eller mer generelt. Bruk DTO-er og valider. "
                                + printKlasseOgMetodeNavn.apply(method))
                        .isFalse();
                if (isCollectionOrMap(method.getParameterTypes()[i])) {
                    for (var param : method.getParameters()) {
                        if (param.getAnnotatedType().isAnnotationPresent(Valid.class)) {
                            throw new AssertionError("Flytt annotering @Valid inn i List/Set/Collection/Map for feltet " + param + ".");
                        }
                        if (param.getAnnotatedType() instanceof AnnotatedParameterizedType annotatedParameterizedType) {
                            var annotert = annotatedParameterizedType.getAnnotatedActualTypeArguments();
                            for (var ann : annotert) {
                                assertThat(ann.isAnnotationPresent(Valid.class)).as(
                                    "Alle parameter for REST-metoder skal være annotert med @Valid. Var ikke det for " +
                                        printKlasseOgMetodeNavn.apply(method)).withFailMessage("Fant parametere som mangler @Valid annotation").isTrue();
                            }
                        }
                    }
                } else {
                    assertThat(isRequiredAnnotationPresent(method.getParameters()[i])).as(
                            "Alle parameter for REST-metoder skal være annotert med @Valid. Var ikke det for " + printKlasseOgMetodeNavn.apply(method))
                        .withFailMessage("Fant parametere som mangler @Valid annotation '" + method.getParameters()[i].toString() + "'")
                        .isTrue();
                }
            }
        }
    }

    private static boolean isCollectionOrMap(Class<?> klasse) {
        return Collection.class.isAssignableFrom(klasse) || Map.class.isAssignableFrom(klasse);
    }

    private boolean isRequiredAnnotationPresent(Parameter parameter) {
        var validAnnotation = parameter.getAnnotation(Valid.class);
        if (validAnnotation == null) {
            var contextAnnotation = parameter.getAnnotation(Context.class);
            return contextAnnotation != null;
        }
        return true;
    }

}

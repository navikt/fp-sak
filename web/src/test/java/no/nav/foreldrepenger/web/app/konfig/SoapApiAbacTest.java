package no.nav.foreldrepenger.web.app.konfig;

import java.lang.reflect.Method;
import java.util.Collection;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

class SoapApiAbacTest {

    private static Collection<Method> soapMethods;

    @BeforeAll
    public static void init() {
        soapMethods = SoapApiTester.finnAlleSoapMetoder();
    }

    /**
     * IKKE ignorer denne testen, sikrer at SOAP-endepunkter får tilgangskontroll
     * <p>
     * Kontakt Team Humle hvis du trenger hjelp til å endre koden din slik at den
     * går igjennom her *
     */
    @Test
    void test_at_alle_soapmetoder_er_annotert_med_BeskyttetRessurs() {
        for (var soapMethod : SoapApiTester.finnAlleSoapMetoder()) {
            if (soapMethod.getAnnotation(BeskyttetRessurs.class) == null) {
                throw new AssertionError("Mangler @" + BeskyttetRessurs.class.getSimpleName() + "-annotering på " + soapMethod);
            }
        }
    }

    /**
     * IKKE ignorer denne testen, helper til med at input til tilgangskontroll blir
     * riktig
     * <p>
     * Kontakt Team Humle hvis du trenger hjelp til å endre koden din slik at den
     * går igjennom her
     */
    @Test
    void test_at_alle_input_parametre_til_soapmetoder_er_annotert_med_TilpassetAbacAttributt() {
        var feilmelding = "Parameter type %s på metode %s.%s må ha annotering " + TilpassetAbacAttributt.class.getSimpleName() + ".\n";
        var feilmeldinger = new StringBuilder();
        for (var soapMethod : soapMethods) {
            for (var parameter : soapMethod.getParameters()) {
                if (parameter.getAnnotation(TilpassetAbacAttributt.class) == null) {
                    feilmeldinger.append(String.format(feilmelding, parameter.getType().getSimpleName(),
                            soapMethod.getDeclaringClass().getSimpleName(), soapMethod.getName()));
                }
            }
        }

        if (feilmeldinger.length() > 0) {
            throw new AssertionError("Følgende inputparametre til SOAP-tjenester passerte ikke validering\n" + feilmeldinger);
        }

    }

}

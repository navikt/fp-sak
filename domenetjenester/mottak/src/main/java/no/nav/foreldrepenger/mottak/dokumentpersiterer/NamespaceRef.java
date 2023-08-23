package no.nav.foreldrepenger.mottak.dokumentpersiterer;

import jakarta.enterprise.inject.Stereotype;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

import java.lang.annotation.*;

/**
 * Annotasjon for å merke klasser som brukes for oversetting av søknader.
 */
@Qualifier
@Stereotype
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
public @interface NamespaceRef {

    /**
     * namespace av dokumentet
     * */
    String value();

    /** AnnotationLiteral som kan brukes ved CDI søk. */
    class NamespaceRefLiteral extends AnnotationLiteral<NamespaceRef> implements NamespaceRef {

        private String value;

        public NamespaceRefLiteral(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }

    }

}

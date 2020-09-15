package no.nav.foreldrepenger.økonomi.økonomistøtte.queue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

/**
 * se {@link ØkonomiImplementasjonVelger}
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
public @interface TestOnlyMqDisabled {
    class TestOnlyMqDisabledLiteral extends AnnotationLiteral<TestOnlyMqDisabled> implements TestOnlyMqDisabled {
    }
}

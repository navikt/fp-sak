package no.nav.foreldrepenger.behandlingslager.diff;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** Ignorer alltid et felt for forskjeller ved traversing av entity graph. (se TraverseEntityGraph).*/
@Target(FIELD)
@Retention(RUNTIME)
public @interface DiffIgnore {

}

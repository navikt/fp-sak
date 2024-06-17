package no.nav.foreldrepenger.dbstoette;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
@ExtendWith(MockitoExtension.class)
public @interface CdiDbAwareTest {

}

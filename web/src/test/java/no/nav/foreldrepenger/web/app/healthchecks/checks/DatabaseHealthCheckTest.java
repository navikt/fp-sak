package no.nav.foreldrepenger.web.app.healthchecks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class DatabaseHealthCheckTest {


    @Test
    public void test_working_query() {
        assertThat(new DatabaseHealthCheck().isOK()).isTrue();
    }


}

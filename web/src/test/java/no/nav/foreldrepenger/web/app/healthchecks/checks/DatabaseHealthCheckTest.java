package no.nav.foreldrepenger.web.app.healthchecks.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.web.app.healthchecks.Selftests;
import no.nav.foreldrepenger.web.app.jackson.HealthCheckRestService;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class DatabaseHealthCheckTest {


    @Test
    public void test_working_query() {
        assertThat(new DatabaseHealthCheck().isOK()).isTrue();
    }


}

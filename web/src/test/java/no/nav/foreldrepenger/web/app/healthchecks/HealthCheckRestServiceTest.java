package no.nav.foreldrepenger.web.app.healthchecks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.web.app.jackson.HealthCheckRestService;

@ExtendWith(MockitoExtension.class)
public class HealthCheckRestServiceTest {

    private HealthCheckRestService restTjeneste;

    @Mock
    private Selftests selftests;

    @BeforeEach
    public void setup() {
        restTjeneste = new HealthCheckRestService(selftests);
    }

    @Test
    public void test_isAlive_skal_returnere_status_200() {

        when(selftests.isKafkaAlive()).thenReturn(true);

        restTjeneste.setIsContextStartupReady(true);
        var response = restTjeneste.isAlive();

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }

    @Test
    public void test_isReady_skal_returnere_service_unavailable_når_kritiske_selftester_feiler() {
        when(selftests.isReady()).thenReturn(false);

        restTjeneste.setIsContextStartupReady(true);
        var responseReady = restTjeneste.isReady();

        restTjeneste.setIsContextStartupReady(false);
        var responseAlive = restTjeneste.isAlive();

        assertThat(responseReady.getStatus()).isEqualTo(Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
        assertThat(responseAlive.getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void test_isReady_skal_returnere_status_delvis_når_db_feiler() {
        when(selftests.isReady()).thenReturn(false);
        when(selftests.isKafkaAlive()).thenReturn(true);

        restTjeneste.setIsContextStartupReady(true);
        var responseReady = restTjeneste.isReady();
        var responseAlive = restTjeneste.isAlive();

        assertThat(responseReady.getStatus()).isEqualTo(Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
        assertThat(responseAlive.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }

    @Test
    public void test_isReady_skal_returnere_status_ok_når_selftester_er_ok() {
        when(selftests.isReady()).thenReturn(true);
        when(selftests.isKafkaAlive()).thenReturn(true);

        restTjeneste.setIsContextStartupReady(true);
        var responseReady = restTjeneste.isReady();
        var responseAlive = restTjeneste.isAlive();

        assertThat(responseReady.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(responseAlive.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }

}

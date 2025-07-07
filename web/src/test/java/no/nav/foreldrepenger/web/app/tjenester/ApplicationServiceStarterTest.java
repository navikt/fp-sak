package no.nav.foreldrepenger.web.app.tjenester;

import static org.mockito.Mockito.verify;

import no.nav.vedtak.server.Controllable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class ApplicationServiceStarterTest {

    private ApplicationServiceStarter serviceStarter;

    @Mock
    private Controllable service;

    @BeforeEach
    void setup() {
        serviceStarter = new ApplicationServiceStarter(service);
    }

    @Test
    void test_skal_kalle_Controllable_start_og_stop() {
        serviceStarter.startServices();
        serviceStarter.stopServices();
        verify(service).start();
        verify(service).stop();
    }
}

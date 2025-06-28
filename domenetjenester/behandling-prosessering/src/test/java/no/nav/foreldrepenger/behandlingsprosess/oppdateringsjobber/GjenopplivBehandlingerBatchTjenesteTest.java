package no.nav.foreldrepenger.behandlingsprosess.oppdateringsjobber;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingsprosess.dagligejobber.gjenopptak.AutomatiskGjenopptagelseTjeneste;

class GjenopplivBehandlingerBatchTjenesteTest {

    private GjenopplivBehandlingerBatchTjeneste batchTjeneste; // objektet vi tester

    private AutomatiskGjenopptagelseTjeneste mockTjeneste;

    private static final String BATCHNAME = GjenopplivBehandlingerBatchTjeneste.BATCHNAME;


    @BeforeEach
    void setup() {
        mockTjeneste = mock(AutomatiskGjenopptagelseTjeneste.class);
        batchTjeneste = new GjenopplivBehandlingerBatchTjeneste(mockTjeneste);
    }

    @Test
    void skal_gi_respons() {
        // Arrange
        var response = "-1";
        when(mockTjeneste.gjenopplivBehandlinger()).thenReturn(response);

        // Act
        var batchStatus = batchTjeneste.launch(new Properties());

        // Assert
        assertThat(batchStatus).isEqualTo(BATCHNAME + response);
    }
}

package no.nav.foreldrepenger.behandlingsprosess.dagligejobber.gjenopptak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AutomatiskGjenopptagelseBatchTjenesteTest {

    private AutomatiskGjenopptagelseBatchTjeneste batchTjeneste; // objektet vi tester

    private AutomatiskGjenopptagelseTjeneste mockTjeneste;

    private static final String BATCHNAME = AutomatiskGjenopptagelseBatchTjeneste.BATCHNAME;

    @BeforeEach
    void setup() {
        mockTjeneste = mock(AutomatiskGjenopptagelseTjeneste.class);
        batchTjeneste = new AutomatiskGjenopptagelseBatchTjeneste(mockTjeneste);
    }

    @Test
    void skal_gi_respons() {
        // Arrange
        var response = "-1";
        when(mockTjeneste.gjenopptaBehandlinger()).thenReturn(response);

        // Act
        var batchStatus = batchTjeneste.launch(new Properties());

        // Assert
        assertThat(batchStatus).isEqualTo(BATCHNAME + response);
    }
}

package no.nav.foreldrepenger.behandlingsprosess.dagligejobber.gjenopptak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.batch.BatchArguments;
import no.nav.foreldrepenger.batch.EmptyBatchArguments;

public class AutomatiskRegisterOppdateringBatchTjenesteTest {

    private AutomatiskRegisterOppdateringBatchTjeneste batchTjeneste; // objektet vi tester

    private AutomatiskGjenopptagelseTjeneste mockTjeneste;

    private static final String BATCHNAME = AutomatiskRegisterOppdateringBatchTjeneste.BATCHNAME;
    private static final BatchArguments ARGUMENTS = new EmptyBatchArguments(Map.of());


    @BeforeEach
    public void setup() {
        mockTjeneste = mock(AutomatiskGjenopptagelseTjeneste.class);
        batchTjeneste = new AutomatiskRegisterOppdateringBatchTjeneste(mockTjeneste);
    }

    @Test
    public void skal_gi_respons() {
        // Arrange
        var response = "-1";
        when(mockTjeneste.oppdaterBehandlingerFraOppgaveFrist()).thenReturn(response);

        // Act
        var batchStatus = batchTjeneste.launch(ARGUMENTS);

        // Assert
        assertThat(batchStatus).isEqualTo(BATCHNAME + response);
    }
}

package no.nav.foreldrepenger.økonomistøtte.simulering.tjeneste;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.økonomistøtte.simulering.klient.FpOppdragRestKlient;
import no.nav.vedtak.exception.TekniskException;

public class SimuleringIntegrasjonTjenesteTest {

    private static final Long BEHANDLING_ID = 1234L;

    private SimuleringIntegrasjonTjeneste integrasjonTjeneste; // tjenesten som testes

    private final FpOppdragRestKlient restKlientMock = mock(FpOppdragRestKlient.class);

    @BeforeEach
    void setup() {
        integrasjonTjeneste = new SimuleringIntegrasjonTjeneste(restKlientMock);
    }

    @Test
    public void test_skalFeileVedBehandlingIdNull() {
        assertThrows(NullPointerException.class, () -> integrasjonTjeneste.startSimulering(null, null));
    }

    @Test
    public void test_skalFeileVedOppdraglisteNull() {
        assertThrows(NullPointerException.class, () -> integrasjonTjeneste.startSimulering(BEHANDLING_ID, null));
    }

    @Test
    public void test_skalSendeRequestTilRestKlient() {
        integrasjonTjeneste.startSimulering(BEHANDLING_ID, Collections.singletonList("test"));
        verify(restKlientMock, atLeastOnce()).startSimulering(any());
    }

    @Test
    public void test_skalIkkeStartSimuleringNårOppdraglisteEmpty() {
        integrasjonTjeneste.startSimulering(BEHANDLING_ID, Collections.emptyList());
        verify(restKlientMock, never()).startSimulering(any());
    }

    @Test
    public void test_skalFeileNårOppdragsystemKasterException() {
        doThrow(SimulerOppdragIntegrasjonTjenesteFeil.startSimuleringFeiletMedFeilmelding(BEHANDLING_ID, new RuntimeException()))
            .when(restKlientMock).startSimulering(any());
        assertThatThrownBy(() -> integrasjonTjeneste.startSimulering(BEHANDLING_ID, Collections.singletonList("test")))
            .isInstanceOf(TekniskException.class)
            .hasMessageContaining("FP-423523");
    }
}

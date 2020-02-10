package no.nav.foreldrepenger.økonomi.simulering.tjeneste;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Collections;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import no.nav.foreldrepenger.økonomi.simulering.klient.FpOppdragRestKlient;
import no.nav.vedtak.exception.TekniskException;

public class SimuleringIntegrasjonTjenesteTest {

    private static final Long BEHANDLING_ID = 1234L;

    private SimuleringIntegrasjonTjeneste integrasjonTjeneste; // tjenesten som testes

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private FpOppdragRestKlient restKlientMock = mock(FpOppdragRestKlient.class);

    @Before
    public void setup() {
        integrasjonTjeneste = new SimuleringIntegrasjonTjeneste(restKlientMock);
    }

    @Test
    public void test_skalFeileVedBehandlingIdNull() {
        expectedException.expect(NullPointerException.class);

        integrasjonTjeneste.startSimulering(null, null);
    }

    @Test
    public void test_skalFeileVedOppdraglisteNull() {
        expectedException.expect(NullPointerException.class);

        integrasjonTjeneste.startSimulering(BEHANDLING_ID, null);
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
        doThrow(SimulerOppdragIntegrasjonTjenesteFeil.FACTORY.startSimuleringFeiletMedFeilmelding(BEHANDLING_ID, new RuntimeException()).toException())
            .when(restKlientMock).startSimulering(any());
        expectedException.expect(TekniskException.class);
        expectedException.expectMessage("FP-423523");
        integrasjonTjeneste.startSimulering(BEHANDLING_ID, Collections.singletonList("test"));
    }
}

package no.nav.foreldrepenger.økonomistøtte.simulering.tjeneste;

import static no.nav.foreldrepenger.økonomistøtte.OppdragTestDataHelper.oppdragskontrollUtenOppdrag;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomistøtte.OppdragTestDataHelper;
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
    void test_skalFeileVedBehandlingIdNull() {
        var oppdragskontroll = mock(Oppdragskontroll.class);
        when(oppdragskontroll.getBehandlingId()).thenReturn(null);
        when(oppdragskontroll.getOppdrag110Liste()).thenReturn(List.of());
        assertThrows(NullPointerException.class, () -> integrasjonTjeneste.startSimulering(Optional.of(oppdragskontroll)));
    }

    @Test
    void test_skalFeileVedOppdraglisteNull() {
        var oppdragskontroll = mock(Oppdragskontroll.class);
        when(oppdragskontroll.getBehandlingId()).thenReturn(BEHANDLING_ID);
        when(oppdragskontroll.getOppdrag110Liste()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> integrasjonTjeneste.startSimulering(Optional.of(oppdragskontroll)));
    }

    @Test
    void test_skalSendeRequestTilRestKlientNårOppdragskontrollHarBådeBehandlingsIdOgOppdrag() {
        var oppdragskontroll = OppdragTestDataHelper.oppdragskontrollMedOppdrag(new Saksnummer("123456"), BEHANDLING_ID);
        integrasjonTjeneste.startSimulering(Optional.of(oppdragskontroll));
        verify(restKlientMock, atLeastOnce()).startSimulering(any());
    }

    @Test
    void test_skalIkkeStartSimuleringNårOppdraglisteEmpty() {
        var oppdragskontroll = oppdragskontrollUtenOppdrag(new Saksnummer("123456"), 1L, 123L);
        integrasjonTjeneste.startSimulering(Optional.of(oppdragskontroll));
        verify(restKlientMock, never()).startSimulering(any());
    }

    @Test
    void test_skalFeileNårOppdragsystemKasterException() {
        doThrow(SimulerOppdragIntegrasjonTjenesteFeil.startSimuleringFeiletMedFeilmelding(BEHANDLING_ID, new RuntimeException()))
            .when(restKlientMock).startSimulering(any());
        assertThatThrownBy(() -> integrasjonTjeneste.startSimuleringOLD(BEHANDLING_ID, Collections.singletonList("test")))
            .isInstanceOf(TekniskException.class)
            .hasMessageContaining("FP-423523");
    }

    @Test
    void test_skalIkkeFeileNårFpWsProxyKasterException() {
        doThrow(SimulerOppdragIntegrasjonTjenesteFeil.startSimuleringFeiletMedFeilmelding(BEHANDLING_ID, new RuntimeException()))
            .when(restKlientMock).startSimuleringFpWsProxy(any());
        var oppdragskontroll = OppdragTestDataHelper.oppdragskontrollMedOppdrag(new Saksnummer("123456"), BEHANDLING_ID);
        assertThrows(TekniskException.class, () -> integrasjonTjeneste.startSimuleringViaFpWsProxy(any()));
        assertDoesNotThrow(() -> integrasjonTjeneste.startSimulering(Optional.of(oppdragskontroll)));

    }
}

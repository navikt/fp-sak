package no.nav.foreldrepenger.økonomistøtte.simulering.tjeneste;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomistøtte.simulering.klient.FpOppdragRestKlient;
import no.nav.foreldrepenger.økonomistøtte.simulering.klient.OppdragForventetNedetidException;
import no.nav.vedtak.exception.IntegrasjonException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static no.nav.foreldrepenger.økonomistøtte.OppdragTestDataHelper.oppdragskontrollMedOppdrag;
import static no.nav.foreldrepenger.økonomistøtte.OppdragTestDataHelper.oppdragskontrollUtenOppdrag;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SimuleringIntegrasjonTjenesteTest {

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
        var oppdragskontrollOpt = Optional.of(oppdragskontroll);
        assertThrows(NullPointerException.class, () -> integrasjonTjeneste.startSimulering(oppdragskontrollOpt));
    }

    @Test
    void test_skalFeileVedOppdraglisteNull() {
        var oppdragskontroll = mock(Oppdragskontroll.class);
        when(oppdragskontroll.getBehandlingId()).thenReturn(BEHANDLING_ID);
        when(oppdragskontroll.getOppdrag110Liste()).thenReturn(null);
        var oppdragskontrollOpt = Optional.of(oppdragskontroll);
        assertThrows(NullPointerException.class, () -> integrasjonTjeneste.startSimulering(oppdragskontrollOpt));
    }

    @Test
    void test_skalSendeRequestTilRestKlientNårOppdragskontrollHarBådeBehandlingsIdOgOppdrag() {
        var oppdragskontroll = oppdragskontrollMedOppdrag(new Saksnummer("123456"), BEHANDLING_ID);
        integrasjonTjeneste.startSimulering(Optional.of(oppdragskontroll));
        verify(restKlientMock, atLeastOnce()).startSimulering(any());
    }

    @Test
    void test_skalIkkeStartSimuleringNårOppdraglisteEmpty() {
        var oppdragskontroll = oppdragskontrollUtenOppdrag(new Saksnummer("123456"), BEHANDLING_ID, 123L);
        integrasjonTjeneste.startSimulering(Optional.of(oppdragskontroll));
        verify(restKlientMock, never()).startSimulering(any());
    }

    @Test
    void test_skalFeileNårOppdragsystemKasterExsception() {
        doThrow(new OppdragForventetNedetidException())
            .when(restKlientMock).startSimulering(any());
        var oppdragskontroll = oppdragskontrollMedOppdrag(new Saksnummer("123456"), BEHANDLING_ID);
        var oppdragskontrollOpt = Optional.of(oppdragskontroll);
        assertThatThrownBy(() -> integrasjonTjeneste.startSimulering(oppdragskontrollOpt))
            .isInstanceOf(IntegrasjonException.class)
            .hasMessageContaining(OppdragForventetNedetidException.MELDING);
        verify(restKlientMock, atLeastOnce()).startSimulering(any());
    }
}

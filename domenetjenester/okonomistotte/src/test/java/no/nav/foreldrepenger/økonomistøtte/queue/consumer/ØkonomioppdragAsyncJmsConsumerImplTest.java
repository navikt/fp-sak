package no.nav.foreldrepenger.økonomistøtte.queue.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.jms.JMSException;
import javax.jms.TextMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.Alvorlighetsgrad;
import no.nav.foreldrepenger.økonomistøtte.BehandleØkonomioppdragKvittering;
import no.nav.foreldrepenger.økonomistøtte.ØkonomiKvittering;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.integrasjon.jms.precond.DefaultDatabaseOppePreconditionChecker;

@ExtendWith(MockitoExtension.class)
public class ØkonomioppdragAsyncJmsConsumerImplTest {

    private static final long BEHANDLINGID = 802L;

    @Mock
    private BehandleØkonomioppdragKvittering behandleØkonomioppdragKvittering;

    private ArgumentCaptor<ØkonomiKvittering> captor;
    private ØkonomioppdragAsyncJmsConsumerImpl økonomioppdragAsyncJmsConsumerImpl;

    @BeforeEach
    public void setUp() throws Exception {
        final var mockDefaultDatabaseOppePreconditionChecker = mock(DefaultDatabaseOppePreconditionChecker.class);
        var jmsKonfig = new ØkonomioppdragJmsConsumerKonfig();
        økonomioppdragAsyncJmsConsumerImpl = new ØkonomioppdragAsyncJmsConsumerImpl(behandleØkonomioppdragKvittering, mockDefaultDatabaseOppePreconditionChecker, jmsKonfig);
        captor = ArgumentCaptor.forClass(ØkonomiKvittering.class);
    }

    @Test
    public void testHandleMessageWithUnparseableMessage() throws JMSException, IOException, URISyntaxException {
        // Arrange
        var message = opprettKvitteringXml("parsingFeil.xml");

        // Act
        assertThrows(TekniskException.class, () -> økonomioppdragAsyncJmsConsumerImpl.handle(message));
    }

    @Test
    public void testHandleMessageWithStatusOk() throws JMSException, IOException, URISyntaxException {
        // Arrange
        var message = opprettKvitteringXml("statusOk.xml");

        // Act
        økonomioppdragAsyncJmsConsumerImpl.handle(message);

        // Assert
        verify(behandleØkonomioppdragKvittering).behandleKvittering(captor.capture());
        var kvittering = captor.getValue();
        assertThat(kvittering).isNotNull();
        verifiserKvittering(kvittering, Alvorlighetsgrad.OK, null, BEHANDLINGID, "Oppdrag behandlet");
    }

    @Test
    public void testHandleMessageWithStatusFeil() throws JMSException, IOException, URISyntaxException {
        // Arrange
        var message = opprettKvitteringXml("statusFeil.xml");

        // Act
        økonomioppdragAsyncJmsConsumerImpl.handle(message);

        // Assert
        verify(behandleØkonomioppdragKvittering).behandleKvittering(captor.capture());
        var kvittering = captor.getValue();
        assertThat(kvittering).isNotNull();
        verifiserKvittering(kvittering, Alvorlighetsgrad.FEIL, "B110006F", 341L, "UTBET-FREKVENS har en ugyldig verdi: ENG");
    }

    private TextMessage opprettKvitteringXml(String filename) throws JMSException, IOException, URISyntaxException {
        var textMessage = mock(TextMessage.class);
        var xml = getInputXML("xml/" + filename);
        when(textMessage.getText()).thenReturn(xml);
        return textMessage;
    }

    private String getInputXML(String filename) throws IOException, URISyntaxException {
        var path = Paths.get(getClass().getClassLoader().getResource(filename).toURI());
        return Files.readString(path);
    }

    private void verifiserKvittering(ØkonomiKvittering kvittering, Alvorlighetsgrad alvorlighetsgrad, String meldingKode, Long behandlingId, String beskrMelding) {
        assertThat(kvittering.getAlvorlighetsgrad()).isEqualTo(alvorlighetsgrad);
        assertThat(kvittering.getMeldingKode()).isEqualTo(meldingKode);
        assertThat(kvittering.getBehandlingId()).isEqualTo(behandlingId);
        assertThat(kvittering.getBeskrMelding()).isEqualTo(beskrMelding);
    }
}

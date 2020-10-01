package no.nav.foreldrepenger.økonomi.økonomistøtte.queue.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.jms.JMSException;
import javax.jms.TextMessage;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.økonomi.økonomistøtte.BehandleØkonomioppdragKvittering;
import no.nav.foreldrepenger.økonomi.økonomistøtte.ØkonomiKvittering;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.integrasjon.jms.BaseJmsKonfig;
import no.nav.vedtak.felles.integrasjon.jms.precond.DefaultDatabaseOppePreconditionChecker;

public class ØkonomioppdragAsyncJmsConsumerImplTest {

    private static final long BEHANDLINGID = 802L;
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();
    @Mock
    private BehandleØkonomioppdragKvittering behandleØkonomioppdragKvittering;

    private ArgumentCaptor<ØkonomiKvittering> captor;
    private ØkonomioppdragAsyncJmsConsumerImpl økonomioppdragAsyncJmsConsumerImpl;

    @Before
    public void setUp() throws Exception {
        final DefaultDatabaseOppePreconditionChecker mockDefaultDatabaseOppePreconditionChecker = mock(DefaultDatabaseOppePreconditionChecker.class);
        final BaseJmsKonfig jmsKonfig = new BaseJmsKonfig("qu");
        jmsKonfig.setQueueName("asdf");
        jmsKonfig.setQueueManagerChannelName("asdf");
        jmsKonfig.setQueueManagerHostname("asdf");
        økonomioppdragAsyncJmsConsumerImpl = new ØkonomioppdragAsyncJmsConsumerImpl(behandleØkonomioppdragKvittering, mockDefaultDatabaseOppePreconditionChecker, jmsKonfig);
        captor = ArgumentCaptor.forClass(ØkonomiKvittering.class);
    }

    @Test(expected = TekniskException.class)
    public void testHandleMessageWithUnparseableMessage() throws JMSException, IOException, URISyntaxException {
        // Arrange
        TextMessage message = opprettKvitteringXml("parsingFeil.xml");

        // Act
        økonomioppdragAsyncJmsConsumerImpl.handle(message);
    }

    @Test
    public void testHandleMessageWithStatusOk() throws JMSException, IOException, URISyntaxException {
        // Arrange
        TextMessage message = opprettKvitteringXml("statusOk.xml");

        // Act
        økonomioppdragAsyncJmsConsumerImpl.handle(message);

        // Assert
        verify(behandleØkonomioppdragKvittering).behandleKvittering(captor.capture());
        ØkonomiKvittering kvittering = captor.getValue();
        assertThat(kvittering).isNotNull();
        verifiserKvittering(kvittering, "00", null, BEHANDLINGID, "Oppdrag behandlet");
    }

    @Test
    public void testHandleMessageWithStatusFeil() throws JMSException, IOException, URISyntaxException {
        // Arrange
        TextMessage message = opprettKvitteringXml("statusFeil.xml");

        // Act
        økonomioppdragAsyncJmsConsumerImpl.handle(message);

        // Assert
        verify(behandleØkonomioppdragKvittering).behandleKvittering(captor.capture());
        ØkonomiKvittering kvittering = captor.getValue();
        assertThat(kvittering).isNotNull();
        verifiserKvittering(kvittering, "08", "B110006F", 341L, "UTBET-FREKVENS har en ugyldig verdi: ENG");
    }

    private TextMessage opprettKvitteringXml(String filename) throws JMSException, IOException, URISyntaxException {
        TextMessage textMessage = mock(TextMessage.class);
        String xml = getInputXML("xml/" + filename);
        when(textMessage.getText()).thenReturn(xml);
        return textMessage;
    }

    private String getInputXML(String filename) throws IOException, URISyntaxException {
        Path path = Paths.get(getClass().getClassLoader().getResource(filename).toURI());
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private void verifiserKvittering(ØkonomiKvittering kvittering, String alvorlighetsgrad, String meldingKode, Long behandlingId, String beskrMelding) {
        assertThat(kvittering.getAlvorlighetsgrad()).isEqualTo(alvorlighetsgrad);
        assertThat(kvittering.getMeldingKode()).isEqualTo(meldingKode);
        assertThat(kvittering.getBehandlingId()).isEqualTo(behandlingId);
        assertThat(kvittering.getBeskrMelding()).isEqualTo(beskrMelding);
    }
}

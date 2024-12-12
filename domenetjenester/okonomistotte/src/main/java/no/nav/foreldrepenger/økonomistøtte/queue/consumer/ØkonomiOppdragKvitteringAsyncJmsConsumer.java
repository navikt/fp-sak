package no.nav.foreldrepenger.økonomistøtte.queue.consumer;

import javax.xml.stream.XMLStreamException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.UnmarshalException;

import no.nav.vedtak.server.Controllable;

import org.xml.sax.SAXException;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.Alvorlighetsgrad;
import no.nav.foreldrepenger.felles.jms.QueueConsumer;
import no.nav.foreldrepenger.felles.jms.precond.PreconditionChecker;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Mmel;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.Oppdrag;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.OppdragSkjemaConstants;
import no.nav.foreldrepenger.xmlutils.JaxbHelper;
import no.nav.foreldrepenger.økonomistøtte.BehandleØkonomioppdragKvittering;
import no.nav.foreldrepenger.økonomistøtte.queue.config.DatabasePreconditionChecker;
import no.nav.foreldrepenger.økonomistøtte.ØkonomiKvittering;
import no.nav.vedtak.exception.TekniskException;

@ApplicationScoped
public class ØkonomiOppdragKvitteringAsyncJmsConsumer extends QueueConsumer implements Controllable {

    private BehandleØkonomioppdragKvittering behandleØkonomioppdragKvittering;
    private DatabasePreconditionChecker preconditionChecker;

    ØkonomiOppdragKvitteringAsyncJmsConsumer() {
        // CDI
    }

    @Inject
    public ØkonomiOppdragKvitteringAsyncJmsConsumer(BehandleØkonomioppdragKvittering behandleØkonomioppdragKvittering,
                                                    DatabasePreconditionChecker preconditionChecker,
                                                    ØkonomioppdragJmsConsumerKonfig konfig) {
        super(konfig.getJmsKonfig());
        super.setConnectionFactory(konfig.getMqConnectionFactory());
        super.setQueue(konfig.getMqQueue());
        super.setToggleJms(new FellesJmsToggle());
        super.setMdcHandler(new QueueMdcLogHandler());
        this.behandleØkonomioppdragKvittering = behandleØkonomioppdragKvittering;
        this.preconditionChecker = preconditionChecker;
    }

    @Override
    public PreconditionChecker getPreconditionChecker() {
        return preconditionChecker;
    }

    @Override
    public void handle(Message message) throws JMSException {
        LOG.debug("Mottar melding");
        if (message instanceof TextMessage tm) {
            handle(tm.getText());
        } else {
            LOG.warn("Mottok en ikke støttet melding av klasse {}. Kø-elementet ble ignorert.", message.getClass());
        }
    }

    private void handle(String message) {
        try {
            var kvitteringsmelding = unmarshalOgKorriger(message);
            if (inneholderOppdragslinjer(kvitteringsmelding)) {
                var kvittering = fraKvitteringsmelding(kvitteringsmelding);
                behandleØkonomioppdragKvittering.behandleKvittering(kvittering);
            } else {
                loggKvitteringUtenLinjer(kvitteringsmelding);
            }
        } catch (SAXException | JAXBException e) {
            throw new TekniskException("FP-595437", "Uventet feil med JAXB ved parsing av melding "
                + "oppdragskjema.oppdrag: " + message, e);
        } catch (XMLStreamException e) {
            throw new TekniskException("FP-744861", "Feil i parsing av oppdragskjema.oppdrag", e);
        }
    }

    private void loggKvitteringUtenLinjer(Oppdrag kvitteringsmelding) {
        var oppdrag110 = kvitteringsmelding.getOppdrag110();
        var mmel = kvitteringsmelding.getMmel();
        var fagsystemId = oppdrag110.getFagsystemId();
        var fagområde = oppdrag110.getKodeFagomraade();
        var alvorlighetsgrad = mmel.getAlvorlighetsgrad();
        var beskrivendeMelding = mmel.getBeskrMelding();
        var kodeMelding = mmel.getKodeMelding();
        LOG.warn("""
            Mottok og ignorerte kvitteringsmelding uten oppdragslinjer,
            kan ikke direkte identifisere behandling. Gjelder fagsystemId={}.
            Innhold i kvittering: alvorlighetsgrad={} meldingKode='{}' beskrivendeMelding='{}', fagomraade={}.
            Skal følges opp manuelt
            """, fagsystemId, alvorlighetsgrad, kodeMelding, beskrivendeMelding, fagområde);
    }

    private boolean inneholderOppdragslinjer(Oppdrag kvitteringsmelding) {
        return !kvitteringsmelding.getOppdrag110().getOppdragsLinje150().isEmpty();
    }

    private Oppdrag unmarshalOgKorriger(String message) throws JAXBException, XMLStreamException, SAXException {
        Oppdrag kvitteringsmelding;
        try {
            kvitteringsmelding = JaxbHelper.unmarshalAndValidateXMLWithStAX(OppdragSkjemaConstants.JAXB_CLASS, message,
                OppdragSkjemaConstants.XSD_LOCATION);
        } catch (UnmarshalException e) {
            var editedMessage = message.replace("<oppdrag ", "<xml_1:oppdrag ")
                .replace("xmlns=", "xmlns:xml_1=")
                .replace("</oppdrag>", "</xml_1:oppdrag>")
                .replace("</ns2:oppdrag>", "</xml_1:oppdrag>");
            kvitteringsmelding = JaxbHelper.unmarshalAndValidateXMLWithStAX(OppdragSkjemaConstants.JAXB_CLASS,
                editedMessage, OppdragSkjemaConstants.XSD_LOCATION);
        }
        return kvitteringsmelding;
    }

    private ØkonomiKvittering fraKvitteringsmelding(Oppdrag melding) {
        var kvittering = new ØkonomiKvittering();
        fraMmel(kvittering, melding.getMmel(), melding.getOppdrag110().getFagsystemId());
        fraOppdragLinje150(kvittering, melding);
        return kvittering;
    }

    private void fraOppdragLinje150(ØkonomiKvittering kvittering, Oppdrag melding) {
        // trenger kun en tilfeldig oppdrag150 for å hente behandlingId
        var oppdragsLinje150 = melding.getOppdrag110().getOppdragsLinje150().get(0);
        kvittering.setBehandlingId(Long.valueOf(oppdragsLinje150.getHenvisning()));
    }

    private void fraMmel(ØkonomiKvittering kvittering, Mmel mmel, String fagsystemId) {
        kvittering.setAlvorlighetsgrad(Alvorlighetsgrad.fraKode(mmel.getAlvorlighetsgrad()));
        kvittering.setMeldingKode(mmel.getKodeMelding());
        kvittering.setBeskrMelding(mmel.getBeskrMelding());
        kvittering.setFagsystemId(Long.parseLong(fagsystemId));
    }

    @Override
    public void start() {
        if (!isDisabled()) {
            LOG.debug("Starter {}", ØkonomiOppdragKvitteringAsyncJmsConsumer.class.getSimpleName());
            super.start();
            LOG.info("Startet: {}", ØkonomiOppdragKvitteringAsyncJmsConsumer.class.getSimpleName());
        }
    }

    @Override
    public void stop() {
        if (!isDisabled()) {
            LOG.debug("Stoping {}", ØkonomiOppdragKvitteringAsyncJmsConsumer.class.getSimpleName());
            super.stop();
            LOG.info("Stoppet: {}", ØkonomiOppdragKvitteringAsyncJmsConsumer.class.getSimpleName());
        }
    }
}

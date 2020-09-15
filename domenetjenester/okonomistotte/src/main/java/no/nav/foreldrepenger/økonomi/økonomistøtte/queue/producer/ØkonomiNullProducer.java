package no.nav.foreldrepenger.økonomi.økonomistøtte.queue.producer;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import no.nav.foreldrepenger.økonomi.økonomistøtte.BehandleØkonomioppdragKvittering;
import no.nav.foreldrepenger.økonomi.økonomistøtte.queue.TestOnlyMqDisabled;
import no.nav.foreldrepenger.økonomi.økonomistøtte.ØkonomiKvittering;
import no.nav.vedtak.util.env.Environment;

@ApplicationScoped
@TestOnlyMqDisabled
public class ØkonomiNullProducer extends ØkonomioppdragJmsProducer {
    private static final Logger logger = LoggerFactory.getLogger(ØkonomiNullProducer.class);

    private BehandleØkonomioppdragKvittering behandleØkonomioppdragKvittering;

    public ØkonomiNullProducer() {

    }

    @Inject
    public ØkonomiNullProducer(BehandleØkonomioppdragKvittering behandleØkonomioppdragKvittering) {
        this.behandleØkonomioppdragKvittering = behandleØkonomioppdragKvittering;
    }

    @Override
    public void sendØkonomiOppdrag(String oppdragXML) {
        if (Environment.current().isProd()) {
            throw new IllegalStateException(ØkonomiNullProducer.class.getSimpleName() + " skal ikke brukes i prod");
        }
        logger.info("Sender økonomiOppdrag ut i intet");

        registrerKvittering(oppdragXML);

    }

    private void registrerKvittering(String oppdragXML) {
        logger.info("Skal registrerer kvittering for økonomiOppdrag.");
        try {
            Document oppdragDocument = getDocument(oppdragXML);
            NodeList oppdrag110Noder = getNodes("/oppdrag/oppdrag-110", oppdragDocument, "oppdrag-110");
            Node oppdrag110Node = oppdrag110Noder.item(0);

            String fagSystemId = hentVerdi(oppdrag110Node, "fagsystemId");

            NodeList oppdragsLinjer = getNodes("oppdrags-linje-150", oppdrag110Node);
            Node oppdragsLinje150Node = oppdragsLinjer.item(0);
            String henvisning = hentVerdi(oppdragsLinje150Node, "henvisning");

            ØkonomiKvittering kvittering = new ØkonomiKvittering();
            kvittering.setFagsystemId(Long.parseLong(fagSystemId));
            kvittering.setBehandlingId(Long.parseLong(henvisning));
            kvittering.setAlvorlighetsgrad("00");

            logger.info("Registrerer kvittering for økonomiOppdrag for fagsystemId {} og henvisning {}.", fagSystemId, henvisning);
            behandleØkonomioppdragKvittering.behandleKvittering(kvittering);
            logger.info("Kvittering registrert for økonomiOppdrag for fagsystemId {} og henvisning {}.", fagSystemId, henvisning);
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new IllegalStateException("Exception under registrering av kvittering på sendt oppdrag", e);
        }
    }

    @Override
    public void testConnection() {

    }

    @Override
    public String getConnectionEndpoint() {
        return "dummy(" + ØkonomiNullProducer.class.getName() + ")";
    }

    private Document getDocument(String xml) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        return documentBuilder.parse(new ByteArrayInputStream(xml.getBytes()));
    }

    private NodeList getNodes(String nodePath, Document kilde, String typeNode) {
        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            return (NodeList) xPath.evaluate(nodePath, kilde, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new IllegalStateException("Fikk exception under henting av " + typeNode + " fra generert oppdrags-xml", e);
        }
    }

    private NodeList getNodes(String nodePath, Node parentNode) {
        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            return (NodeList) xPath.evaluate(nodePath, parentNode, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new IllegalStateException("Fikk exception under henting av " + nodePath, e);
        }
    }

    private String hentVerdi(Node node, String feltNavn) {
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            return xPath.evaluate(feltNavn, node);
        } catch (XPathExpressionException e) {
            throw new IllegalStateException("Fikk exception under henting av verdi for <" + feltNavn + "> fra", e);
        }
    }

}

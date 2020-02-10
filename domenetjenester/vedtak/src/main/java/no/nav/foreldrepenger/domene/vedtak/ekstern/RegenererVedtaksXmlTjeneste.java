package no.nav.foreldrepenger.domene.vedtak.ekstern;

import static no.nav.vedtak.felles.xml.XmlUtils.retrieveNameSpaceOfXML;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.lagretvedtak.LagretVedtak;
import no.nav.foreldrepenger.domene.vedtak.repo.LagretVedtakRepository;
import no.nav.foreldrepenger.domene.vedtak.xml.FatteVedtakXmlTjeneste;
import no.nav.foreldrepenger.vedtak.v1.ForeldrepengerVedtakConstants;
import no.nav.foreldrepenger.vedtak.v2.VedtakConstants;
import no.nav.vedtak.felles.integrasjon.felles.ws.JaxbHelper;

@ApplicationScoped
public class RegenererVedtaksXmlTjeneste {

    private FatteVedtakXmlTjeneste fpSakVedtakXmlTjeneste;
    private LagretVedtakRepository lagretVedtakRepository;
    private final Logger log = LoggerFactory.getLogger(RegenererVedtaksXmlTjeneste.class);
    private static Map<String, RegenererVedtaksXmlTjeneste.DokumentParserKonfig> SCHEMA_AND_CLASSES_TIL_STRUKTURERTE_DOKUMENTER = new HashMap<>();

    static {
        SCHEMA_AND_CLASSES_TIL_STRUKTURERTE_DOKUMENTER.put(VedtakConstants.NAMESPACE, new RegenererVedtaksXmlTjeneste.DokumentParserKonfig(
            VedtakConstants.JAXB_CLASS, VedtakConstants.XSD_LOCATION, VedtakConstants.ADDITIONAL_XSD_LOCATIONS, VedtakConstants.ADDITIONAL_CLASSES
        ));
        SCHEMA_AND_CLASSES_TIL_STRUKTURERTE_DOKUMENTER.put(ForeldrepengerVedtakConstants.NAMESPACE, new RegenererVedtaksXmlTjeneste.DokumentParserKonfig(
            ForeldrepengerVedtakConstants.JAXB_CLASS, ForeldrepengerVedtakConstants.XSD_LOCATION
        ));
    }

    public RegenererVedtaksXmlTjeneste() {
    }

    @Inject
    public RegenererVedtaksXmlTjeneste(FatteVedtakXmlTjeneste fpSakVedtakXmlTjeneste, LagretVedtakRepository lagretVedtakRepository) {
        this.fpSakVedtakXmlTjeneste = fpSakVedtakXmlTjeneste;
        this.lagretVedtakRepository = lagretVedtakRepository;
    }



    void validerOgRegenerer(Behandling behandling){
        LagretVedtak lagretVedtak = lagretVedtakRepository.hentLagretVedtakForBehandlingForOppdatering(behandling.getId());
        String namespace;
        try {
            namespace = retrieveNameSpaceOfXML(lagretVedtak.getXmlClob());
        } catch (XMLStreamException e) {
            log.info("Kunne ikke utlede namespace for vedtak {}, med behandlingid {}.", lagretVedtak.getId(), behandling.getId(),e);
            return;
        }
        RegenererVedtaksXmlTjeneste.DokumentParserKonfig dokumentParserKonfig = SCHEMA_AND_CLASSES_TIL_STRUKTURERTE_DOKUMENTER.get(namespace);
        try {
            JaxbHelper.unmarshalAndValidateXMLWithStAX(dokumentParserKonfig.jaxbClass, lagretVedtak.getXmlClob(), dokumentParserKonfig.xsdLocation, dokumentParserKonfig.additionalXsd, dokumentParserKonfig.additionalClasses);
            log.info("Vedtak med id {} og behandlingid {} gyldig ", lagretVedtak.getId(), behandling.getId());
        } catch (JAXBException | XMLStreamException | SAXException e) {
            log.info("Vedtak med id {} og behandlingid {} var ikke gyldig.", lagretVedtak.getId(), behandling.getId(),e);
            String vedtak = null;
            if (behandling.getFagsakYtelseType().gjelderForeldrepenger()) {
                vedtak = fpSakVedtakXmlTjeneste.opprettVedtakXml(behandling.getId());
            } else if (behandling.getFagsakYtelseType().gjelderEngangsstønad()) {
                vedtak = fpSakVedtakXmlTjeneste.opprettVedtakXml(behandling.getId());
            }
            lagretVedtakRepository.oppdater(lagretVedtak,vedtak);

        }

    }

    void regenerer(Behandling behandling) {
        LagretVedtak lagretVedtak = lagretVedtakRepository.hentLagretVedtakForBehandlingForOppdatering(behandling.getId());
        String vedtak = fpSakVedtakXmlTjeneste.opprettVedtakXml(behandling.getId());
        lagretVedtakRepository.oppdater(lagretVedtak, vedtak);

    }

    public boolean valider(Behandling behandling){
        LagretVedtak lagretVedtak = lagretVedtakRepository.hentLagretVedtakForBehandling(behandling.getId());
        String namespace;
        try {
            namespace = retrieveNameSpaceOfXML(lagretVedtak.getXmlClob());
        } catch (XMLStreamException e) { // NOSONAR
            log.info("Kunne ikke utlede namespace for vedtak {}, med behandlingid {}.", lagretVedtak.getId(), behandling.getId(),e);
            return false;
        }
        RegenererVedtaksXmlTjeneste.DokumentParserKonfig dokumentParserKonfig = SCHEMA_AND_CLASSES_TIL_STRUKTURERTE_DOKUMENTER.get(namespace);
        try {
            JaxbHelper.unmarshalAndValidateXMLWithStAX(dokumentParserKonfig.jaxbClass, lagretVedtak.getXmlClob(), dokumentParserKonfig.xsdLocation, dokumentParserKonfig.additionalXsd, dokumentParserKonfig.additionalClasses);
            log.info("Vedtak med id {} og behandlingid {} gyldig ", lagretVedtak.getId(), behandling.getId());
        } catch (JAXBException | XMLStreamException | SAXException e) {
            log.info("Vedtak med id {} og behandlingid {} var ikke gyldig.", lagretVedtak.getId(), behandling.getId(),e);
            return false;

        }
        return true;
    }



    private static class DokumentParserKonfig {
        Class<?> jaxbClass;
        String xsdLocation;
        String[] additionalXsd = new String[0];
        Class<?>[] additionalClasses = new Class[0];

        DokumentParserKonfig(Class<?> jaxbClass, String xsdLocation) {
            this.jaxbClass = jaxbClass;
            this.xsdLocation = xsdLocation;
        }

        public DokumentParserKonfig(Class<?> jaxbClass, String xsdLocation, String[] additionalXsd, Class<?>... additionalClasses) {
            this.jaxbClass = jaxbClass;
            this.xsdLocation = xsdLocation;
            this.additionalXsd = additionalXsd;
            this.additionalClasses = additionalClasses;
        }
    }
}

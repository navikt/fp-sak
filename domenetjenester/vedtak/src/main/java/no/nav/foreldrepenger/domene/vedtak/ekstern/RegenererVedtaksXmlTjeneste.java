package no.nav.foreldrepenger.domene.vedtak.ekstern;

import static no.nav.foreldrepenger.xmlutils.XmlUtils.retrieveNameSpaceOfXML;

import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.lagretvedtak.LagretVedtakRepository;
import no.nav.foreldrepenger.datavarehus.xml.FatteVedtakXmlTjeneste;
import no.nav.foreldrepenger.vedtak.v1.ForeldrepengerVedtakConstants;
import no.nav.foreldrepenger.vedtak.v2.VedtakConstants;
import no.nav.foreldrepenger.xmlutils.JaxbHelper;

@ApplicationScoped
public class RegenererVedtaksXmlTjeneste {

    private FatteVedtakXmlTjeneste fpSakVedtakXmlTjeneste;
    private LagretVedtakRepository lagretVedtakRepository;
    private static final Logger LOG = LoggerFactory.getLogger(RegenererVedtaksXmlTjeneste.class);
    private static final Map<String, RegenererVedtaksXmlTjeneste.DokumentParserKonfig> SCHEMA_AND_CLASSES_TIL_STRUKTURERTE_DOKUMENTER = new HashMap<>();

    static {
        SCHEMA_AND_CLASSES_TIL_STRUKTURERTE_DOKUMENTER.put(VedtakConstants.NAMESPACE, new RegenererVedtaksXmlTjeneste.DokumentParserKonfig(
            VedtakConstants.JAXB_CLASS, VedtakConstants.XSD_LOCATION, VedtakConstants.ADDITIONAL_XSD_LOCATIONS, VedtakConstants.ADDITIONAL_CLASSES
        ));
        SCHEMA_AND_CLASSES_TIL_STRUKTURERTE_DOKUMENTER.put(ForeldrepengerVedtakConstants.NAMESPACE, new RegenererVedtaksXmlTjeneste.DokumentParserKonfig(
            ForeldrepengerVedtakConstants.JAXB_CLASS, ForeldrepengerVedtakConstants.XSD_LOCATION
        ));
    }

    public RegenererVedtaksXmlTjeneste() {
        // Plattform trenger tom Ctor (Hibernate, CDI, etc)
    }

    @Inject
    public RegenererVedtaksXmlTjeneste(FatteVedtakXmlTjeneste fpSakVedtakXmlTjeneste, LagretVedtakRepository lagretVedtakRepository) {
        this.fpSakVedtakXmlTjeneste = fpSakVedtakXmlTjeneste;
        this.lagretVedtakRepository = lagretVedtakRepository;
    }



    void validerOgRegenerer(Behandling behandling){
        var lagretVedtak = lagretVedtakRepository.hentLagretVedtakForBehandlingForOppdatering(behandling.getId());
        String namespace;
        try {
            namespace = retrieveNameSpaceOfXML(lagretVedtak.getXmlClob());
        } catch (XMLStreamException e) {
            LOG.info("Kunne ikke utlede namespace for vedtak {}, med behandlingid {}.", lagretVedtak.getId(), behandling.getId(),e);
            return;
        }
        var dokumentParserKonfig = SCHEMA_AND_CLASSES_TIL_STRUKTURERTE_DOKUMENTER.get(namespace);
        try {
            JaxbHelper.unmarshalAndValidateXMLWithStAX(dokumentParserKonfig.jaxbClass, lagretVedtak.getXmlClob(), dokumentParserKonfig.xsdLocation, dokumentParserKonfig.additionalXsd, dokumentParserKonfig.additionalClasses);
            LOG.info("Vedtak med id {} og behandlingid {} gyldig ", lagretVedtak.getId(), behandling.getId());
        } catch (JAXBException | XMLStreamException | SAXException e) {
            LOG.info("Vedtak med id {} og behandlingid {} var ikke gyldig.", lagretVedtak.getId(), behandling.getId(),e);
            String vedtak = null;
            if (FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType()) || FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType())) {
                vedtak = fpSakVedtakXmlTjeneste.opprettVedtakXml(behandling.getId());
            }
            lagretVedtakRepository.oppdater(lagretVedtak,vedtak);

        }

    }

    void regenerer(Behandling behandling) {
        var lagretVedtak = lagretVedtakRepository.hentLagretVedtakForBehandlingForOppdatering(behandling.getId());
        var vedtak = fpSakVedtakXmlTjeneste.opprettVedtakXml(behandling.getId());
        lagretVedtakRepository.oppdater(lagretVedtak, vedtak);

    }

    public boolean valider(Behandling behandling){
        var lagretVedtak = lagretVedtakRepository.hentLagretVedtakForBehandling(behandling.getId());
        String namespace;
        try {
            namespace = retrieveNameSpaceOfXML(lagretVedtak.getXmlClob());
        } catch (XMLStreamException e) {
            LOG.info("Kunne ikke utlede namespace for vedtak {}, med behandlingid {}.", lagretVedtak.getId(), behandling.getId(),e);
            return false;
        }
        var dokumentParserKonfig = SCHEMA_AND_CLASSES_TIL_STRUKTURERTE_DOKUMENTER.get(namespace);
        try {
            JaxbHelper.unmarshalAndValidateXMLWithStAX(dokumentParserKonfig.jaxbClass, lagretVedtak.getXmlClob(), dokumentParserKonfig.xsdLocation, dokumentParserKonfig.additionalXsd, dokumentParserKonfig.additionalClasses);
            LOG.info("Vedtak med id {} og behandlingid {} gyldig ", lagretVedtak.getId(), behandling.getId());
        } catch (JAXBException | XMLStreamException | SAXException e) {
            LOG.info("Vedtak med id {} og behandlingid {} var ikke gyldig.", lagretVedtak.getId(), behandling.getId(),e);
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

package no.nav.foreldrepenger.domene.vedtak.innsyn;

import static no.nav.foreldrepenger.domene.vedtak.innsyn.TransformerVedtakXmlFeil.feilVedTransformeringAvVedtakXml;
import static no.nav.foreldrepenger.domene.vedtak.innsyn.TransformerVedtakXmlFeil.ioFeilVedTransformeringAvVedtakXml;
import static no.nav.foreldrepenger.domene.vedtak.innsyn.TransformerVedtakXmlFeil.ukjentNamespace;
import static no.nav.foreldrepenger.domene.vedtak.innsyn.TransformerVedtakXmlFeil.uventetFeilVedParsingAvVedtaksXml;
import static no.nav.foreldrepenger.domene.vedtak.innsyn.TransformerVedtakXmlFeil.vedtakXmlValiderteIkke;
import static no.nav.foreldrepenger.xmlutils.XmlUtils.retrieveNameSpaceOfXML;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.SAXException;

import no.nav.foreldrepenger.vedtak.v1.ForeldrepengerVedtakConstants;
import no.nav.foreldrepenger.vedtak.v2.VedtakConstants;
import no.nav.foreldrepenger.xmlutils.JaxbHelper;
import no.nav.foreldrepenger.xmlutils.XmlUtils;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.es.v2.PersonopplysningerEngangsstoenad;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.fp.v2.PersonopplysningerForeldrepenger;
import no.nav.vedtak.felles.xml.vedtak.v2.Vedtak;
import no.nav.vedtak.felles.xml.vedtak.ytelse.fp.v2.YtelseForeldrepenger;

public class VedtakXMLTilHTMLTransformator {

    private static final String TPS = "Folkeregisteret.";
    private static final String INNTEKTSKOMPONENTEN = "a-ordningen.";
    private static final String SIGRUN = "Sigrun.";
    private static final String MEDL2 = "Medlemsskapsregisteret.";
    private static final String AAREGISTERET = "Aa-registeret.";

    private static final Map<String, DokumentParserKonfig> SCHEMA_AND_CLASSES_TIL_STRUKTURERTE_DOKUMENTER = new HashMap<>();

    static {
        SCHEMA_AND_CLASSES_TIL_STRUKTURERTE_DOKUMENTER.put(VedtakConstants.NAMESPACE,
            new DokumentParserKonfig(VedtakConstants.JAXB_CLASS, VedtakConstants.XSD_LOCATION,
                VedtakConstants.ADDITIONAL_XSD_LOCATIONS, VedtakConstants.ADDITIONAL_CLASSES));
        SCHEMA_AND_CLASSES_TIL_STRUKTURERTE_DOKUMENTER.put(ForeldrepengerVedtakConstants.NAMESPACE,
            new DokumentParserKonfig(ForeldrepengerVedtakConstants.JAXB_CLASS,
                ForeldrepengerVedtakConstants.XSD_LOCATION));
    }

    private VedtakXMLTilHTMLTransformator() {
    }

    public static String transformer(String vedtakXML, Long lagretVedtakId) {

        var namespace = getNameSpace(lagretVedtakId, vedtakXML);

        try {
            var transformer = HtmlTransformerProvider.lagTransformer(namespace);
            var vedtakXmlStream = lagInputStream(vedtakXML, lagretVedtakId);
            var kilder = lagKildeoversikt(vedtakXML);
            return transformer(transformer, vedtakXmlStream, kilder);
        } catch (TransformerConfigurationException e) {
            throw feilVedTransformeringAvVedtakXml(lagretVedtakId, e);
        } catch (TransformerException e) {
            var lineNumber = e.getLocator() != null ? e.getLocator().getLineNumber() : null;
            var columnNumber = e.getLocator() != null ? e.getLocator().getColumnNumber() : null;
            throw feilVedTransformeringAvVedtakXml(lagretVedtakId, lineNumber, columnNumber, e);
        } catch (Exception e) {
            throw uventetFeilVedParsingAvVedtaksXml(lagretVedtakId, namespace, e);
        }
    }

    private static String getNameSpace(Long lagretVedtakId, String xml) {
        var nameSpaceOfXML = "ukjent";
        try {
            return XmlUtils.retrieveNameSpaceOfXML(xml);
        } catch (Exception e) {
            throw vedtakXmlValiderteIkke(lagretVedtakId, nameSpaceOfXML, e);
        }
    }

    private static String transformer(Transformer transformer,
                                      Source vedtakXmlStream,
                                      String kilder) throws TransformerException {
        var resultat = new StringWriter();
        var streamResultat = new StreamResult(resultat);

        transformer.transform(vedtakXmlStream, streamResultat);
        var sb = new StringBuilder(resultat.toString());
        if (kilder != null) {
            sb.insert(sb.indexOf("</body>"), kilder);
        }
        return sb.toString();
    }

    private static Source lagInputStream(String vedtakXML, Long lagretVedtakId) {
        InputStream sourceStream;
        try {
            sourceStream = new ByteArrayInputStream(vedtakXML.getBytes(StandardCharsets.UTF_8.name()));
            return new StreamSource(sourceStream);
        } catch (UnsupportedEncodingException e) {
            throw ioFeilVedTransformeringAvVedtakXml(lagretVedtakId, e);
        }
    }

    // HACK: hardkodet systemutledning basert på elementer i xml, kilde for info bør egentlig legges på xml feltattributter, og transformeres
    // med xslt
    private static String lagKildeoversikt(String vedtakXml) throws JAXBException, XMLStreamException, SAXException {
        var namespace = retrieveNameSpaceOfXML(vedtakXml);
        if (!VedtakConstants.NAMESPACE.equals(namespace)) {
            return null; // håndterer bare versjon 2
        }
        var sb = new StringBuilder();
        sb.append("<hr/><h2>Kilder</h2>\r\n<ul>\r\n");
        utledKilder(vedtakXml, namespace).forEach(kilde -> sb.append("<li>" + kilde + "</li>\r\n"));
        sb.append("</ul>");
        return sb.toString();
    }

    private static Set<String> utledKilder(String vedtakXml,
                                           String namespace) throws JAXBException, XMLStreamException, SAXException {
        Set<String> kilder = new HashSet<>();

        var dokumentParserKonfig = SCHEMA_AND_CLASSES_TIL_STRUKTURERTE_DOKUMENTER.get(namespace);
        if (dokumentParserKonfig == null) {
            throw ukjentNamespace(0L, namespace);
        }
        var vedtak = (Vedtak) JaxbHelper.unmarshalAndValidateXMLWithStAX(dokumentParserKonfig.jaxbClass, vedtakXml,
            dokumentParserKonfig.xsdLocation, dokumentParserKonfig.additionalXsd,
            dokumentParserKonfig.additionalClasses);
        kilder.add(TPS); // en behandling vil alltid innhente opplysninger fra TPS
        var personopplysninger = vedtak.getPersonOpplysninger();
        personopplysninger.getAny().forEach(ob -> håndterPersonopplysninger(ob, kilder, vedtak));

        return kilder;
    }

    private static void håndterPersonopplysninger(Object o, Set<String> kilder, Vedtak vedtak) {
        var po = ((JAXBElement<?>) o).getValue();

        if (po instanceof PersonopplysningerEngangsstoenad) {
            var poes = (PersonopplysningerEngangsstoenad) ((JAXBElement<?>) o).getValue();
            var poesm = poes.getMedlemskapsperioder();
            if (poesm != null && !poesm.getMedlemskapsperiode().isEmpty()) {
                kilder.add(MEDL2);
            }
            if (!((PersonopplysningerEngangsstoenad) po).getInntekter().getInntekt().isEmpty()) {
                kilder.add(INNTEKTSKOMPONENTEN);
            }
        } else if (po instanceof PersonopplysningerForeldrepenger) {
            var poes = (PersonopplysningerForeldrepenger) ((JAXBElement<?>) o).getValue();
            var medlemsskap = poes.getMedlemskap();
            if (medlemsskap != null && !medlemsskap.getMedlemskapsperiode().isEmpty()) {
                kilder.add(MEDL2);
            }
            if (!((PersonopplysningerForeldrepenger) po).getInntekter().getInntekt().isEmpty()) {
                kilder.add(INNTEKTSKOMPONENTEN);
            }
            if (poes.getEgenNaeringer() != null && !poes.getEgenNaeringer().getEgenNaering().isEmpty()) {
                kilder.add(SIGRUN);
            }

            var ber = vedtak.getBehandlingsresultat();
            var bres = ber.getBeregningsresultat();
            var ty = bres.getTilkjentYtelse();
            ty.getAny().forEach(ob -> håndterYtelser(ob, kilder));
        }
    }

    private static void håndterYtelser(Object o, Set<String> kilder) {
        var po = ((JAXBElement<?>) o).getValue();
        if (po instanceof YtelseForeldrepenger) {
            var yf = (YtelseForeldrepenger) ((JAXBElement<?>) o).getValue();
            var beregningsresultater = yf.getBeregningsresultat();
            for (var br : beregningsresultater) {
                if (br.getVirksomhet().getOrgnr() != null) {
                    kilder.add(AAREGISTERET);
                }
            }
        }
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

        public DokumentParserKonfig(Class<?> jaxbClass,
                                    String xsdLocation,
                                    String[] additionalXsd,
                                    Class<?>... additionalClasses) {
            this.jaxbClass = jaxbClass;
            this.xsdLocation = xsdLocation;
            this.additionalXsd = additionalXsd;
            this.additionalClasses = additionalClasses;
        }
    }
}

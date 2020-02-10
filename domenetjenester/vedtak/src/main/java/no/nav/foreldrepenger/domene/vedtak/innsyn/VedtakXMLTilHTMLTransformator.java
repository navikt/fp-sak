package no.nav.foreldrepenger.domene.vedtak.innsyn;

import static no.nav.foreldrepenger.domene.vedtak.innsyn.TransformerVedtakXmlFeil.FACTORY;
import static no.nav.vedtak.felles.xml.XmlUtils.retrieveNameSpaceOfXML;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import no.nav.vedtak.felles.integrasjon.felles.ws.JaxbHelper;
import no.nav.vedtak.felles.xml.XmlUtils;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.es.v2.PersonopplysningerEngangsstoenad;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.fp.v2.Medlemskap;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.fp.v2.PersonopplysningerForeldrepenger;
import no.nav.vedtak.felles.xml.vedtak.v2.Behandlingsresultat;
import no.nav.vedtak.felles.xml.vedtak.v2.Beregningsresultat;
import no.nav.vedtak.felles.xml.vedtak.v2.Personopplysninger;
import no.nav.vedtak.felles.xml.vedtak.v2.TilkjentYtelse;
import no.nav.vedtak.felles.xml.vedtak.v2.Vedtak;
import no.nav.vedtak.felles.xml.vedtak.ytelse.fp.v2.YtelseForeldrepenger;

public class VedtakXMLTilHTMLTransformator {

    private static final String TPS = "Folkeregisteret.";
    private static final String INNTEKTSKOMPONENTEN = "a-ordningen.";
    private static final String SIGRUN = "Sigrun.";
    private static final String MEDL2 = "Medlemsskapsregisteret.";
    private static final String AAREGISTERET = "Aa-registeret.";

    private static Map<String, DokumentParserKonfig> SCHEMA_AND_CLASSES_TIL_STRUKTURERTE_DOKUMENTER = new HashMap<>();

    static {
        SCHEMA_AND_CLASSES_TIL_STRUKTURERTE_DOKUMENTER.put(VedtakConstants.NAMESPACE, new DokumentParserKonfig(
            VedtakConstants.JAXB_CLASS, VedtakConstants.XSD_LOCATION, VedtakConstants.ADDITIONAL_XSD_LOCATIONS, VedtakConstants.ADDITIONAL_CLASSES));
        SCHEMA_AND_CLASSES_TIL_STRUKTURERTE_DOKUMENTER.put(ForeldrepengerVedtakConstants.NAMESPACE, new DokumentParserKonfig(
            ForeldrepengerVedtakConstants.JAXB_CLASS, ForeldrepengerVedtakConstants.XSD_LOCATION));
    }

    private VedtakXMLTilHTMLTransformator() {
    }

    public static String transformer(String vedtakXML, Long lagretVedtakId) {

        String namespace = getNameSpace(lagretVedtakId, vedtakXML);

        try {
            Transformer transformer = HtmlTransformerProvider.lagTransformer(namespace);
            Source vedtakXmlStream = lagInputStream(vedtakXML, lagretVedtakId);
            String kilder = lagKildeoversikt(vedtakXML);
            return transformer(transformer, vedtakXmlStream, kilder);
        } catch (TransformerConfigurationException e) {
            throw FACTORY.feilVedTransformeringAvVedtakXml(lagretVedtakId, e).toException();
        } catch (TransformerException e) {
            Integer lineNumber = e.getLocator() != null ? e.getLocator().getLineNumber() : null;
            Integer columnNumber = e.getLocator() != null ? e.getLocator().getColumnNumber() : null;
            throw FACTORY.feilVedTransformeringAvVedtakXml(lagretVedtakId, lineNumber, columnNumber, e).toException();
        } catch (Exception e) {
            throw FACTORY.uventetFeilVedParsingAvVedtaksXml(lagretVedtakId, namespace, e).toException();
        }
    }

    private static String getNameSpace(Long lagretVedtakId, String xml) {
        String nameSpaceOfXML = "ukjent";
        try {
            return XmlUtils.retrieveNameSpaceOfXML(xml);
        } catch (Exception e) {
            throw FACTORY.vedtakXmlValiderteIkke(lagretVedtakId, nameSpaceOfXML, e).toException();
        }
    }

    private static String transformer(Transformer transformer, Source vedtakXmlStream, String kilder) throws TransformerException {
        StringWriter resultat = new StringWriter();
        StreamResult streamResultat = new StreamResult(resultat);

        transformer.transform(vedtakXmlStream, streamResultat);
        StringBuilder sb = new StringBuilder(resultat.toString());
        if (kilder != null) {
            sb.insert(sb.indexOf("</body>"), kilder);
        }
        return sb.toString();
    }

    private static Source lagInputStream(String vedtakXML, Long lagretVedtakId) {
        InputStream sourceStream = null;
        try {
            sourceStream = new ByteArrayInputStream(vedtakXML.getBytes(StandardCharsets.UTF_8.name()));
            return new StreamSource(sourceStream);
        } catch (UnsupportedEncodingException e) {
            throw FACTORY.ioFeilVedTransformeringAvVedtakXml(lagretVedtakId, e).toException();
        }
    }

    // HACK: hardkodet systemutledning basert på elementer i xml, kilde for info bør egentlig legges på xml feltattributter, og transformeres
    // med xslt
    private static String lagKildeoversikt(String vedtakXml) throws JAXBException, XMLStreamException, SAXException {
        final String namespace = retrieveNameSpaceOfXML(vedtakXml);
        if (!VedtakConstants.NAMESPACE.equals(namespace)) {
            return null; // håndterer bare versjon 2
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<hr/><h2>Kilder</h2>\r\n<ul>\r\n");
        utledKilder(vedtakXml, namespace).forEach(kilde -> sb.append("<li>" + kilde + "</li>\r\n"));
        sb.append("</ul>");
        return sb.toString();
    }

    private static Set<String> utledKilder(String vedtakXml, String namespace) throws JAXBException, XMLStreamException, SAXException {
        Set<String> kilder = new HashSet<>();

        DokumentParserKonfig dokumentParserKonfig = SCHEMA_AND_CLASSES_TIL_STRUKTURERTE_DOKUMENTER.get(namespace);
        if (dokumentParserKonfig == null) {
            throw FACTORY.ukjentNamespace(0L, namespace).toException();
        }
        Vedtak vedtak = (Vedtak) JaxbHelper.unmarshalAndValidateXMLWithStAX(dokumentParserKonfig.jaxbClass, vedtakXml, dokumentParserKonfig.xsdLocation,
            dokumentParserKonfig.additionalXsd, dokumentParserKonfig.additionalClasses);
        kilder.add(TPS); // en behandling vil alltid innhente opplysninger fra TPS
        Personopplysninger personopplysninger = vedtak.getPersonOpplysninger();
        personopplysninger.getAny().forEach(ob -> håndterPersonopplysninger(ob, kilder, vedtak));

        return kilder;
    }

    private static void håndterPersonopplysninger(Object o, Set<String> kilder, Vedtak vedtak) {
        Object po = ((JAXBElement<?>) o).getValue();

        if (po instanceof PersonopplysningerEngangsstoenad) { // NOSONAR
            PersonopplysningerEngangsstoenad poes = (PersonopplysningerEngangsstoenad) ((JAXBElement<?>) o).getValue();
            PersonopplysningerEngangsstoenad.Medlemskapsperioder poesm = poes.getMedlemskapsperioder();
            if (poesm != null && !poesm.getMedlemskapsperiode().isEmpty()) {
                kilder.add(MEDL2);
            }
            if (!((PersonopplysningerEngangsstoenad) po).getInntekter().getInntekt().isEmpty()) {
                kilder.add(INNTEKTSKOMPONENTEN);
            }
        } else if (po instanceof PersonopplysningerForeldrepenger) { // NOSONAR
            PersonopplysningerForeldrepenger poes = (PersonopplysningerForeldrepenger) ((JAXBElement<?>) o).getValue();
            Medlemskap medlemsskap = poes.getMedlemskap();
            if (medlemsskap != null && !medlemsskap.getMedlemskapsperiode().isEmpty()) {
                kilder.add(MEDL2);
            }
            if (!((PersonopplysningerForeldrepenger) po).getInntekter().getInntekt().isEmpty()) {
                kilder.add(INNTEKTSKOMPONENTEN);
            }
            if (poes.getEgenNaeringer() != null && !poes.getEgenNaeringer().getEgenNaering().isEmpty()) {
                kilder.add(SIGRUN);
            }

            Behandlingsresultat ber = vedtak.getBehandlingsresultat();
            Beregningsresultat bres = ber.getBeregningsresultat();
            TilkjentYtelse ty = bres.getTilkjentYtelse();
            ty.getAny().forEach(ob -> håndterYtelser(ob, kilder));
        }
    }

    private static void håndterYtelser(Object o, Set<String> kilder) {
        Object po = ((JAXBElement<?>) o).getValue();
        if (po instanceof YtelseForeldrepenger) {
            YtelseForeldrepenger yf = (YtelseForeldrepenger) ((JAXBElement<?>) o).getValue();
            List<no.nav.vedtak.felles.xml.vedtak.ytelse.fp.v2.Beregningsresultat> beregningsresultater = yf.getBeregningsresultat();
            for (no.nav.vedtak.felles.xml.vedtak.ytelse.fp.v2.Beregningsresultat br : beregningsresultater) {
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

        public DokumentParserKonfig(Class<?> jaxbClass, String xsdLocation, String[] additionalXsd, Class<?>... additionalClasses) {
            this.jaxbClass = jaxbClass;
            this.xsdLocation = xsdLocation;
            this.additionalXsd = additionalXsd;
            this.additionalClasses = additionalClasses;
        }
    }
}

package no.nav.foreldrepenger.mottak.dokumentpersiterer.xml;

import static no.nav.foreldrepenger.xmlutils.XmlUtils.retrieveNameSpaceOfXML;

import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.MottattDokumentWrapper;
import no.nav.foreldrepenger.søknad.v3.SøknadConstants;
import no.nav.foreldrepenger.xmlutils.JaxbHelper;
import no.nav.vedtak.exception.TekniskException;
import no.seres.xsd.nav.inntektsmelding_m._201812.InntektsmeldingConstants;

public final class MottattDokumentXmlParser {

    private static final Pattern INNSENDING_MINUTT = Pattern.compile(".*T\\d{2}:\\d{2}</innsendingstidspunkt.*");

    private static final Map<String, DokumentParserKonfig> SCHEMA_AND_CLASSES_TIL_STRUKTURERTE_DOKUMENTER = Map.of(
        no.seres.xsd.nav.inntektsmelding_m._201809.InntektsmeldingConstants.NAMESPACE,
            new DokumentParserKonfig(no.seres.xsd.nav.inntektsmelding_m._201809.InntektsmeldingConstants.JAXB_CLASS,
                no.seres.xsd.nav.inntektsmelding_m._201809.InntektsmeldingConstants.XSD_LOCATION),
        InntektsmeldingConstants.NAMESPACE,
            new DokumentParserKonfig(InntektsmeldingConstants.JAXB_CLASS, InntektsmeldingConstants.XSD_LOCATION),
        SøknadConstants.NAMESPACE,
            new DokumentParserKonfig(SøknadConstants.JAXB_CLASS, SøknadConstants.XSD_LOCATION,
                SøknadConstants.ADDITIONAL_XSD_LOCATION, SøknadConstants.ADDITIONAL_CLASSES)
    );

    private static final Map<String, Function<String, String>> KORREKSJONER = Map.of(
        no.seres.xsd.nav.inntektsmelding_m._201809.InntektsmeldingConstants.NAMESPACE, MottattDokumentXmlParser::normaliserInntektsmelding,
        InntektsmeldingConstants.NAMESPACE, MottattDokumentXmlParser::normaliserInntektsmelding,
        SøknadConstants.NAMESPACE, Function.identity()
    );


    private MottattDokumentXmlParser() {
    }

    @SuppressWarnings("rawtypes")
    public static MottattDokumentWrapper unmarshallXml(String xml) {
        final Object mottattDokument;
        var namespace = hentNamespace(xml);

        try {
            var dokumentParserKonfig = SCHEMA_AND_CLASSES_TIL_STRUKTURERTE_DOKUMENTER.get(namespace);
            if (dokumentParserKonfig == null) {
                throw new TekniskException("FP-958724", "Fant ikke xsd for namespacet " + namespace);
            }
            mottattDokument = JaxbHelper.unmarshalAndValidateXMLWithStAX(dokumentParserKonfig.jaxbClass,
                KORREKSJONER.getOrDefault(namespace, Function.identity()).apply(xml),
                dokumentParserKonfig.xsdLocation,
                dokumentParserKonfig.additionalXsd,
                dokumentParserKonfig.additionalClasses);
            return MottattDokumentWrapper.tilXmlWrapper(mottattDokument);
        } catch (Exception e) {
            throw parseException(namespace, e);
        }
    }

    private static TekniskException parseException(String namespace, Exception e) {
        return new TekniskException("FP-312346", "Feil ved parsing av ukjent journaldokument-type med namespace " + namespace, e);
    }

    private static String hentNamespace(String xml) {
        final String namespace;
        try {
            namespace = retrieveNameSpaceOfXML(xml);
        } catch (Exception e) {
            throw parseException("ukjent", e);
        }
        return namespace;
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

    public static String normaliserInntektsmelding(String xml) {
        if (INNSENDING_MINUTT.matcher(xml).find()) {
            return xml.replace("</innsendingstidspunkt>", ":00</innsendingstidspunkt>");
        } else {
            return xml;
        }
    }
}

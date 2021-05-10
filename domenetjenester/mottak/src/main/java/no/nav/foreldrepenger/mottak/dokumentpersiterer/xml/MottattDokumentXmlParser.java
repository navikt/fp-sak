package no.nav.foreldrepenger.mottak.dokumentpersiterer.xml;

import static no.nav.vedtak.xmlutils.XmlUtils.retrieveNameSpaceOfXML;

import java.util.HashMap;
import java.util.Map;

import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.MottattDokumentWrapper;
import no.nav.foreldrepenger.xmlutils.JaxbHelper;
import no.nav.vedtak.exception.TekniskException;

public final class MottattDokumentXmlParser {

    private static final Map<String, DokumentParserKonfig> SCHEMA_AND_CLASSES_TIL_STRUKTURERTE_DOKUMENTER = new HashMap<>();

    static {
        SCHEMA_AND_CLASSES_TIL_STRUKTURERTE_DOKUMENTER.put(no.seres.xsd.nav.inntektsmelding_m._201809.InntektsmeldingConstants.NAMESPACE,
            new DokumentParserKonfig(no.seres.xsd.nav.inntektsmelding_m._201809.InntektsmeldingConstants.JAXB_CLASS,
                no.seres.xsd.nav.inntektsmelding_m._201809.InntektsmeldingConstants.XSD_LOCATION));
        SCHEMA_AND_CLASSES_TIL_STRUKTURERTE_DOKUMENTER.put(no.seres.xsd.nav.inntektsmelding_m._201812.InntektsmeldingConstants.NAMESPACE,
            new DokumentParserKonfig(no.seres.xsd.nav.inntektsmelding_m._201812.InntektsmeldingConstants.JAXB_CLASS,
                no.seres.xsd.nav.inntektsmelding_m._201812.InntektsmeldingConstants.XSD_LOCATION));
        SCHEMA_AND_CLASSES_TIL_STRUKTURERTE_DOKUMENTER.put(no.nav.foreldrepenger.søknad.v3.SøknadConstants.NAMESPACE,
            new DokumentParserKonfig(no.nav.foreldrepenger.søknad.v3.SøknadConstants.JAXB_CLASS, no.nav.foreldrepenger.søknad.v3.SøknadConstants.XSD_LOCATION,
                no.nav.foreldrepenger.søknad.v3.SøknadConstants.ADDITIONAL_XSD_LOCATION, no.nav.foreldrepenger.søknad.v3.SøknadConstants.ADDITIONAL_CLASSES));
    }


    private MottattDokumentXmlParser() {
    }

    @SuppressWarnings("rawtypes")
    public static MottattDokumentWrapper unmarshallXml(String xml) {
        final Object mottattDokument;
        final var namespace = hentNamespace(xml);

        try {
            var dokumentParserKonfig = SCHEMA_AND_CLASSES_TIL_STRUKTURERTE_DOKUMENTER.get(namespace);
            if (dokumentParserKonfig == null) {
                throw new TekniskException("FP-958724", "Fant ikke xsd for namespacet " + namespace);
            }
            mottattDokument = JaxbHelper.unmarshalAndValidateXMLWithStAX(dokumentParserKonfig.jaxbClass,
                xml,
                dokumentParserKonfig.xsdLocation,
                dokumentParserKonfig.additionalXsd,
                dokumentParserKonfig.additionalClasses);
            return MottattDokumentWrapper.tilXmlWrapper(mottattDokument);
        } catch (Exception e) {
            throw parseException(namespace);
        }
    }

    private static TekniskException parseException(String namespace) {
        return new TekniskException("FP-312346", "Feil ved parsing av ukjent journaldokument-type med namespace " + namespace);
    }

    private static String hentNamespace(String xml) {
        final String namespace;
        try {
            namespace = retrieveNameSpaceOfXML(xml);
        } catch (Exception e) {
            throw parseException("ukjent");
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
}

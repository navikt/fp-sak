package no.nav.foreldrepenger.domene.vedtak.innsyn;

import no.nav.vedtak.exception.TekniskException;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.UnsupportedEncodingException;

final class TransformerVedtakXmlFeil {

    private TransformerVedtakXmlFeil() {
    }

    static TekniskException feilVedTransformeringAvVedtakXml(Long behandlingId,
                                                             TransformerConfigurationException cause) {
        var msg = String.format(
            "Fikk uventet feil ved transformasjon av VedtakXML for behandlingId '%s' til leselig format", behandlingId);
        return new TekniskException("FP-296812", msg, cause);
    }

    static TekniskException feilVedTransformeringAvVedtakXml(Long behandlingId,
                                                             Integer line,
                                                             Integer columnNumber,
                                                             TransformerException cause) {
        var msg = String.format(
            "Fikk uventet feil ved transformasjon av VedtakXML for behandlingId '%s' til leselig format (linje %s kolonne %s). "
                + "Forventet at kan skje ved transfomasjon av vedtak som er gjort på fundamentet.", behandlingId, line,
            columnNumber);
        return new TekniskException("FP-956702", msg, cause);
    }

    static TekniskException ioFeilVedTransformeringAvVedtakXml(Long behandlingId, UnsupportedEncodingException cause) {
        return new TekniskException("FP-566266",
            String.format("VedtakXMl var ikke UTF-8-encodet for behandlingId '%s'", behandlingId), cause);
    }

    static TekniskException ukjentNamespace(Long behandlingId, String nameSpaceOfXML) {
        var msg = String.format(
            "VedtakXMl har et ukjent namespacee for behandlingId '%s'." + " Namespace of XML er '%s'.", behandlingId,
            nameSpaceOfXML);
        return new TekniskException("FP-116361", msg);
    }

    static TekniskException vedtakXmlValiderteIkke(Long behandlingId, String nameSpaceOfXML, Exception cause) {
        var msg = String.format("VedtakXMl validerer ikke mot xsd for behandlingId '%s'. Namespace of XML er '%s'.",
            behandlingId, nameSpaceOfXML);
        return new TekniskException("FP-376155", msg, cause);
    }

    static TekniskException uventetFeilVedParsingAvVedtaksXml(Long behandlingId,
                                                              String nameSpaceOfXML,
                                                              Exception cause) {
        var msg = String.format("Feil ved parsing av vedtaksxml behandling %s namespace '%s'", behandlingId,
            nameSpaceOfXML);
        return new TekniskException("FP-376156", msg, cause);
    }

}

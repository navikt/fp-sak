package no.nav.foreldrepenger.domene.vedtak.innsyn;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import no.nav.foreldrepenger.vedtak.v1.ForeldrepengerVedtakConstants;
import no.nav.foreldrepenger.vedtak.v2.VedtakConstants;

public class HtmlTransformerProvider {

    private static Map<String, String> namespaces = Collections.unmodifiableMap(Stream.of(
            new AbstractMap.SimpleEntry<>(ForeldrepengerVedtakConstants.NAMESPACE, "vedtakXmlTilHtml_v1.xsl"),
            new AbstractMap.SimpleEntry<>(VedtakConstants.NAMESPACE, "vedtakXmlTilHtml_v2.xsl"))
            .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue)));

    private static Map<String, Templates> templates = new ConcurrentHashMap<>();

    private HtmlTransformerProvider() {
    }

    private static String getXslFileForNamespace(String namespace) {
        return namespaces.getOrDefault(namespace, "vedtakXmlTilHtml_v2.xsl"); //Vi defaulter til versjon 2 da den skal være generell og forhåpentligvis også fungere med framtidige versjoner
    }

    private static Templates lagTemplate(String xslTransformerFilename) {
        var factory = TransformerFactory.newInstance();

        var classLoader = Thread.currentThread().getContextClassLoader();
        // FIXME (ToreEndestad): Denne kan caches statisk og gjenbrukes (template)
        try(var inputStream = classLoader.getResourceAsStream(xslTransformerFilename)) {
            var template = factory.newTemplates(new StreamSource(inputStream));
            return template;
        } catch (TransformerConfigurationException | IOException e) {
            throw new IllegalStateException("Kunne ikke lese template: " + xslTransformerFilename , e);
        }
    }

    public static Transformer lagTransformer(String namespace) throws TransformerConfigurationException {
        return templates.computeIfAbsent(namespace, ns -> lagTemplate(getXslFileForNamespace(ns))).newTransformer();
    }
}

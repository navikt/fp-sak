package no.nav.foreldrepenger.økonomistøtte;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

//TODO palfi fjern ligger i felles
public final class JaxbHelper {
    private static final Map<Class<?>, JAXBContext> CONTEXTS = new ConcurrentHashMap<>(); // NOSONAR
    private static final Map<String, Schema> SCHEMAS = new ConcurrentHashMap<>(); // NOSONAR

    private JaxbHelper() {
    }

    public static String marshalAndValidateJaxb(Class<?> clazz, Object jaxbObject, String xsdLocation) throws JAXBException, SAXException {
        if (!CONTEXTS.containsKey(clazz)) {
            CONTEXTS.put(clazz, JAXBContext.newInstance(clazz));
        }
        Marshaller marshaller = CONTEXTS.get(clazz).createMarshaller();

        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        Schema schema = getSchema(xsdLocation);
        marshaller.setSchema(schema);

        StringWriter writer = new StringWriter();
        marshaller.marshal(jaxbObject, writer);
        return writer.toString();
    }

    public static <T> T unmarshalAndValidateXMLWithStAX(Class<T> clazz, String xml, String xsdLocation) throws JAXBException, XMLStreamException, SAXException {
        if (!CONTEXTS.containsKey(clazz)) {
            CONTEXTS.put(clazz, JAXBContext.newInstance(clazz));
        }

        Schema schema = null;
        if (xsdLocation != null) {
            schema = getSchema(xsdLocation);
        }

        return unmarshalAndValidateXMLWithStAXProvidingSchema(clazz, new StreamSource(new StringReader(xml)), schema);
    }

    public static <T> T unmarshalAndValidateXMLWithStAXProvidingSchema(Class<T> clazz, Source source, Schema schema)
        throws JAXBException, XMLStreamException {
        if (!CONTEXTS.containsKey(clazz)) {
            CONTEXTS.put(clazz, JAXBContext.newInstance(clazz));
        }
        Unmarshaller unmarshaller = CONTEXTS.get(clazz).createUnmarshaller();

        if (schema != null) {
            unmarshaller.setSchema(schema);
        }

        XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
        xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(source);

        JAXBElement<T> root = unmarshaller.unmarshal(xmlStreamReader, clazz);

        return root.getValue();
    }

    private static Schema getSchema(String xsdLocation) throws SAXException {
        if (!SCHEMAS.containsKey(xsdLocation)) {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            final String systemId = JaxbHelper.class.getClassLoader().getResource(xsdLocation).toExternalForm();
            final StreamSource source = new StreamSource(systemId);
            SCHEMAS.putIfAbsent(xsdLocation, schemaFactory.newSchema(source));
        }
        return SCHEMAS.get(xsdLocation);
    }
}

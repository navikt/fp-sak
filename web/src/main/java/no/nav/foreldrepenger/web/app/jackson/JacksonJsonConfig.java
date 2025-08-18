package no.nav.foreldrepenger.web.app.jackson;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import no.nav.foreldrepenger.domene.opptjening.dto.AvklarAktivitetsPerioderDto;
import no.nav.foreldrepenger.domene.person.verge.dto.AvklarVergeDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderFaktaOmBeregningDto;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto;
import no.nav.foreldrepenger.web.app.IndexClasses;
import no.nav.foreldrepenger.web.app.tjenester.RestImplementationClasses;

@Provider
public class JacksonJsonConfig implements ContextResolver<ObjectMapper> {

    private final ObjectMapper objectMapper;

    /**
     * Default instance for Jax-rs application. Genererer ikke navn som del av
     * output for kodeverk.
     */
    public JacksonJsonConfig() {
        this(false);
    }

    public JacksonJsonConfig(boolean serialiserKodelisteNavn) {
        objectMapper = createObjectMapper(createModule(serialiserKodelisteNavn));
    }

    private static ObjectMapper createObjectMapper(SimpleModule simpleModule) {
        var om = new ObjectMapper();
        om.registerModule(new Jdk8Module());
        om.registerModule(new JavaTimeModule());
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        om.registerModule(simpleModule);

        // registrer jackson JsonTypeName subtypes basert på rest implementasjoner
        var restClasses = RestImplementationClasses.getImplementationClasses();

        Set<Class<?>> scanClasses = new LinkedHashSet<>(restClasses);

        // hack - additional locations to scan (jars uten rest services) - trenger det
        // her p.t. for å bestemme hvilke jars / maven moduler som skal scannes for
        // andre dtoer
        scanClasses.add(AvklarAktivitetsPerioderDto.class);
        scanClasses.add(VurderFaktaOmBeregningDto.class);
        scanClasses.add(AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto.class);
        scanClasses.add(AvklarVergeDto.class);

        // avled code location fra klassene
        scanClasses
                .stream()
                .map(c -> {
                    try {
                        return c.getProtectionDomain().getCodeSource().getLocation().toURI();
                    } catch (URISyntaxException e) {
                        throw new IllegalArgumentException("Ikke en URI for klasse: " + c, e);
                    }
                })
                .distinct()
                .forEach(uri -> om.registerSubtypes(getJsonTypeNameClasses(uri)));
        return om;
    }

    private static SimpleModule createModule(boolean serialiserKodelisteNavn) {
        var module = new SimpleModule("VL-REST", new Version(1, 0, 0, null, null, null));

        addSerializers(module, serialiserKodelisteNavn);

        return module;
    }

    private static void addSerializers(SimpleModule module, boolean serialiserKodelisteNavn) {
        module.addSerializer(new KodeverdiSerializer(serialiserKodelisteNavn));
        module.addSerializer(new KalkulusKodeverdiSerializer());
    }

    /**
     * Scan subtyper dynamisk fra WAR slik at superklasse slipper å
     * deklarere @JsonSubtypes.
     */
    public static List<Class<?>> getJsonTypeNameClasses(URI classLocation) {
        IndexClasses indexClasses;
        indexClasses = IndexClasses.getIndexFor(classLocation);
        return indexClasses.getClassesWithAnnotation(JsonTypeName.class);
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return objectMapper;
    }

}

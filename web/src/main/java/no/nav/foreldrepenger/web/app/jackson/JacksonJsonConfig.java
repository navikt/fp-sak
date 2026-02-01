package no.nav.foreldrepenger.web.app.jackson;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import no.nav.foreldrepenger.domene.opptjening.dto.AvklarAktivitetsPerioderDto;
import no.nav.foreldrepenger.domene.person.verge.dto.AvklarVergeDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderFaktaOmBeregningDto;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.omsorgsovertakelse.dto.VurderOmsorgsovertakelseVilkårAksjonspunktDto;
import no.nav.foreldrepenger.web.app.IndexClasses;
import no.nav.foreldrepenger.web.app.tjenester.RestImplementationClasses;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;
import no.nav.vedtak.sikkerhet.oidc.token.impl.TokenXExchangeKlient;

@Provider
public class JacksonJsonConfig implements ContextResolver<ObjectMapper> {

    private static final JsonMapper JSON_MAPPER = createObjectMapper();

    private static synchronized JsonMapper createObjectMapper() {
        // registrer jackson JsonTypeName subtypes basert på rest implementasjoner
        var restClasses = RestImplementationClasses.getImplementationClasses();

        Set<Class<?>> scanClasses = new LinkedHashSet<>(restClasses);

        // hack - additional locations to scan (jars uten rest services) - trenger det
        // her p.t. for å bestemme hvilke jars / maven moduler som skal scannes for
        // andre dtoer
        scanClasses.add(AvklarAktivitetsPerioderDto.class);
        scanClasses.add(VurderFaktaOmBeregningDto.class);
        scanClasses.add(VurderOmsorgsovertakelseVilkårAksjonspunktDto.class);
        scanClasses.add(AvklarVergeDto.class);

        // avled code location fra klassene
        var typeNameClasses = scanClasses
                .stream()
                .map(c -> {
                    try {
                        return c.getProtectionDomain().getCodeSource().getLocation().toURI();
                    } catch (URISyntaxException e) {
                        throw new IllegalArgumentException("Ikke en URI for klasse: " + c, e);
                    }
                })
                .distinct()
                .map(JacksonJsonConfig::getJsonTypeNameClasses)
                .flatMap(List::stream)
                .toList();
        return DefaultJsonMapper.getJsonMapper().rebuild().registerSubtypes(typeNameClasses).build();
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

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return JSON_MAPPER;
    }

}

package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.web.app.rest.ResourceLink;
import no.nav.foreldrepenger.web.app.rest.ResourceLinks;
import no.nav.foreldrepenger.web.app.tjenester.RestImplementationClasses;

import org.glassfish.jersey.uri.UriTemplate;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class BehandlingDtoLenkeTestUtils {
    private static final LocalDate now = LocalDate.now();

    static Set<Behandling> lagOgLagreBehandlinger(BehandlingRepositoryProvider repositoryProvider) {
        var orginalFP = lagFPBehandling().medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD).lagre(repositoryProvider);
        var orginalSVP = lagSVPBehandling().medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD).lagre(repositoryProvider);
        var orginalES = lagESBehandling().medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD).lagre(repositoryProvider);

        return Set.of(orginalFP,
            lagFPBehandling().medBehandlingType(BehandlingType.REVURDERING).medOriginalBehandling(orginalFP, BehandlingÅrsakType.RE_ANNET).lagre(repositoryProvider),
            lagFPBehandling().medBehandlingType(BehandlingType.KLAGE).medOriginalBehandling(orginalFP,BehandlingÅrsakType.KLAGE_TILBAKEBETALING).lagre(repositoryProvider),
            lagFPBehandling().medBehandlingType(BehandlingType.ANKE).medOriginalBehandling(orginalFP, BehandlingÅrsakType.ETTER_KLAGE).lagre(repositoryProvider),
            lagFPBehandling().medBehandlingType(BehandlingType.INNSYN).lagre(repositoryProvider),

            orginalSVP,
            lagSVPBehandling().medBehandlingType(BehandlingType.REVURDERING).medOriginalBehandling(orginalSVP, BehandlingÅrsakType.RE_ANNET).lagre(repositoryProvider),
            lagSVPBehandling().medBehandlingType(BehandlingType.KLAGE).medOriginalBehandling(orginalSVP,BehandlingÅrsakType.KLAGE_TILBAKEBETALING).lagre(repositoryProvider),
            lagSVPBehandling().medBehandlingType(BehandlingType.ANKE).medOriginalBehandling(orginalSVP, BehandlingÅrsakType.ETTER_KLAGE).lagre(repositoryProvider),
            lagSVPBehandling().medBehandlingType(BehandlingType.INNSYN).lagre(repositoryProvider),

            orginalES,
            lagESBehandling().medBehandlingType(BehandlingType.REVURDERING).medOriginalBehandling(orginalES, BehandlingÅrsakType.RE_ANNET).lagre(repositoryProvider),
            lagESBehandling().medBehandlingType(BehandlingType.KLAGE).medOriginalBehandling(orginalES,BehandlingÅrsakType.KLAGE_TILBAKEBETALING).lagre(repositoryProvider),
            lagESBehandling().medBehandlingType(BehandlingType.ANKE).medOriginalBehandling(orginalES, BehandlingÅrsakType.ETTER_KLAGE).lagre(repositoryProvider),
            lagESBehandling().medBehandlingType(BehandlingType.INNSYN).lagre(repositoryProvider)
            );

    }

    static ScenarioMorSøkerEngangsstønad lagESBehandling() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().erFødsel().build();
        return scenario;
    }

    static ScenarioMorSøkerSvangerskapspenger lagSVPBehandling() {
        return ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
    }

    static ScenarioMorSøkerForeldrepenger lagFPBehandling() {
        return ScenarioMorSøkerForeldrepenger.forFødsel()
            .medDefaultSøknadTerminbekreftelse()
            .medDefaultBekreftetTerminbekreftelse()
            .medFordeling(new OppgittFordelingEntitet(Collections.singletonList(
                OppgittPeriodeBuilder.ny().medPeriodeType(UttakPeriodeType.FEDREKVOTE).medPeriode(now.plusWeeks(8), now.plusWeeks(12)).build()),
                true));

    }

    static Collection<AnnotatedRoute> getRoutes() {
        Set<AnnotatedRoute> routes = new HashSet<>();
        var restClasses = RestImplementationClasses.getImplementationClasses();
        for (var aClass : restClasses) {
            var pathFromClass = getClassAnnotationValue(aClass, Path.class, "value");
            var methods = aClass.getMethods();
            for (var aMethod : methods) {
                ResourceLink.HttpMethod method = null;
                if (aMethod.getAnnotation(POST.class) != null) {
                    method = ResourceLink.HttpMethod.POST;
                }
                if (aMethod.getAnnotation(GET.class) != null) {
                    method = ResourceLink.HttpMethod.GET;
                }
                if (aMethod.getAnnotation(PUT.class) != null) {
                    method = ResourceLink.HttpMethod.PUT;
                }
                if (aMethod.getAnnotation(DELETE.class) != null) {
                    method = ResourceLink.HttpMethod.DELETE;
                }
                if (method != null) {
                    var pathFromMethod = "";
                    if (aMethod.getAnnotation(Path.class) != null) {
                        pathFromMethod = aMethod.getAnnotation(Path.class).value();
                    }

                    List<String> queryParameters = Arrays.stream(aMethod.getParameters())
                        .map(p -> p.getDeclaredAnnotation(QueryParam.class))
                        .filter(Objects::nonNull)
                        .map(QueryParam::value)
                        .toList();

                    var path = pathFromClass + pathFromMethod;
                    routes.add(new AnnotatedRoute(method, path, queryParameters));
                }
            }
        }
        return routes;
    }

    public static String getClassAnnotationValue(Class<?> aClass, @SuppressWarnings("rawtypes") Class annotationClass, String name) {
        @SuppressWarnings("unchecked") var aClassAnnotation = aClass.getAnnotation(annotationClass);
        if (aClassAnnotation != null) {
            var type = aClassAnnotation.annotationType();
            for (var method : type.getDeclaredMethods()) {
                try {
                    var value = method.invoke(aClassAnnotation);
                    if (method.getName().equals(name)) {
                        return value.toString();
                    }
                } catch (InvocationTargetException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return null;
    }

    public record AnnotatedRoute(ResourceLink.HttpMethod method, String path, List<String> queryParameters) {
        public String getUri() {
            var q = queryParameters.isEmpty() ? null : queryParameters.stream().collect(Collectors.toMap(v -> v, v -> String.format("{%s}", v)));
            return ResourceLinks.addPathPrefix(path) + ResourceLinks.toQuery(q);
        }

        public boolean hasSameHttpMethod(ResourceLink resource) {
            return method().equals(resource.getType());
        }

        public boolean matchesUrlTemplate(ResourceLink resource) {
            UriTemplate uriTemplate = new UriTemplate(getUri());
            var extractedTemplateValues = new HashMap<String, String>();
            return uriTemplate.match(resource.getHref().toString(), extractedTemplateValues) && extractedTemplateValues.keySet()
                .containsAll(queryParameters());
        }
    }
}

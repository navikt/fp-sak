package no.nav.foreldrepenger.web.app.tjenester.behandling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
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

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;

import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;

import no.nav.foreldrepenger.web.app.rest.ResourceLinks;

import org.glassfish.jersey.uri.UriTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingValg;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.beregnkontoer.UtregnetStønadskontoTjeneste;
import no.nav.foreldrepenger.domene.vedtak.TotrinnTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.app.rest.ResourceLink;
import no.nav.foreldrepenger.web.app.tjenester.RestImplementationClasses;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.UtvidetBehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving.TilbakekrevingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dokumentasjon.DokumentasjonVurderingBehovDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.fakta.FaktaUttakPeriodeDtoTjeneste;

@CdiDbAwareTest
class BehandlingDtoTjenesteTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private BeregningTjeneste beregningTjeneste;

    @Inject
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    @Inject
    private TilbakekrevingRepository tilbakekrevingRepository;

    @Inject
    private BehandlingDokumentRepository behandlingDokumentRepository;

    @Inject
    private ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste;

    @Inject
    private DokumentasjonVurderingBehovDtoTjeneste dokumentasjonVurderingBehovDtoTjeneste;

    @Inject
    private FaktaUttakPeriodeDtoTjeneste faktaUttakPeriodeDtoTjeneste;

    @Inject
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;

    @Inject
    private DekningsgradTjeneste dekningsgradTjeneste;

    @Inject
    private UttakTjeneste uttakTjeneste;

    @Inject
    private VergeRepository vergeRepository;

    private BehandlingDtoTjeneste tjeneste;

    private final LocalDate now = LocalDate.now();

    @BeforeEach
    public void setUp() {
        tjeneste = new BehandlingDtoTjeneste(repositoryProvider, beregningTjeneste, uttakTjeneste, tilbakekrevingRepository,
            skjæringstidspunktTjeneste, behandlingDokumentRepository, mock(TotrinnTjeneste.class), dokumentasjonVurderingBehovDtoTjeneste,
            faktaUttakPeriodeDtoTjeneste, fagsakRelasjonTjeneste,
            new UtregnetStønadskontoTjeneste(fagsakRelasjonTjeneste, foreldrepengerUttakTjeneste), dekningsgradTjeneste, vergeRepository);
    }

    @Test
    void skal_ha_med_tilbakekrevings_link_når_det_finnes_et_resultat() {
        var behandling = lagBehandling();

        tilbakekrevingRepository.lagre(behandling,
            TilbakekrevingValg.utenMulighetForInntrekk(TilbakekrevingVidereBehandling.OPPRETT_TILBAKEKREVING, "varsel"));

        var dto = tjeneste.lagUtvidetBehandlingDto(behandling, null);
        var link = ResourceLinks.get(TilbakekrevingRestTjeneste.VALG_PATH, "", new UuidDto(dto.getUuid()));
        assertThat(getLinkRel(dto)).contains("tilbakekrevingvalg");
        assertThat(getLinkHref(dto)).contains(link.getHref());
    }


    @Test
    void skal_ikke_ha_med_tilbakekrevings_link_når_det_ikke_finnes_et_resultat() {
        var behandling = lagBehandling();

        var dto = tjeneste.lagUtvidetBehandlingDto(behandling, null);
        var link = ResourceLinks.get(TilbakekrevingRestTjeneste.VALG_PATH, "", new UuidDto(dto.getUuid()));
        assertThat(getLinkRel(dto)).doesNotContain("tilbakekrevingvalg");
        assertThat(getLinkHref(dto)).doesNotContain(link.getHref());
    }

    @Test
    void alle_paths_skal_eksistere() {
        Set<Behandling> behandlinger = new HashSet<>();
        behandlinger.add(lagBehandling());
        behandlinger.add(lagBehandling(BehandlingType.KLAGE));
        behandlinger.add(lagBehandling(BehandlingType.ANKE));
        behandlinger.add(lagBehandling(BehandlingType.INNSYN));
        behandlinger.add(lagBehandling(FagsakYtelseType.ENGANGSTØNAD));
        behandlinger.add(lagBehandling(FagsakYtelseType.SVANGERSKAPSPENGER));

        var routes = getRoutes();
        for (var behandling : behandlinger) {
            for (var dtoLink : tjeneste.lagUtvidetBehandlingDto(behandling, null).getLinks()) {
                assertThat(dtoLink).isNotNull();
                assertThat(routes.stream().anyMatch(route -> route.hasSameHttpMethod(dtoLink) && route.matchesUrlTemplate(dtoLink))).withFailMessage(
                    "Route " + dtoLink + " does not exist.").isTrue();
            }
        }
    }


    private Behandling lagBehandling() {
        return ScenarioMorSøkerForeldrepenger.forFødsel()
                .medDefaultSøknadTerminbekreftelse()
                .medDefaultBekreftetTerminbekreftelse()
                .medFordeling(new OppgittFordelingEntitet(Collections.singletonList(OppgittPeriodeBuilder.ny()
                        .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
                        .medPeriode(now.plusWeeks(8), now.plusWeeks(12))
                        .build()), true))
                .lagre(repositoryProvider);
    }

    private Behandling lagBehandling(BehandlingType behandlingType) {
        return ScenarioMorSøkerForeldrepenger.forFødsel()
                .medDefaultSøknadTerminbekreftelse()
                .medDefaultBekreftetTerminbekreftelse()
                .medFordeling(new OppgittFordelingEntitet(Collections.singletonList(OppgittPeriodeBuilder.ny()
                        .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
                        .medPeriode(now.plusWeeks(8), now.plusWeeks(12))
                        .build()), true))
                .medBehandlingType(behandlingType)
                .lagre(repositoryProvider);
    }

    private Behandling lagBehandling(FagsakYtelseType fagsakYtelseType) {
        if (fagsakYtelseType.equals(FagsakYtelseType.SVANGERSKAPSPENGER)) {
            return ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger().lagre(repositoryProvider);
        }
        if (fagsakYtelseType.equals(FagsakYtelseType.ENGANGSTØNAD)) {
            var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
            scenario.medSøknadHendelse().erFødsel().build();
            return scenario.lagre(repositoryProvider);
        }
        return null;
    }

    private List<URI> getLinkHref(UtvidetBehandlingDto dto) {
        return dto.getLinks().stream().map(ResourceLink::getHref).toList();
    }

    private List<String> getLinkRel(UtvidetBehandlingDto dto) {
        return dto.getLinks().stream().map(ResourceLink::getRel).toList();
    }

    public Collection<AnnotatedRoute> getRoutes() {
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

                    List<String> pathParameters = Arrays.stream(aMethod.getParameters())
                        .map(p -> p.getDeclaredAnnotation(PathParam.class))
                        .filter(Objects::nonNull)
                        .map(PathParam::value)
                        .toList();

                    var path = pathFromClass + pathFromMethod;
                    routes.add(new AnnotatedRoute(method, path, pathParameters, queryParameters));
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

    public record AnnotatedRoute(ResourceLink.HttpMethod method, String path, List<String> pathParameters, List<String> queryParameters) {
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
                .containsAll(pathParameters()) && extractedTemplateValues.keySet().containsAll(queryParameters());
        }
    }
}

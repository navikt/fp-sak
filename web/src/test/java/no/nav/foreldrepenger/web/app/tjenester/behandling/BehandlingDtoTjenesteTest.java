package no.nav.foreldrepenger.web.app.tjenester.behandling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;

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
import no.nav.foreldrepenger.domene.uttak.beregnkontoer.UtregnetStønadskontoTjeneste;
import no.nav.foreldrepenger.domene.vedtak.TotrinnTjeneste;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.app.konfig.ApiConfig;
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

    private static final Environment ENV = Environment.current();

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

    private BehandlingDtoTjeneste tjeneste;

    private final LocalDate now = LocalDate.now();

    @BeforeEach
    public void setUp() {
        tjeneste = new BehandlingDtoTjeneste(repositoryProvider, beregningTjeneste, tilbakekrevingRepository, skjæringstidspunktTjeneste,
                behandlingDokumentRepository, foreldrepengerUttakTjeneste, mock(TotrinnTjeneste.class),
            dokumentasjonVurderingBehovDtoTjeneste, faktaUttakPeriodeDtoTjeneste, fagsakRelasjonTjeneste,
            new UtregnetStønadskontoTjeneste(fagsakRelasjonTjeneste, foreldrepengerUttakTjeneste), dekningsgradTjeneste);
    }

    @Test
    void skal_ha_med_tilbakekrevings_link_når_det_finnes_et_resultat() {
        var behandling = lagBehandling();

        tilbakekrevingRepository.lagre(behandling,
                TilbakekrevingValg.utenMulighetForInntrekk(TilbakekrevingVidereBehandling.OPPRETT_TILBAKEKREVING, "varsel"));

        var dto = tjeneste.lagUtvidetBehandlingDto(behandling, null);
        var link = ResourceLink.get(href(TilbakekrevingRestTjeneste.VALG_PATH), "", new UuidDto(dto.getUuid()));
        assertThat(getLinkRel(dto)).contains("tilbakekrevingvalg");
        assertThat(getLinkHref(dto)).contains(link.getHref());
    }

    private String href(String path) {
        return ENV.getProperty("context.path", "/fpsak") + ApiConfig.API_URI + path;
    }

    @Test
    void skal_ikke_ha_med_tilbakekrevings_link_når_det_ikke_finnes_et_resultat() {
        var behandling = lagBehandling();

        var dto = tjeneste.lagUtvidetBehandlingDto(behandling, null);
        var link = ResourceLink.get(href(TilbakekrevingRestTjeneste.VALG_PATH), "", new UuidDto(dto.getUuid()));
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
                assertThat(routeExists(dtoLink, routes)).withFailMessage("Route " + dtoLink + " does not exist.").isTrue();
            }
        }
    }

    private Boolean routeExists(ResourceLink dtoLink, Collection<ResourceLink> routes) {
        if (dtoLink.getRel().equals("simuleringResultat")) {
            return true;
        }
        for (var routeLink : routes) {
            if (dtoLink.getHref().getPath().equals(routeLink.getHref().getPath()) && dtoLink.getType().equals(routeLink.getType())) {
                return true;
            }
        }
        return false;
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

    public Collection<ResourceLink> getRoutes() {
        Set<ResourceLink> routes = new HashSet<>();
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
                    var resourceLink = new ResourceLink(href(pathFromClass) + pathFromMethod, aMethod.getName(), method);
                    routes.add(resourceLink);
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
}

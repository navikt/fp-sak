package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.util.List;

import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;

import no.nav.foreldrepenger.web.app.rest.ResourceLinks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;

import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingValg;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.beregnkontoer.UtregnetStønadskontoTjeneste;
import no.nav.foreldrepenger.domene.vedtak.TotrinnTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.app.rest.ResourceLink;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
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

    @BeforeEach
    public void setUp() {
        tjeneste = new BehandlingDtoTjeneste(repositoryProvider, beregningTjeneste, uttakTjeneste, tilbakekrevingRepository,
            skjæringstidspunktTjeneste, behandlingDokumentRepository, mock(TotrinnTjeneste.class), dokumentasjonVurderingBehovDtoTjeneste,
            faktaUttakPeriodeDtoTjeneste, fagsakRelasjonTjeneste,
            new UtregnetStønadskontoTjeneste(fagsakRelasjonTjeneste, foreldrepengerUttakTjeneste), dekningsgradTjeneste, vergeRepository);
    }

    @Test
    void skal_ha_med_tilbakekrevings_link_når_det_finnes_et_resultat() {
        var behandling = BehandlingDtoLenkeTestUtils.lagFPBehandling().lagre(repositoryProvider);

        tilbakekrevingRepository.lagre(behandling,
            TilbakekrevingValg.utenMulighetForInntrekk(TilbakekrevingVidereBehandling.OPPRETT_TILBAKEKREVING, "varsel"));

        var dto = tjeneste.lagUtvidetBehandlingDto(behandling, null);
        var link = ResourceLinks.get(TilbakekrevingRestTjeneste.VALG_PATH, "", new UuidDto(dto.getUuid()));
        assertThat(getLinkRel(dto)).contains("tilbakekrevingvalg");
        assertThat(getLinkHref(dto)).contains(link.getHref());
    }


    @Test
    void skal_ikke_ha_med_tilbakekrevings_link_når_det_ikke_finnes_et_resultat() {
        var behandling = BehandlingDtoLenkeTestUtils.lagFPBehandling().lagre(repositoryProvider);

        var dto = tjeneste.lagUtvidetBehandlingDto(behandling, null);
        var link = ResourceLinks.get(TilbakekrevingRestTjeneste.VALG_PATH, "", new UuidDto(dto.getUuid()));
        assertThat(getLinkRel(dto)).doesNotContain("tilbakekrevingvalg");
        assertThat(getLinkHref(dto)).doesNotContain(link.getHref());
    }

    @Test
    void alle_ressurslenker_skal_matche_annotert_restmetode() {
        var behandlinger = BehandlingDtoLenkeTestUtils.lagOgLagreBehandlinger(repositoryProvider);

        var routes = BehandlingDtoLenkeTestUtils.getRoutes();
        for (var behandling : behandlinger) {
            for (var dtoLink : tjeneste.lagUtvidetBehandlingDto(behandling, null).getLinks()) {
                assertThat(dtoLink).isNotNull();
                assertThat(routes.stream().anyMatch(route -> route.hasSameHttpMethod(dtoLink) && route.matchesUrlTemplate(dtoLink))).withFailMessage(
                    "Route " + dtoLink + " does not exist.").isTrue();
            }
        }
    }

    private List<URI> getLinkHref(UtvidetBehandlingDto dto) {
        return dto.getLinks().stream().map(ResourceLink::getHref).toList();
    }

    private List<String> getLinkRel(UtvidetBehandlingDto dto) {
        return dto.getLinks().stream().map(ResourceLink::getRel).toList();
    }
}

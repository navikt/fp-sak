package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.eøs.EøsUttakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.beregnkontoer.UtregnetStønadskontoTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;


@CdiDbAwareTest
class BehandlingFormidlingDtoTjenesteTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private BeregningTjeneste beregningTjeneste;

    @Inject
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    @Inject
    private BehandlingDokumentRepository behandlingDokumentRepository;

    @Inject
    private RelatertBehandlingTjeneste relatertBehandlingTjeneste;

    @Inject
    private DekningsgradTjeneste dekningsgradTjeneste;

    @Inject
    private UttakTjeneste uttakTjeneste;

    @Inject
    private UtregnetStønadskontoTjeneste utregnetStønadskontoTjeneste;

    @Inject
    private MedlemTjeneste medlemTjeneste;

    @Inject
    private VergeRepository vergeRepository;

    @Inject
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;

    @Inject
    private UføretrygdRepository uføretrygdRepository;

    @Inject
    private EøsUttakRepository eøsUttakRepository;

    private BehandlingFormidlingDtoTjeneste tjeneste;

    @BeforeEach
    void setUp() {
        tjeneste = new BehandlingFormidlingDtoTjeneste(repositoryProvider, beregningTjeneste, skjæringstidspunktTjeneste,
            behandlingDokumentRepository, relatertBehandlingTjeneste, uttakTjeneste, dekningsgradTjeneste, utregnetStønadskontoTjeneste,
            medlemTjeneste, vergeRepository, ytelseFordelingTjeneste, uføretrygdRepository, eøsUttakRepository);
    }


    @Test
    void alle_ressurslenker_skal_matche_annotert_restmetode() {
        var behandlinger = BehandlingDtoLenkeTestUtils.lagOgLagreBehandlinger(repositoryProvider);

        var routes = BehandlingDtoLenkeTestUtils.getRoutes();
        for (var behandling : behandlinger) {
            for (var dtoLink : tjeneste.lagDtoForFormidling(behandling).getLinks()) {
                assertThat(dtoLink).isNotNull();
                assertThat(routes.stream().anyMatch(route -> route.hasSameHttpMethod(dtoLink) && route.matchesUrlTemplate(dtoLink))).withFailMessage(
                    "Route " + dtoLink + " does not exist.").isTrue();
            }
        }
    }

}

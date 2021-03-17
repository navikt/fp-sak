package no.nav.foreldrepenger.økonomistøtte.es;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.person.pdl.AktørTjeneste;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.økonomistøtte.OppdragInputTjeneste;
import no.nav.foreldrepenger.økonomistøtte.SimulerOppdragTjeneste;
import no.nav.foreldrepenger.økonomistøtte.ny.mapper.LagOppdragTjeneste;
import no.nav.foreldrepenger.økonomistøtte.ny.tjeneste.NyOppdragskontrollTjenesteImpl;
import no.nav.foreldrepenger.økonomistøtte.ØkonomioppdragRepository;

@ExtendWith(MockitoExtension.class)
public class SimulerOppdragTjenesteTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private SimulerOppdragTjeneste simulerOppdragTjeneste;

    @Mock
    private AktørTjeneste aktørTjeneste;

    @BeforeEach
    public void setup() {
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);

        final ØkonomioppdragRepository økonomioppdragRepository = new ØkonomioppdragRepository(entityManager);
        final LegacyESBeregningRepository beregningRepository = new LegacyESBeregningRepository(entityManager);
        final BehandlingVedtakRepository behandlingVedtakRepository = new BehandlingVedtakRepository(entityManager);
        final FamilieHendelseRepository familieHendelseRepository = new FamilieHendelseRepository(entityManager);

        when(aktørTjeneste.hentPersonIdentForAktørId(any())).thenReturn(Optional.of(PersonIdent.fra("0987654321")));

        OppdragInputTjeneste oppdragInputTjeneste = new OppdragInputTjeneste(
            repositoryProvider.getBehandlingRepository(),
            null,
            behandlingVedtakRepository,
            familieHendelseRepository,
            new TilbakekrevingRepository(entityManager),
            aktørTjeneste, økonomioppdragRepository, beregningRepository);

        simulerOppdragTjeneste = new SimulerOppdragTjeneste(
            new NyOppdragskontrollTjenesteImpl(new LagOppdragTjeneste(), økonomioppdragRepository),
            oppdragInputTjeneste);
    }

    @Test
    public void simulerOppdrag_uten_behandling_vedtak_ES() {
        // Arrange
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        Behandling behandling = scenario.lagre(repositoryProvider);

        // Act
        var resultat = simulerOppdragTjeneste.simulerOppdrag(behandling.getId());

        // Assert
        assertThat(resultat).hasSize(0);
    }
}

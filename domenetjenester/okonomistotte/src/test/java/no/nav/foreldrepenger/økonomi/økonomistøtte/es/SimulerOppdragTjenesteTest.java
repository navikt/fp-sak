package no.nav.foreldrepenger.økonomi.økonomistøtte.es;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
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
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.økonomi.økonomistøtte.FinnNyesteOppdragForSak;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OppdragskontrollManagerFactory;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OppdragskontrollManagerFactoryProvider;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OppdragskontrollTjeneste;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OppdragskontrollTjenesteImpl;
import no.nav.foreldrepenger.økonomi.økonomistøtte.SimulerOppdragTjeneste;
import no.nav.foreldrepenger.økonomi.økonomistøtte.ØkonomioppdragRepository;
import no.nav.foreldrepenger.økonomi.økonomistøtte.kontantytelse.es.OppdragskontrollEngangsstønad;
import no.nav.foreldrepenger.økonomi.økonomistøtte.kontantytelse.es.adapter.MapBehandlingInfoES;
import no.nav.vedtak.felles.testutilities.db.Repository;

@ExtendWith(MockitoExtension.class)
public class SimulerOppdragTjenesteTest extends EntityManagerAwareTest {

    private static final PersonIdent PERSON_IDENT = PersonIdent.fra("12345678901");

    private BehandlingRepositoryProvider repositoryProvider;

    private ØkonomioppdragRepository økonomioppdragRepository;
    private FinnNyesteOppdragForSak finnNyesteOppdragForSak;

    private LegacyESBeregningRepository beregningRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private FamilieHendelseRepository familieHendelseRepository;

    private SimulerOppdragTjeneste simulerOppdragTjeneste;

    @Mock
    private PersoninfoAdapter tpsTjeneste;

    @BeforeEach
    public void setup() {
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        økonomioppdragRepository = new ØkonomioppdragRepository(entityManager);
        finnNyesteOppdragForSak = new FinnNyesteOppdragForSak(økonomioppdragRepository);
        beregningRepository = new LegacyESBeregningRepository(entityManager);
        behandlingVedtakRepository = new BehandlingVedtakRepository(entityManager);
        familieHendelseRepository = new FamilieHendelseRepository(entityManager);
        when(tpsTjeneste.hentFnrForAktør(any())).thenReturn(PERSON_IDENT);
        simulerOppdragTjeneste = new SimulerOppdragTjeneste(mockTjeneste());
    }

    private OppdragskontrollTjeneste mockTjeneste() {
        OppdragskontrollManagerFactory oppdragskontrollManagerFactory = mockFactoryES();
        OppdragskontrollManagerFactoryProvider providerMock = mock(OppdragskontrollManagerFactoryProvider.class);
        when(providerMock.getTjeneste(any(FagsakYtelseType.class))).thenReturn(oppdragskontrollManagerFactory);
        return new OppdragskontrollTjenesteImpl(repositoryProvider, økonomioppdragRepository, providerMock);
    }

    private OppdragskontrollManagerFactory mockFactoryES() {
        MapBehandlingInfoES mapBehandlingInfo = new MapBehandlingInfoES(finnNyesteOppdragForSak, tpsTjeneste,
            beregningRepository, behandlingVedtakRepository, familieHendelseRepository
        );
        var manager = new OppdragskontrollEngangsstønad(mapBehandlingInfo);
        OppdragskontrollManagerFactory oppdragskontrollManagerFactory = mock(OppdragskontrollManagerFactory.class);
        when(oppdragskontrollManagerFactory.getManager(any(), anyBoolean())).thenReturn(Optional.of(manager));
        return oppdragskontrollManagerFactory;
    }

    @Test
    public void simulerOppdrag_uten_behandling_vedtak_ES() {
        // Arrange
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        Behandling behandling = scenario.lagre(repositoryProvider);
        new Repository(getEntityManager()).lagre(behandling.getBehandlingsresultat());

        // Act
        var resultat = simulerOppdragTjeneste.simulerOppdrag(behandling.getId(), 0L);

        // Assert
        assertThat(resultat).hasSize(1);

    }
}

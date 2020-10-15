package no.nav.foreldrepenger.økonomi.økonomistøtte.es;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.finn.unleash.FakeUnleash;
import no.finn.unleash.Unleash;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.økonomi.økonomistøtte.FinnNyesteOppdragForSak;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OppdragskontrollManagerFactory;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OppdragskontrollManagerFactoryProvider;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OppdragskontrollTjeneste;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OppdragskontrollTjenesteImpl;
import no.nav.foreldrepenger.økonomi.økonomistøtte.SimulerOppdragApplikasjonTjeneste;
import no.nav.foreldrepenger.økonomi.økonomistøtte.kontantytelse.es.OppdragskontrollEngangsstønad;
import no.nav.foreldrepenger.økonomi.økonomistøtte.kontantytelse.es.adapter.MapBehandlingInfoES;
import no.nav.foreldrepenger.økonomi.økonomistøtte.ØkonomioppdragRepository;

public class SimulerOppdragApplikasjonTjenesteTest {

    private static final PersonIdent PERSON_IDENT = PersonIdent.fra("12345678901");

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();
    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final EntityManager entityManager = repoRule.getEntityManager();
    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);

    private ØkonomioppdragRepository økonomioppdragRepository = new ØkonomioppdragRepository(entityManager);
    private FinnNyesteOppdragForSak finnNyesteOppdragForSak = new FinnNyesteOppdragForSak(økonomioppdragRepository);

    private LegacyESBeregningRepository beregningRepository = new LegacyESBeregningRepository(entityManager);
    private BehandlingVedtakRepository behandlingVedtakRepository = new BehandlingVedtakRepository(entityManager);
    private FamilieHendelseRepository familieHendelseRepository = new FamilieHendelseRepository(entityManager);

    private SimulerOppdragApplikasjonTjeneste simulerOppdragApplikasjonTjeneste;
    private Unleash unleash = new FakeUnleash();

    @Mock
    private PersoninfoAdapter tpsTjeneste;

    @Before
    public void setup() {
        when(tpsTjeneste.hentFnrForAktør(any())).thenReturn(PERSON_IDENT);
        simulerOppdragApplikasjonTjeneste = new SimulerOppdragApplikasjonTjeneste(mockTjeneste());
    }

    private OppdragskontrollTjeneste mockTjeneste() {
        OppdragskontrollManagerFactory oppdragskontrollManagerFactory = mockFactoryES();
        OppdragskontrollManagerFactoryProvider providerMock = mock(OppdragskontrollManagerFactoryProvider.class);
        when(providerMock.getTjeneste(any(FagsakYtelseType.class))).thenReturn(oppdragskontrollManagerFactory);
        OppdragskontrollTjeneste oppdragskontrollTjeneste = new OppdragskontrollTjenesteImpl(repositoryProvider, økonomioppdragRepository, providerMock, unleash);
        return oppdragskontrollTjeneste;
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
        repoRule.getRepository().lagre(behandling.getBehandlingsresultat());

        // Act
        var resultat = simulerOppdragApplikasjonTjeneste.simulerOppdrag(behandling.getId(), 0L);

        // Assert
        assertThat(resultat).hasSize(1);

    }
}

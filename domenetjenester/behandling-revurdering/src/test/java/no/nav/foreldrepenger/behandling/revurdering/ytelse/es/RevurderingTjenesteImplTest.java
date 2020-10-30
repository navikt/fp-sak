package no.nav.foreldrepenger.behandling.revurdering.ytelse.es;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjenesteFelles;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingskontrollTjenesteImpl;
import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;

public class RevurderingTjenesteImplTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;
    private RevurderingTjeneste revurderingTjeneste;

    @BeforeEach
    public void setup() {
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
        var serviceProvider = new BehandlingskontrollServiceProvider(entityManager,
            new BehandlingModellRepository(), null);
        var revurderingEndringES = new RevurderingEndringImpl(behandlingRepository,
            new LegacyESBeregningRepository(entityManager));
        var vergeRepository = new VergeRepository(entityManager, new BehandlingLåsRepository(entityManager));
        var revurderingTjenesteFelles = new RevurderingTjenesteFelles(repositoryProvider);
        revurderingTjeneste = new RevurderingTjenesteImpl(repositoryProvider,
            new BehandlingskontrollTjenesteImpl(serviceProvider), revurderingEndringES, revurderingTjenesteFelles,
            vergeRepository);
    }

    @Test
    public void skal_opprette_automatisk_revurdering_basert_på_siste_innvilgede_behandling() {
        var behandlingSomSkalRevurderes = opprettRevurderingsKandidat();
        final Behandling revurdering = revurderingTjeneste
            .opprettAutomatiskRevurdering(behandlingSomSkalRevurderes.getFagsak(),
                BehandlingÅrsakType.RE_AVVIK_ANTALL_BARN, new OrganisasjonsEnhet("1234", "Test"));

        assertThat(revurdering.getFagsak()).isEqualTo(behandlingSomSkalRevurderes.getFagsak());
        assertThat(revurdering.getBehandlingÅrsaker().get(0).getBehandlingÅrsakType())
            .isEqualTo(BehandlingÅrsakType.RE_AVVIK_ANTALL_BARN);
    }

    @Test
    public void skal_opprette_manuell_behandling_med_saksbehandler_som_historikk_aktør() {
        var behandlingSomSkalRevurderes = opprettRevurderingsKandidat();
        OrganisasjonsEnhet enhet = new OrganisasjonsEnhet("4806", "Nye Nav FP");
        final Behandling revurdering = revurderingTjeneste
            .opprettManuellRevurdering(behandlingSomSkalRevurderes.getFagsak(),
                BehandlingÅrsakType.RE_MANGLER_FØDSEL_I_PERIODE, enhet);

        assertThat(revurdering.getFagsak()).isEqualTo(behandlingSomSkalRevurderes.getFagsak());
        assertThat(revurdering.getBehandlingÅrsaker().get(0).getBehandlingÅrsakType())
            .isEqualTo(BehandlingÅrsakType.RE_MANGLER_FØDSEL_I_PERIODE);
        assertThat(revurdering.getBehandlendeOrganisasjonsEnhet().getEnhetId()).isEqualTo(enhet.getEnhetId());
    }

    private Behandling opprettRevurderingsKandidat() {

        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medBehandlingVedtak().medVedtakstidspunkt(LocalDateTime.now())
            .medVedtakResultatType(VedtakResultatType.INNVILGET);
        scenario.buildAvsluttet(behandlingRepository, repositoryProvider);
        return scenario.getBehandling();
    }
}

package no.nav.foreldrepenger.behandling.revurdering.ytelse.es;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjenesteFelles;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingskontrollTjenesteImpl;
import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingGrunnlagRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.JpaExtension;

@ExtendWith(JpaExtension.class)
public class RevurderingTjenesteImplTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingGrunnlagRepositoryProvider grunnlagProvider;
    private BehandlingRepository behandlingRepository;
    private RevurderingTjeneste revurderingTjeneste;

    @BeforeEach
    public void setup(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        grunnlagProvider = new BehandlingGrunnlagRepositoryProvider(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
        var serviceProvider = new BehandlingskontrollServiceProvider(entityManager,
                new BehandlingModellRepository(), null);
        var revurderingEndringES = new RevurderingEndringImpl(behandlingRepository,
                new LegacyESBeregningRepository(entityManager), repositoryProvider.getBehandlingsresultatRepository());
        var vergeRepository = new VergeRepository(entityManager, new BehandlingLåsRepository(entityManager));
        var revurderingTjenesteFelles = new RevurderingTjenesteFelles(repositoryProvider);
        revurderingTjeneste = new RevurderingTjenesteImpl(behandlingRepository, grunnlagProvider,
                new BehandlingskontrollTjenesteImpl(serviceProvider), revurderingEndringES, revurderingTjenesteFelles,
                vergeRepository);
    }

    @Test
    public void skal_opprette_automatisk_revurdering_basert_på_siste_innvilgede_behandling() {
        var behandlingSomSkalRevurderes = opprettRevurderingsKandidat();
        final var revurdering = revurderingTjeneste
                .opprettAutomatiskRevurdering(behandlingSomSkalRevurderes.getFagsak(),
                        BehandlingÅrsakType.RE_AVVIK_ANTALL_BARN, new OrganisasjonsEnhet("1234", "Test"));

        assertThat(revurdering.getFagsak()).isEqualTo(behandlingSomSkalRevurderes.getFagsak());
        assertThat(revurdering.getBehandlingÅrsaker().get(0).getBehandlingÅrsakType())
                .isEqualTo(BehandlingÅrsakType.RE_AVVIK_ANTALL_BARN);
    }

    @Test
    public void skal_opprette_manuell_behandling_med_saksbehandler_som_historikk_aktør() {
        var behandlingSomSkalRevurderes = opprettRevurderingsKandidat();
        var enhet = new OrganisasjonsEnhet("4806", "Nye Nav FP");
        final var revurdering = revurderingTjeneste
                .opprettManuellRevurdering(behandlingSomSkalRevurderes.getFagsak(),
                        BehandlingÅrsakType.RE_MANGLER_FØDSEL_I_PERIODE, enhet);

        assertThat(revurdering.getFagsak()).isEqualTo(behandlingSomSkalRevurderes.getFagsak());
        assertThat(revurdering.getBehandlingÅrsaker().get(0).getBehandlingÅrsakType())
                .isEqualTo(BehandlingÅrsakType.RE_MANGLER_FØDSEL_I_PERIODE);
        assertThat(revurdering.getBehandlendeOrganisasjonsEnhet().enhetId()).isEqualTo(enhet.enhetId());
    }

    private Behandling opprettRevurderingsKandidat() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medBehandlingVedtak().medVedtakstidspunkt(LocalDateTime.now())
                .medVedtakResultatType(VedtakResultatType.INNVILGET);
        var behandling = scenario.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling.getId()));
        return behandling;
    }
}

package no.nav.foreldrepenger.behandling.revurdering.ytelse.es;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingEndring;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjenesteFelles;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingskontrollTjenesteImpl;
import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

@RunWith(CdiRunner.class)
public class RevurderingTjenesteImplTest {

    @Rule
    public final RepositoryRule repoRule = new UnittestRepositoryRule();

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private RevurderingTjeneste revurderingTjeneste;
    private RevurderingTjenesteFelles revurderingTjenesteFelles;
    private Behandling behandlingSomSkalRevurderes;

    @Inject
    private BehandlingskontrollServiceProvider serviceProvider;
    @Inject
    @FagsakYtelseTypeRef("ES")
    private RevurderingEndring revurderingEndringES;

    @Inject
    private VergeRepository vergeRepository;

    @Before
    public void setup() {
        opprettRevurderingsKandidat();
        revurderingTjenesteFelles = new RevurderingTjenesteFelles(repositoryProvider);
    }

    @Test
    public void skal_opprette_automatisk_revurdering_basert_på_siste_innvilgede_behandling() {
        final BehandlingskontrollTjenesteImpl behandlingskontrollTjeneste = new BehandlingskontrollTjenesteImpl(serviceProvider);
        revurderingTjeneste = new RevurderingTjenesteImpl(repositoryProvider, behandlingskontrollTjeneste, revurderingEndringES, revurderingTjenesteFelles, vergeRepository);
        final Behandling revurdering = revurderingTjeneste.opprettAutomatiskRevurdering(behandlingSomSkalRevurderes.getFagsak(), BehandlingÅrsakType.RE_AVVIK_ANTALL_BARN, new OrganisasjonsEnhet("1234", "Test"));

        assertThat(revurdering.getFagsak()).isEqualTo(behandlingSomSkalRevurderes.getFagsak());
        assertThat(revurdering.getBehandlingÅrsaker().get(0).getBehandlingÅrsakType()).isEqualTo(BehandlingÅrsakType.RE_AVVIK_ANTALL_BARN);
    }

    @Test
    public void skal_opprette_manuell_behandling_med_saksbehandler_som_historikk_aktør() {
        OrganisasjonsEnhet enhet = new OrganisasjonsEnhet("4806", "Nye Nav FP");
        final BehandlingskontrollTjenesteImpl behandlingskontrollTjeneste = new BehandlingskontrollTjenesteImpl(serviceProvider);
        revurderingTjeneste = new RevurderingTjenesteImpl(repositoryProvider, behandlingskontrollTjeneste, revurderingEndringES, revurderingTjenesteFelles, vergeRepository);
        final Behandling revurdering = revurderingTjeneste.opprettManuellRevurdering(behandlingSomSkalRevurderes.getFagsak(), BehandlingÅrsakType.RE_MANGLER_FØDSEL_I_PERIODE, enhet);

        assertThat(revurdering.getFagsak()).isEqualTo(behandlingSomSkalRevurderes.getFagsak());
        assertThat(revurdering.getBehandlingÅrsaker().get(0).getBehandlingÅrsakType()).isEqualTo(BehandlingÅrsakType.RE_MANGLER_FØDSEL_I_PERIODE);
        assertThat(revurdering.getBehandlendeOrganisasjonsEnhet().getEnhetId()).isEqualTo(enhet.getEnhetId());
    }

    private void opprettRevurderingsKandidat() {

        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.now())
            .medVedtakResultatType(VedtakResultatType.INNVILGET);
        scenario.buildAvsluttet(behandlingRepository, repositoryProvider);
        repoRule.getRepository().flushAndClear();
        behandlingSomSkalRevurderes = repoRule.getEntityManager().find(Behandling.class, scenario.getBehandling().getId());
    }
}

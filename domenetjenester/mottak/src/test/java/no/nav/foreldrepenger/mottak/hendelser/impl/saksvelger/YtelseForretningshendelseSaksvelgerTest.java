package no.nav.foreldrepenger.mottak.hendelser.impl.saksvelger;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.mottak.hendelser.saksvelger.YtelseForretningshendelseSaksvelger;
import no.nav.foreldrepenger.mottak.ytelse.YtelseForretningshendelse;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.util.FPDateUtil;

@RunWith(CdiRunner.class)
public class YtelseForretningshendelseSaksvelgerTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Inject
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    private YtelseForretningshendelseSaksvelger saksvelger;

    private Behandling behandling;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        saksvelger = new YtelseForretningshendelseSaksvelger(repositoryProvider);
    }

    @Test
    public void skal_oppdatere_fagsak_med_registeropplysninger_når_sak_er_under_behandling() {
        // Arrange
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        behandling = scenario.lagre(repositoryProvider);
        Fagsak fagsak = behandling.getFagsak();
        YtelseForretningshendelse ytelseForretningshendelse = new YtelseForretningshendelse(ForretningshendelseType.YTELSE_ENDRET, fagsak.getAktørId().getId(), FPDateUtil.iDag());

        // Act
        Map<BehandlingÅrsakType, List<Fagsak>> fagsaker = saksvelger.finnRelaterteFagsaker(ytelseForretningshendelse);

        // Assert
        assertThat(fagsaker.entrySet().stream().findFirst().get().getValue().stream().map(Fagsak::getId).collect(toList()))
            .containsExactly(fagsak.getId());
    }
}

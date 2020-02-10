package no.nav.foreldrepenger.dokumentbestiller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collections;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.util.FPDateUtil;

@RunWith(CdiRunner.class)
public class DokumentBehandlingTjenesteImplTest {
    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    @Mock
    BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    private AbstractTestScenario<?> scenario;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    private Behandling behandling;
    private BehandlingRepository behandlingRepository;
    private int fristUker = 6;
    private LocalDate now = LocalDate.now();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        dokumentBehandlingTjeneste = new DokumentBehandlingTjeneste(repositoryProvider, null, behandlingskontrollTjeneste, null, null);
        this.scenario = ScenarioMorSøkerEngangsstønad
            .forFødsel()
            .medFødselAdopsjonsdato(Collections.singletonList(LocalDate.now().minusDays(3)));
    }

    @Test
    public void skal_finne_behandlingsfrist_fra_manuel() {
        lagBehandling();
        assertThat(dokumentBehandlingTjeneste.finnNyFristManuelt(behandling))
            .isEqualTo(FPDateUtil.iDag().plusWeeks(fristUker));
    }

    @Test
    public void skal_finne_behandlingsfrist_fra_manuelt_medlemskap_ingen_terminbekreftelse() {
        lagBehandling();
        assertThat(dokumentBehandlingTjeneste.utledFristMedlemskap(behandling, Period.ofWeeks(3)))
            .isEqualTo(FPDateUtil.iDag().plusWeeks(fristUker));
    }

    @Test
    public void skal_finne_behandlingsfrist_fra_manuelt_medlemskap_med_terminbekreftelse() {
        this.scenario = ScenarioMorSøkerEngangsstønad
            .forFødsel()
            .medFødselAdopsjonsdato(Collections.singletonList(LocalDate.now().minusDays(3)))
            .medDefaultBekreftetTerminbekreftelse();
        lagBehandling();
        Period aksjonspunktPeriode = Period.ofWeeks(3);
        assertThat(dokumentBehandlingTjeneste.utledFristMedlemskap(behandling, aksjonspunktPeriode))
            .isEqualTo(LocalDate.now().plusWeeks(fristUker));
    }

    @Test
    public void skal_finne_behandlingsfrist_fra_manuelt_medlemskap_med_terminbekreftelse_etter_ap() {
        this.scenario = ScenarioMorSøkerEngangsstønad
            .forFødsel()
            .medFødselAdopsjonsdato(Collections.singletonList(LocalDate.now().minusDays(3)))
            .medDefaultBekreftetTerminbekreftelse();
        this.fristUker = BehandlingType.FØRSTEGANGSSØKNAD.getBehandlingstidFristUker();
        lagBehandling();
        Period aksjonspunktPeriode = Period.ofWeeks(0);
        LocalDate termindato = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId()).getGjeldendeTerminbekreftelse().get().getTermindato();
        assertThat(dokumentBehandlingTjeneste.utledFristMedlemskap(behandling, aksjonspunktPeriode))
            .isEqualTo(termindato.plus(aksjonspunktPeriode));
    }

    @Test
    public void skal_finne_behandlingsfrist_fra_manuelt_medlemskap_med_terminbekreftelse_i_fortiden() {
        this.scenario = ScenarioMorSøkerEngangsstønad
            .forFødsel()
            .medFødselAdopsjonsdato(Collections.singletonList(LocalDate.now().minusDays(3)));
        scenario.medBekreftetHendelse().medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
            .medTermindato(LocalDate.now().minusWeeks(6)));
        lagBehandling();
        Period aksjonspunktPeriode = Period.ofWeeks(3);
        assertThat(dokumentBehandlingTjeneste.utledFristMedlemskap(behandling, aksjonspunktPeriode))
            .isEqualTo(LocalDate.now().plusWeeks(fristUker));
    }

    private void lagBehandling() {
        behandling = Mockito.spy(scenario.medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD).lagre(repositoryProvider));
    }

    @Test
    public void skal_lagre_ny_frist() {
        behandling = scenario.lagre(repositoryProvider);
        dokumentBehandlingTjeneste.oppdaterBehandlingMedNyFrist(behandling, now);
        assertThat(behandlingRepository.hentBehandling(behandling.getId()).getBehandlingstidFrist()).isEqualTo(now);
    }
}

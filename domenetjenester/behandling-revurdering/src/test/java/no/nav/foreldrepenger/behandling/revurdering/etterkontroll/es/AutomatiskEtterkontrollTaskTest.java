package no.nav.foreldrepenger.behandling.revurdering.etterkontroll.es;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Collections;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.task.AutomatiskEtterkontrollTask;
import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.etterkontroll.EtterkontrollRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ExtendWith(MockitoExtension.class)
@ExtendWith(JpaExtension.class)
class AutomatiskEtterkontrollTaskTest {

    @Mock
    private PersoninfoAdapter tpsFamilieTjenesteMock;

    private BehandlingRepository behandlingRepository;

    @Mock
    private ProsessTaskTjeneste taskTjenesteMock;

    private AutomatiskEtterkontrollTask task;
    @Mock
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    private BehandlingRepositoryProvider repositoryProvider;

    private EtterkontrollRepository etterkontrollRepository;

    private FamilieHendelseTjeneste familieHendelseTjeneste;

    private LegacyESBeregningRepository legacyESBeregningRepository;

    @Mock
    private HistorikkRepository historikkRepository;

    private EntityManager em;

    @BeforeEach
    public void setUp(EntityManager entityManager) {
        em = entityManager;
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        legacyESBeregningRepository = new LegacyESBeregningRepository(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        etterkontrollRepository = new EtterkontrollRepository(entityManager);
        familieHendelseTjeneste = new FamilieHendelseTjeneste(null, repositoryProvider.getFamilieHendelseRepository());
        task = new AutomatiskEtterkontrollTask(repositoryProvider, etterkontrollRepository, historikkRepository,
                familieHendelseTjeneste, tpsFamilieTjenesteMock,
                taskTjenesteMock, behandlendeEnhetTjeneste);
        lenient().when(behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(any(Fagsak.class)))
                .thenReturn(new OrganisasjonsEnhet("1234", "Testlokasjon"));
    }

    @Test
    void skal_opprette_revurderingsbehandling_med_årsak_fødsel_mangler_dersom_fødsel_mangler_i_tps() {

        var behandling = opprettRevurderingsKandidat(4, 1, true, false, true);
        when(tpsFamilieTjenesteMock.innhentAlleFødteForBehandlingIntervaller(any(), any())).thenReturn(
                Collections.emptyList());

        var prosessTaskData = ProsessTaskData.forProsessTask(AutomatiskEtterkontrollTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setSekvens("1");

        createTask();
        task.doTask(prosessTaskData);

        assertRevurdering(behandling, BehandlingÅrsakType.RE_MANGLER_FØDSEL);
    }

    private void assertRevurdering(Behandling behandling, BehandlingÅrsakType behandlingÅrsakType) {
        var revurdering = behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(
                behandling.getFagsakId(),
                BehandlingType.REVURDERING);
        assertThat(revurdering).as("Ingen revurdering").isPresent();
        var behandlingÅrsaker = revurdering.get().getBehandlingÅrsaker();
        assertThat(behandlingÅrsaker).isNotEmpty();
        var årsaker = behandlingÅrsaker.stream()
                .map(BehandlingÅrsak::getBehandlingÅrsakType)
                .toList();
        assertThat(årsaker).contains(behandlingÅrsakType);
    }

    private void assertIngenRevurdering(Behandling behandling) {
        var revurdering = behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(
                behandling.getFagsakId(),
                BehandlingType.REVURDERING)
            .filter(b -> !b.harBehandlingÅrsak(BehandlingÅrsakType.RE_SATS_REGULERING));
        assertThat(revurdering).as("Har revurdering: " + behandling).isNotPresent();
    }

    private void createTask() {
        Period.parse("P11W");
        task = new AutomatiskEtterkontrollTask(repositoryProvider, etterkontrollRepository, historikkRepository,
                familieHendelseTjeneste, tpsFamilieTjenesteMock,
                taskTjenesteMock, behandlendeEnhetTjeneste);
    }

    @Test
    void skal_opprette_revurderingsbehandling_med_årsak_fødsel_mangler_i_periode_dersom_fødsel_mangler_i_tps_og_vedtaksdato_er_før_uke29() {

        var behandling = opprettRevurderingsKandidat(12, 1, true, false, true);
        when(tpsFamilieTjenesteMock.innhentAlleFødteForBehandlingIntervaller(any(), any())).thenReturn(
                Collections.emptyList());

        var prosessTaskData = ProsessTaskData.forProsessTask(AutomatiskEtterkontrollTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setSekvens("1");

        createTask();
        task.doTask(prosessTaskData);

        assertRevurdering(behandling, BehandlingÅrsakType.RE_MANGLER_FØDSEL_I_PERIODE);

    }

    @Test
    void skal_opprette_revurderingsbehandling_med_årsak_avvik_antall_barn_dersom_TPS_returnere_ulikt_antall_barn() {

        var barn = Collections.singletonList(byggBaby(LocalDate.now().minusDays(70)));
        var behandling = opprettRevurderingsKandidat(0, 2, true, false, true);
        when(tpsFamilieTjenesteMock.innhentAlleFødteForBehandlingIntervaller(any(), any())).thenReturn(barn);

        var prosessTaskData = ProsessTaskData.forProsessTask(AutomatiskEtterkontrollTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setSekvens("1");

        createTask();
        task.doTask(prosessTaskData);

        assertRevurdering(behandling, BehandlingÅrsakType.RE_AVVIK_ANTALL_BARN);
    }

    @Test
    void skal_registrere_fødsler_dersom_de_oppdages_i_tps() {
        var barn = Collections.singletonList(byggBaby(LocalDate.now().minusDays(70)));
        var behandling = opprettRevurderingsKandidat(0, 2, true, false, true);
        when(tpsFamilieTjenesteMock.innhentAlleFødteForBehandlingIntervaller(any(), any())).thenReturn(barn);

        var prosessTaskData = ProsessTaskData.forProsessTask(AutomatiskEtterkontrollTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setSekvens("1");

        createTask();
        task.doTask(prosessTaskData);

        assertRevurdering(behandling, BehandlingÅrsakType.RE_AVVIK_ANTALL_BARN);

    }

    @Test
    void skal_ikke_opprette_revurdering_dersom_barn_i_tps_matcher_søknad() {
        var barn = Collections.singletonList(byggBaby(LocalDate.now().minusDays(70)));
        var behandling = opprettRevurderingsKandidat(0, 1, true, false, true);
        when(tpsFamilieTjenesteMock.innhentAlleFødteForBehandlingIntervaller(any(), any())).thenReturn(barn);

        var prosessTaskData = ProsessTaskData.forProsessTask(AutomatiskEtterkontrollTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setSekvens("1");

        createTask();
        task.doTask(prosessTaskData);

        assertIngenRevurdering(behandling);
    }

    @Test
    void skal_ikke_opprette_revurdering_dersom_barn_i_tps_matcher_søknad_og_bekreftet() {
        var barn = Collections.singletonList(byggBaby(LocalDate.now().minusDays(70)));
        var behandling = opprettRevurderingsKandidat(0, 1, true, true, false);
        when(tpsFamilieTjenesteMock.innhentAlleFødteForBehandlingIntervaller(any(), any())).thenReturn(barn);

        var prosessTaskData = ProsessTaskData.forProsessTask(AutomatiskEtterkontrollTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setSekvens("1");

        createTask();
        task.doTask(prosessTaskData);

        assertIngenRevurdering(behandling);
    }

    @Test
    void skal_opprette_vurder_konsekvens_oppgave_hvis_det_finnes_åpen_førstegangs_behandling() {
        var behandling = opprettRevurderingsKandidat(0, 2, false, false, false);

        var prosessTaskData = ProsessTaskData.forProsessTask(AutomatiskEtterkontrollTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setSekvens("1");

        createTask();
        task.doTask(prosessTaskData);
        assertIngenRevurdering(behandling);
    }

    private Behandling opprettRevurderingsKandidat(int fødselUkerFørTermin,
            int antallBarn,
            boolean avsluttet,
            boolean medBekreftet,
            boolean medOverstyrt) {
        var terminDato = LocalDate.now().minusDays(70);

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel()
                .medSøknadDato(terminDato.minusDays(20));
        scenario.medSøknadHendelse()
                .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                        .medNavnPå("Lege Legesen")
                        .medTermindato(terminDato)
                        .medUtstedtDato(terminDato.minusDays(40)))
                .medAntallBarn(antallBarn);

        if (medBekreftet) {
            scenario.medBekreftetHendelse()
                    .medFødselsDato(terminDato)
                    .erFødsel()
                    .medAntallBarn(antallBarn);
        }

        if (medOverstyrt) {
            scenario.medOverstyrtHendelse()
                    .medTerminbekreftelse(scenario.medOverstyrtHendelse().getTerminbekreftelseBuilder()
                            .medNavnPå("Lege Legesen")
                            .medTermindato(terminDato)
                            .medUtstedtDato(terminDato.minusDays(40)))
                    .medAntallBarn(antallBarn);
        }

        var behandling = scenario.lagre(repositoryProvider);

        if (!avsluttet) {
            return behandling;
        }

        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET);

        behandling.avsluttBehandling();

        var lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);

        var vedtak = BehandlingVedtak.builder()
                .medVedtakResultatType(VedtakResultatType.INNVILGET)
                .medVedtakstidspunkt(terminDato.minusWeeks(fødselUkerFørTermin).atStartOfDay())
                .medBehandlingsresultat(behandling.getBehandlingsresultat())
                .medAnsvarligSaksbehandler("Severin Saksbehandler")
                .build();

        repositoryProvider.getBehandlingVedtakRepository().lagre(vedtak, lås);

        var sats = legacyESBeregningRepository.finnEksaktSats(BeregningSatsType.ENGANG, LocalDate.now()).getVerdi();
        var beregning = new LegacyESBeregning(sats, antallBarn, antallBarn * sats, LocalDateTime.now());
        var beregningResultat = LegacyESBeregningsresultat.builder()
                .medBeregning(beregning)
                .buildFor(behandling, behandling.getBehandlingsresultat());
        legacyESBeregningRepository.lagre(beregningResultat, lås);
        em.flush();
        em.clear();

        return behandlingRepository.hentBehandling(behandling.getId());
    }

    private FødtBarnInfo byggBaby(LocalDate fødselsdato) {
        return new FødtBarnInfo.Builder()
                .medFødselsdato(fødselsdato)
                .medIdent(PersonIdent.fra("12345678901"))
                .build();
    }

}

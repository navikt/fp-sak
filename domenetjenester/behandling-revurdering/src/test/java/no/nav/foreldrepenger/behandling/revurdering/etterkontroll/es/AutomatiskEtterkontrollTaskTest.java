package no.nav.foreldrepenger.behandling.revurdering.etterkontroll.es;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.EtterkontrollRepository;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.task.AutomatiskEtterkontrollTask;
import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
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
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.person.tps.TpsFamilieTjeneste;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;
import no.nav.vedtak.felles.testutilities.db.Repository;

@ExtendWith(MockitoExtension.class)
@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class AutomatiskEtterkontrollTaskTest extends EntityManagerAwareTest {

    @Mock
    private TpsFamilieTjeneste tpsFamilieTjenesteMock;

    private BehandlingRepository behandlingRepository;

    @Mock
    private ProsessTaskRepository prosessTaskRepositoryMock;

    private AutomatiskEtterkontrollTask task;
    @Mock
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    private BehandlingRepositoryProvider repositoryProvider;

    private EtterkontrollRepository etterkontrollRepository;

    private FamilieHendelseTjeneste familieHendelseTjeneste;

    private LegacyESBeregningRepository legacyESBeregningRepository;

    @Mock
    private HistorikkRepository historikkRepository;

    private Repository repo;

    @BeforeEach
    public void setUp() {
        repo = new Repository(getEntityManager());
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        legacyESBeregningRepository = new LegacyESBeregningRepository(getEntityManager());
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        etterkontrollRepository = new EtterkontrollRepository(getEntityManager());
        familieHendelseTjeneste = new FamilieHendelseTjeneste(null, repositoryProvider.getFamilieHendelseRepository());
        task = new AutomatiskEtterkontrollTask(repositoryProvider, etterkontrollRepository, historikkRepository, familieHendelseTjeneste, tpsFamilieTjenesteMock,
                prosessTaskRepositoryMock, behandlendeEnhetTjeneste);
        lenient().when(behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(any(Fagsak.class)))
                .thenReturn(new OrganisasjonsEnhet("1234", "Testlokasjon"));
    }

    @Test
    public void skal_opprette_revurderingsbehandling_med_årsak_fødsel_mangler_dersom_fødsel_mangler_i_tps() {

        Behandling behandling = opprettRevurderingsKandidat(4, 1, true, false, true);
        when(tpsFamilieTjenesteMock.getFødslerRelatertTilBehandling(any(), any())).thenReturn(Collections.emptyList());

        ProsessTaskData prosessTaskData = new ProsessTaskData(AutomatiskEtterkontrollTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setSekvens("1");

        createTask();
        task.doTask(prosessTaskData);

        assertRevurdering(behandling, BehandlingÅrsakType.RE_MANGLER_FØDSEL);
    }

    private void assertRevurdering(Behandling behandling, BehandlingÅrsakType behandlingÅrsakType) {
        Optional<Behandling> revurdering = behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(behandling.getFagsakId(),
                BehandlingType.REVURDERING);
        assertThat(revurdering).as("Ingen revurdering").isPresent();
        List<BehandlingÅrsak> behandlingÅrsaker = revurdering.get().getBehandlingÅrsaker();
        assertThat(behandlingÅrsaker).isNotEmpty();
        List<BehandlingÅrsakType> årsaker = behandlingÅrsaker.stream().map(bå -> bå.getBehandlingÅrsakType()).collect(Collectors.toList());
        assertThat(årsaker).contains(behandlingÅrsakType);
    }

    private void assertIngenRevurdering(Behandling behandling) {
        Optional<Behandling> revurdering = behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(behandling.getFagsakId(),
                BehandlingType.REVURDERING);
        assertThat(revurdering).as("Har revurdering: " + behandling).isNotPresent();
    }

    private void createTask() {
        Period etterkontrollTpsRegistreringPeriode = Period.parse("P11W");
        task = new AutomatiskEtterkontrollTask(repositoryProvider, etterkontrollRepository, historikkRepository, familieHendelseTjeneste, tpsFamilieTjenesteMock,
            prosessTaskRepositoryMock, behandlendeEnhetTjeneste);
    }

    @Test
    public void skal_opprette_revurderingsbehandling_med_årsak_fødsel_mangler_i_periode_dersom_fødsel_mangler_i_tps_og_vedtaksdato_er_før_uke29() {

        Behandling behandling = opprettRevurderingsKandidat(12, 1, true, false, true);
        when(tpsFamilieTjenesteMock.getFødslerRelatertTilBehandling(any(), any())).thenReturn(Collections.emptyList());

        ProsessTaskData prosessTaskData = new ProsessTaskData(AutomatiskEtterkontrollTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setSekvens("1");

        createTask();
        task.doTask(prosessTaskData);

        assertRevurdering(behandling, BehandlingÅrsakType.RE_MANGLER_FØDSEL_I_PERIODE);

    }

    @Test
    public void skal_opprette_revurderingsbehandling_med_årsak_avvik_antall_barn_dersom_TPS_returnere_ulikt_antall_barn() {

        List<FødtBarnInfo> barn = Collections.singletonList(byggBaby(LocalDate.now().minusDays(70)));
        Behandling behandling = opprettRevurderingsKandidat(0, 2, true, false, true);
        when(tpsFamilieTjenesteMock.getFødslerRelatertTilBehandling(any(), any())).thenReturn(barn);

        ProsessTaskData prosessTaskData = new ProsessTaskData(AutomatiskEtterkontrollTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setSekvens("1");

        createTask();
        task.doTask(prosessTaskData);

        assertRevurdering(behandling, BehandlingÅrsakType.RE_AVVIK_ANTALL_BARN);
    }

    @Test
    public void skal_registrere_fødsler_dersom_de_oppdages_i_tps() {
        List<FødtBarnInfo> barn = Collections.singletonList(byggBaby(LocalDate.now().minusDays(70)));
        Behandling behandling = opprettRevurderingsKandidat(0, 2, true, false, true);
        when(tpsFamilieTjenesteMock.getFødslerRelatertTilBehandling(any(), any())).thenReturn(barn);

        ProsessTaskData prosessTaskData = new ProsessTaskData(AutomatiskEtterkontrollTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setSekvens("1");

        createTask();
        task.doTask(prosessTaskData);

        assertRevurdering(behandling, BehandlingÅrsakType.RE_AVVIK_ANTALL_BARN);

    }

    @Test
    public void skal_ikke_opprette_revurdering_dersom_barn_i_tps_matcher_søknad() {
        List<FødtBarnInfo> barn = Collections.singletonList(byggBaby(LocalDate.now().minusDays(70)));
        Behandling behandling = opprettRevurderingsKandidat(0, 1, true, false, true);
        when(tpsFamilieTjenesteMock.getFødslerRelatertTilBehandling(any(), any())).thenReturn(barn);

        ProsessTaskData prosessTaskData = new ProsessTaskData(AutomatiskEtterkontrollTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setSekvens("1");

        createTask();
        task.doTask(prosessTaskData);

        assertIngenRevurdering(behandling);
    }

    @Test
    public void skal_ikke_opprette_revurdering_dersom_barn_i_tps_matcher_søknad_og_bekreftet() {
        List<FødtBarnInfo> barn = Collections.singletonList(byggBaby(LocalDate.now().minusDays(70)));
        Behandling behandling = opprettRevurderingsKandidat(0, 1, true, true, false);
        when(tpsFamilieTjenesteMock.getFødslerRelatertTilBehandling(any(), any())).thenReturn(barn);

        ProsessTaskData prosessTaskData = new ProsessTaskData(AutomatiskEtterkontrollTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setSekvens("1");

        createTask();
        task.doTask(prosessTaskData);

        assertIngenRevurdering(behandling);
    }

    @Test
    public void skal_opprette_vurder_konsekvens_oppgave_hvis_det_finnes_åpen_førstegangs_behandling() {
        Behandling behandling = opprettRevurderingsKandidat(0, 2, false, false, false);

        ProsessTaskData prosessTaskData = new ProsessTaskData(AutomatiskEtterkontrollTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setSekvens("1");

        createTask();
        task.doTask(prosessTaskData);
        assertIngenRevurdering(behandling);
    }

    private Behandling opprettRevurderingsKandidat(int fødselUkerFørTermin, int antallBarn, boolean avsluttet, boolean medBekreftet,
            boolean medOverstyrt) {
        LocalDate terminDato = LocalDate.now().minusDays(70);

        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel()
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

        Behandling behandling = scenario.lagre(repositoryProvider);

        if (!avsluttet) {
            return behandling;
        }

        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET);

        behandling.avsluttBehandling();

        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);

        BehandlingVedtak vedtak = BehandlingVedtak.builder()
                .medVedtakResultatType(VedtakResultatType.INNVILGET)
                .medVedtakstidspunkt(terminDato.minusWeeks(fødselUkerFørTermin).atStartOfDay())
                .medBehandlingsresultat(behandling.getBehandlingsresultat())
                .medAnsvarligSaksbehandler("Severin Saksbehandler")
                .build();

        repositoryProvider.getBehandlingVedtakRepository().lagre(vedtak, lås);

        var sats = legacyESBeregningRepository.finnEksaktSats(BeregningSatsType.ENGANG, LocalDate.now()).getVerdi();
        LegacyESBeregning beregning = new LegacyESBeregning(sats, antallBarn, antallBarn*sats, LocalDateTime.now());
        LegacyESBeregningsresultat beregningResultat = LegacyESBeregningsresultat.builder().medBeregning(beregning).buildFor(behandling, behandling.getBehandlingsresultat());
        legacyESBeregningRepository.lagre(beregningResultat, lås);

        repo.flushAndClear();

        return getEntityManager().find(Behandling.class, behandling.getId());
    }

    private FødtBarnInfo byggBaby(LocalDate fødselsdato) {
        return new FødtBarnInfo.Builder()
                .medFødselsdato(fødselsdato)
                .medIdent(PersonIdent.fra("12345678901"))
                .medNavn("barn")
                .medNavBrukerKjønn(NavBrukerKjønn.MANN)
                .build();
    }

}

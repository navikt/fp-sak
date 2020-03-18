package no.nav.foreldrepenger.behandling.revurdering.etterkontroll.es;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.EtterkontrollRepository;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.task.AutomatiskEtterkontrollTask;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.tjeneste.EtterkontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.person.tps.TpsFamilieTjeneste;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.testutilities.Whitebox;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@SuppressWarnings("deprecation")
@RunWith(CdiRunner.class)
public class AutomatiskEtterkontrollTaskTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private TpsFamilieTjeneste tpsFamilieTjenesteMock;

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    @FagsakYtelseTypeRef("ES")
    private RevurderingTjeneste revurderingTjenesteMock;

    @Inject
    private ProsessTaskRepository prosessTaskRepositoryMock;

    private AutomatiskEtterkontrollTask task;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private EtterkontrollRepository etterkontrollRepository;

    @Inject
    private ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste;

    EtterkontrollTjeneste etterkontrollTjeneste;

    private HistorikkRepository historikkRepository;

    @Before
    public void setUp() {
        tpsFamilieTjenesteMock = mock(TpsFamilieTjeneste.class);
        behandlendeEnhetTjeneste = mock(BehandlendeEnhetTjeneste.class);
        var bkTjeneste = mock(BehandlingskontrollTjeneste.class);
        when(behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(any(Fagsak.class))).thenReturn(new OrganisasjonsEnhet("1234", "Testlokasjon"));
        etterkontrollTjeneste = new EtterkontrollTjeneste(repositoryProvider,prosessTaskRepositoryMock, bkTjeneste, foreldrepengerUttakTjeneste);
        this.historikkRepository = mock(HistorikkRepository.class);

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
        Optional<Behandling> revurdering = behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(behandling.getFagsakId(), BehandlingType.REVURDERING);
        assertThat(revurdering).as("Ingen revurdering").isPresent();
        List<BehandlingÅrsak> behandlingÅrsaker = revurdering.get().getBehandlingÅrsaker();
        assertThat(behandlingÅrsaker).isNotEmpty();
        List<BehandlingÅrsakType> årsaker = behandlingÅrsaker.stream().map(bå -> bå.getBehandlingÅrsakType()).collect(Collectors.toList());
        assertThat(årsaker).contains(behandlingÅrsakType);
    }

    private void assertIngenRevurdering(Behandling behandling) {
        Optional<Behandling> revurdering = behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(behandling.getFagsakId(), BehandlingType.REVURDERING);
        assertThat(revurdering).as("Har revurdering: " + behandling).isNotPresent();
    }

    private void createTask() {
        Period etterkontrollTpsRegistreringPeriode = Period.parse("P11W");

        task = new AutomatiskEtterkontrollTask(repositoryProvider,
            etterkontrollRepository,
            historikkRepository, tpsFamilieTjenesteMock,
            prosessTaskRepositoryMock, etterkontrollTpsRegistreringPeriode, behandlendeEnhetTjeneste,
            etterkontrollTjeneste);

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

    private Behandling opprettRevurderingsKandidat(int fødselUkerFørTermin, int antallBarn, boolean avsluttet, boolean medBekreftet, boolean medOverstyrt) {
        LocalDate terminDato = LocalDate.now().minusDays(70);

        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel()
            .medSøknadDato(terminDato.minusDays(20));
        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medNavnPå("Lege Legesen")
                .medTermindato(terminDato)
                .medUtstedtDato(terminDato.minusDays(40)))
            .medAntallBarn(1);

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

        Whitebox.setInternalState(behandling, "status", BehandlingStatus.AVSLUTTET);

        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);

        BehandlingVedtak vedtak = BehandlingVedtak.builder()
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medVedtakstidspunkt(terminDato.minusWeeks(fødselUkerFørTermin).atStartOfDay())
            .medBehandlingsresultat(behandling.getBehandlingsresultat())
            .medAnsvarligSaksbehandler("Severin Saksbehandler")
            .build();

        repositoryProvider.getBehandlingVedtakRepository().lagre(vedtak, lås);

        repoRule.getRepository().flushAndClear();

        return repoRule.getEntityManager().find(Behandling.class, behandling.getId());
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

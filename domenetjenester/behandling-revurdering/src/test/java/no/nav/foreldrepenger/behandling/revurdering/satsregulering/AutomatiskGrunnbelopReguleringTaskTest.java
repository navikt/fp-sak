package no.nav.foreldrepenger.behandling.revurdering.satsregulering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.revurdering.flytkontroll.BehandlingFlytkontroll;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskRepositoryImpl;
import no.nav.vedtak.felles.testutilities.Whitebox;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class AutomatiskGrunnbelopReguleringTaskTest {

    private final BehandlingFlytkontroll flytkontroll = mock(BehandlingFlytkontroll.class);
    private final BehandlendeEnhetTjeneste enhetsTjeneste = mock(BehandlendeEnhetTjeneste.class);

    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;
    private ProsessTaskRepository prosessTaskRepository;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
        prosessTaskRepository = new ProsessTaskRepositoryImpl(entityManager, null, null);
    }

    @Test
    public void skal_opprette_revurderingsbehandling_med_årsak_når_avsluttet_behandling() {
        Behandling behandling = opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET);
        when(enhetsTjeneste.finnBehandlendeEnhetFor(any())).thenReturn(new OrganisasjonsEnhet("1234", "Test"));

        ProsessTaskData prosessTaskData = new ProsessTaskData(AutomatiskGrunnbelopReguleringTask.TASKTYPE);
        prosessTaskData.setFagsak(behandling.getFagsakId(), behandling.getAktørId().getId());
        prosessTaskData.setSekvens("1");

        var task = createTask();
        task.doTask(prosessTaskData);

        assertRevurdering(behandling, BehandlingÅrsakType.RE_SATS_REGULERING);
    }

    private void assertRevurdering(Behandling behandling, BehandlingÅrsakType behandlingÅrsakType) {
        Optional<Behandling> revurdering = behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(
            behandling.getFagsakId(), BehandlingType.REVURDERING);
        assertThat(revurdering).as("Ingen revurdering").isPresent();
        List<BehandlingÅrsak> behandlingÅrsaker = revurdering.get().getBehandlingÅrsaker();
        assertThat(behandlingÅrsaker).isNotEmpty();
        List<BehandlingÅrsakType> årsaker = behandlingÅrsaker.stream()
            .map(bå -> bå.getBehandlingÅrsakType())
            .collect(Collectors.toList());
        assertThat(årsaker).contains(behandlingÅrsakType);
    }

    private void assertIngenRevurdering(Fagsak fagsak) {
        Optional<Behandling> revurdering = behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(
            fagsak.getId(), BehandlingType.REVURDERING);
        assertThat(revurdering).as("Har revurdering: " + fagsak.getId()).isNotPresent();
    }

    private AutomatiskGrunnbelopReguleringTask createTask() {
        return new AutomatiskGrunnbelopReguleringTask(repositoryProvider,
            prosessTaskRepository, enhetsTjeneste, flytkontroll);

    }

    @Test
    public void skal_ikke_opprette_revurdering_dersom_åpen_behandling_på_fagsak() {
        Behandling behandling = opprettRevurderingsKandidat(BehandlingStatus.UTREDES);
        when(enhetsTjeneste.finnBehandlendeEnhetFor(any())).thenReturn(new OrganisasjonsEnhet("1234", "Test"));

        ProsessTaskData prosessTaskData = new ProsessTaskData(AutomatiskGrunnbelopReguleringTask.TASKTYPE);
        prosessTaskData.setFagsak(behandling.getFagsakId(), behandling.getAktørId().getId());
        prosessTaskData.setSekvens("1");

        var task = createTask();
        task.doTask(prosessTaskData);

        assertIngenRevurdering(behandling.getFagsak());
    }

    @Test
    public void skal_køe_revurdering_dersom_åpen_berørt_på_fagsak() {
        Behandling behandling = opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET);
        when(flytkontroll.nyRevurderingSkalVente(any())).thenReturn(true);
        when(enhetsTjeneste.finnBehandlendeEnhetFor(any())).thenReturn(new OrganisasjonsEnhet("1234", "Test"));

        ProsessTaskData prosessTaskData = new ProsessTaskData(AutomatiskGrunnbelopReguleringTask.TASKTYPE);
        prosessTaskData.setFagsak(behandling.getFagsakId(), behandling.getAktørId().getId());
        prosessTaskData.setSekvens("1");

        var task = createTask();
        task.doTask(prosessTaskData);

        Optional<Behandling> regulering = behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(
            behandling.getFagsakId(), BehandlingType.REVURDERING);
        assertThat(regulering.filter(b -> b.harBehandlingÅrsak(BehandlingÅrsakType.RE_SATS_REGULERING))).isPresent();
        verify(flytkontroll).settNyRevurderingPåVent(regulering.get());
    }

    private Behandling opprettRevurderingsKandidat(BehandlingStatus status) {
        LocalDate terminDato = LocalDate.now().plusDays(10);

        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medSøknadDato(terminDato.minusDays(20));

        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medNavnPå("Lege Legesen")
                .medTermindato(terminDato)
                .medUtstedtDato(terminDato.minusDays(40)))
            .medAntallBarn(1);

        scenario.medBekreftetHendelse()
            .medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
                .medNavnPå("Lege Legesen")
                .medTermindato(terminDato)
                .medUtstedtDato(terminDato.minusDays(40)))
            .medAntallBarn(1);

        scenario.leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT);
        scenario.medVilkårResultatType(VilkårResultatType.INNVILGET);

        scenario.medBehandlingVedtak()
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medVedtakstidspunkt(terminDato.minusWeeks(2).atStartOfDay())
            .medAnsvarligSaksbehandler("Severin Saksbehandler")
            .build();

        scenario.medBehandlingsresultat(
            Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.INNVILGET));

        Behandling behandling = scenario.lagre(repositoryProvider);

        Whitebox.setInternalState(behandling, "status", status);

        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);

        repositoryProvider.getOpptjeningRepository()
            .lagreOpptjeningsperiode(behandling, LocalDate.now().minusYears(1), LocalDate.now(), false);

        return behandlingRepository.hentBehandling(behandling.getId());
    }

}

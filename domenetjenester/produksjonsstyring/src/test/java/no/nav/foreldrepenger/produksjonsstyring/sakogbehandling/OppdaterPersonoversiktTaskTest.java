package no.nav.foreldrepenger.produksjonsstyring.sakogbehandling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Tema;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ExtendWith(MockitoExtension.class)
class OppdaterPersonoversiktTaskTest {

    private static final String NASJONAL = "4867";

    private OppdaterPersonoversiktTask observer;
    private BehandlingRepositoryProvider repositoryProvider;
    @Mock
    private PersonoversiktHendelseProducer producer;
    @Mock
    private BehandlendeEnhetTjeneste enhetTjeneste;


    @Test
    void skalOppretteOppdaterSakOgBehandlingTaskMedAlleParametereNårBehandlingErOpprettet() {

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medFødselAdopsjonsdato(Collections.singletonList(LocalDate.now().plusDays(1)));
        scenario.medBehandlendeEnhet(NASJONAL);
        var behandling = scenario.lagMocked();
        var fagsak = behandling.getFagsak();
        var task = ProsessTaskData.forProsessTask(OppdaterPersonoversiktTask.class);
        task.setBehandling(fagsak.getSaksnummer().getVerdi(), fagsak.getId(), behandling.getId());
        task.setProperty(OppdaterPersonoversiktTask.PH_REF_KEY, Fagsystem.FPSAK.getOffisiellKode() + "_" + behandling.getId());
        task.setProperty(OppdaterPersonoversiktTask.PH_STATUS_KEY, behandling.getStatus().getKode());
        task.setProperty(OppdaterPersonoversiktTask.PH_TID_KEY, LocalDateTime.now().toString());
        task.setProperty(OppdaterPersonoversiktTask.PH_TYPE_KEY, behandling.getType().getKode());

        repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        observer = new OppdaterPersonoversiktTask(producer,mock(PersoninfoAdapter.class), repositoryProvider, mock(BehandlendeEnhetTjeneste.class));

        var captorKey = ArgumentCaptor.forClass(String.class);
        var captorVal = ArgumentCaptor.forClass(String.class);
        observer.doTask(task);

        verify(producer).sendJsonMedNøkkel(captorKey.capture(), captorVal.capture());
        var value = captorVal.getValue();
        var roundtrip = StandardJsonConfig.fromJson(value, PersonoversiktBehandlingStatusDto.class);
        assertThat(roundtrip.behandlingsID()).isEqualToIgnoringCase(Fagsystem.FPSAK.getOffisiellKode() + "_" + behandling.getId());
        assertThat(roundtrip.behandlingstema().value()).isEqualToIgnoringCase(BehandlingTema.ENGANGSSTØNAD_FØDSEL.getOffisiellKode());
        assertThat(roundtrip.behandlingstype().value()).isEqualToIgnoringCase(BehandlingType.FØRSTEGANGSSØKNAD.getOffisiellKode());
        assertThat(roundtrip.avslutningsstatus()).isNull();
        assertThat(roundtrip.hendelseType()).isEqualTo("behandlingOpprettet");
        assertThat(roundtrip.hendelsesprodusentREF().value()).isEqualTo(Fagsystem.FPSAK.getOffisiellKode());
        assertThat(roundtrip.sakstema().value()).isEqualTo(Tema.FOR.getOffisiellKode());
        assertThat(roundtrip.aktoerREF().get(0).aktoerId()).isEqualTo(behandling.getAktørId().getId());
        assertThat(roundtrip.ansvarligEnhetREF()).isEqualTo(NASJONAL);
    }

    @Test
    void skalOppretteOppdaterSakOgBehandlingTaskMedAlleParametereNårBehandlingErAvsluttet() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medFødselAdopsjonsdato(Collections.singletonList(LocalDate.now().plusDays(1)));
        scenario.medBehandlendeEnhet(NASJONAL);
        var behandling = scenario.lagMocked();
        repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        behandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(behandling, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));
        behandling = repositoryProvider.getBehandlingRepository().hentBehandling(behandling.getId());
        var fagsak =behandling.getFagsak();
        var task = ProsessTaskData.forProsessTask(OppdaterPersonoversiktTask.class);
        task.setBehandling(fagsak.getSaksnummer().getVerdi(), fagsak.getId(), behandling.getId());
        task.setProperty(OppdaterPersonoversiktTask.PH_REF_KEY, Fagsystem.FPSAK.getOffisiellKode() + "_" + behandling.getId());
        task.setProperty(OppdaterPersonoversiktTask.PH_STATUS_KEY, behandling.getStatus().getKode());
        task.setProperty(OppdaterPersonoversiktTask.PH_TID_KEY, LocalDateTime.now().toString());
        task.setProperty(OppdaterPersonoversiktTask.PH_TYPE_KEY, behandling.getType().getKode());


        observer = new OppdaterPersonoversiktTask(producer, mock(PersoninfoAdapter.class), repositoryProvider, mock(BehandlendeEnhetTjeneste.class));

        var captorKey = ArgumentCaptor.forClass(String.class);
        var captorVal = ArgumentCaptor.forClass(String.class);
        observer.doTask(task);

        verify(producer).sendJsonMedNøkkel(captorKey.capture(), captorVal.capture());
        var value = captorVal.getValue();
        var roundtrip = StandardJsonConfig.fromJson(value, PersonoversiktBehandlingStatusDto.class);
        assertThat(roundtrip.behandlingsID()).isEqualToIgnoringCase(Fagsystem.FPSAK.getOffisiellKode() + "_" + behandling.getId());
        assertThat(roundtrip.avslutningsstatus().value()).isEqualTo("ok");
        assertThat(roundtrip.behandlingstema().value()).isEqualToIgnoringCase(BehandlingTema.ENGANGSSTØNAD_FØDSEL.getOffisiellKode());
        assertThat(roundtrip.behandlingstype().value()).isEqualToIgnoringCase(BehandlingType.FØRSTEGANGSSØKNAD.getOffisiellKode());
        assertThat(roundtrip.hendelseType()).isEqualTo("behandlingAvsluttet");
        assertThat(roundtrip.hendelsesprodusentREF().value()).isEqualTo(Fagsystem.FPSAK.getOffisiellKode());
        assertThat(roundtrip.sakstema().value()).isEqualTo(Tema.FOR.getOffisiellKode());
        assertThat(roundtrip.aktoerREF().get(0).aktoerId()).isEqualTo(behandling.getAktørId().getId());
        assertThat(roundtrip.ansvarligEnhetREF()).isEqualTo(NASJONAL);
    }

    @Test
    void skalOppretteOppdaterSakOgBehandlingTaskMedAlleParametereNårTilbakeBehandlingErOpprettet() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medFødselAdopsjonsdato(Collections.singletonList(LocalDate.now().plusDays(1)));
        scenario.medBehandlendeEnhet(NASJONAL);
        var behandling = scenario.lagMocked();
        repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        behandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(behandling, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));
        behandling = repositoryProvider.getBehandlingRepository().hentBehandling(behandling.getId());
        var fagsak =behandling.getFagsak();
        var task = ProsessTaskData.forProsessTask(OppdaterPersonoversiktTask.class);
        var tbkUUID = UUID.randomUUID();
        task.setFagsak(fagsak.getSaksnummer().getVerdi(), fagsak.getId());
        task.setProperty(OppdaterPersonoversiktTask.PH_REF_KEY, Fagsystem.FPSAK.getOffisiellKode() + "_T" + tbkUUID);
        task.setProperty(OppdaterPersonoversiktTask.PH_STATUS_KEY, BehandlingStatus.OPPRETTET.getKode());
        task.setProperty(OppdaterPersonoversiktTask.PH_TID_KEY, LocalDateTime.now().toString());
        task.setProperty(OppdaterPersonoversiktTask.PH_TYPE_KEY, BehandlingType.TILBAKEKREVING_ORDINÆR.getKode());

        when(enhetTjeneste.finnBehandlendeEnhetFra(any(Behandling.class))).thenReturn(new OrganisasjonsEnhet(NASJONAL, "Nasjonal"));

        observer = new OppdaterPersonoversiktTask(producer, mock(PersoninfoAdapter.class), repositoryProvider, enhetTjeneste);

        var captorKey = ArgumentCaptor.forClass(String.class);
        var captorVal = ArgumentCaptor.forClass(String.class);
        observer.doTask(task);

        verify(producer).sendJsonMedNøkkel(captorKey.capture(), captorVal.capture());
        var value = captorVal.getValue();
        var roundtrip = StandardJsonConfig.fromJson(value, PersonoversiktBehandlingStatusDto.class);
        assertThat(roundtrip.behandlingsID()).isEqualToIgnoringCase(Fagsystem.FPSAK.getOffisiellKode() + "_T" + tbkUUID);
        assertThat(roundtrip.behandlingstema().value()).isEqualToIgnoringCase(BehandlingTema.ENGANGSSTØNAD_FØDSEL.getOffisiellKode());
        assertThat(roundtrip.behandlingstype().value()).isEqualToIgnoringCase(BehandlingType.TILBAKEKREVING_ORDINÆR.getOffisiellKode());
        assertThat(roundtrip.hendelseType()).isEqualTo("behandlingOpprettet");
        assertThat(roundtrip.hendelsesprodusentREF().value()).isEqualTo(Fagsystem.FPSAK.getOffisiellKode());
        assertThat(roundtrip.sakstema().value()).isEqualTo(Tema.FOR.getOffisiellKode());
        assertThat(roundtrip.aktoerREF().get(0).aktoerId()).isEqualTo(behandling.getAktørId().getId());
        assertThat(roundtrip.ansvarligEnhetREF()).isEqualTo(NASJONAL);
    }

    @Test
    void skalOppretteOppdaterSakOgBehandlingTaskMedAlleParametereNårTilbakeBehandlingErAvsluttet() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medFødselAdopsjonsdato(Collections.singletonList(LocalDate.now().plusDays(1)));
        scenario.medBehandlendeEnhet(NASJONAL);
        var behandling = scenario.lagMocked();
        repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        behandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(behandling, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));
        behandling = repositoryProvider.getBehandlingRepository().hentBehandling(behandling.getId());
        var fagsak =behandling.getFagsak();
        var task = ProsessTaskData.forProsessTask(OppdaterPersonoversiktTask.class);
        var tbkUUID = UUID.randomUUID();
        task.setFagsak(fagsak.getSaksnummer().getVerdi(), fagsak.getId());
        task.setProperty(OppdaterPersonoversiktTask.PH_REF_KEY, Fagsystem.FPSAK.getOffisiellKode() + "_T" + tbkUUID);
        task.setProperty(OppdaterPersonoversiktTask.PH_STATUS_KEY, BehandlingStatus.AVSLUTTET.getKode());
        task.setProperty(OppdaterPersonoversiktTask.PH_TID_KEY, LocalDateTime.now().toString());
        task.setProperty(OppdaterPersonoversiktTask.PH_TYPE_KEY, BehandlingType.TILBAKEKREVING_REVURDERING.getKode());

        when(enhetTjeneste.finnBehandlendeEnhetFra(any(Behandling.class))).thenReturn(new OrganisasjonsEnhet(NASJONAL, "Nasjonal"));

        observer = new OppdaterPersonoversiktTask(producer, mock(PersoninfoAdapter.class), repositoryProvider, enhetTjeneste);

        var captorKey = ArgumentCaptor.forClass(String.class);
        var captorVal = ArgumentCaptor.forClass(String.class);
        observer.doTask(task);

        verify(producer).sendJsonMedNøkkel(captorKey.capture(), captorVal.capture());
        var value = captorVal.getValue();
        var roundtrip = StandardJsonConfig.fromJson(value, PersonoversiktBehandlingStatusDto.class);
        assertThat(roundtrip.behandlingsID()).isEqualToIgnoringCase(Fagsystem.FPSAK.getOffisiellKode() + "_T" + tbkUUID);
        assertThat(roundtrip.avslutningsstatus().value()).isEqualTo("ok");
        assertThat(roundtrip.behandlingstema().value()).isEqualToIgnoringCase(BehandlingTema.ENGANGSSTØNAD_FØDSEL.getOffisiellKode());
        assertThat(roundtrip.behandlingstype().value()).isEqualToIgnoringCase(BehandlingType.TILBAKEKREVING_REVURDERING.getOffisiellKode());
        assertThat(roundtrip.hendelseType()).isEqualTo("behandlingAvsluttet");
        assertThat(roundtrip.hendelsesprodusentREF().value()).isEqualTo(Fagsystem.FPSAK.getOffisiellKode());
        assertThat(roundtrip.sakstema().value()).isEqualTo(Tema.FOR.getOffisiellKode());
        assertThat(roundtrip.aktoerREF().get(0).aktoerId()).isEqualTo(behandling.getAktørId().getId());
        assertThat(roundtrip.ansvarligEnhetREF()).isEqualTo(NASJONAL);
    }


}

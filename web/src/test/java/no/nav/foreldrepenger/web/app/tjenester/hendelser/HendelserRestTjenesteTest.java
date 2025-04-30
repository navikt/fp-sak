package no.nav.foreldrepenger.web.app.tjenester.hendelser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.hendelser.HendelseSorteringRepository;
import no.nav.foreldrepenger.behandlingslager.hendelser.HendelsemottakRepository;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.AktørIdDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.HendelseWrapperDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.pdl.DødfødselHendelseDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.pdl.FødselHendelseDto;
import no.nav.foreldrepenger.mottak.hendelser.KlargjørHendelseTask;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

@ExtendWith(MockitoExtension.class)
@ExtendWith(JpaExtension.class)
class HendelserRestTjenesteTest {

    private static final String HENDELSE_ID = "1337";
    private static final TaskType HENDELSE_TASK = TaskType.forProsessTask(KlargjørHendelseTask.class);

    @Mock
    private HendelseSorteringRepository sorteringRepository;
    private HendelserRestTjeneste hendelserRestTjeneste;
    private HendelsemottakRepository hendelsemottakRepository;
    @Mock
    private ProsessTaskTjeneste taskTjeneste;

    @BeforeEach
    public void before(EntityManager entityManager) {
        hendelsemottakRepository = new HendelsemottakRepository(entityManager);
        hendelserRestTjeneste = new HendelserRestTjeneste(sorteringRepository, hendelsemottakRepository,
            taskTjeneste);
    }

    @Test
    void skal_ta_imot_fødselshendelse_og_opprette_prosesstask() {
        var forelder1 = AktørId.dummy();
        var forelder2 = AktørId.dummy();
        var aktørIdForeldre = List.of(forelder1, forelder2);
        var fødselsdato = LocalDate.now();
        var hendelse = lagFødselHendelse(aktørIdForeldre, fødselsdato);

        hendelserRestTjeneste.mottaHendelse(new HendelseWrapperDto(hendelse));

        assertThat(hendelsemottakRepository.hendelseErNy(HENDELSE_ID)).isFalse();
        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste).lagre(captor.capture());
        var tasks = captor.getAllValues();
        var task = tasks.stream().filter(d -> Objects.equals(HENDELSE_TASK, d.taskType())).findFirst().orElseThrow();
        assertThat(task.taskType()).isEqualTo(HENDELSE_TASK);
        assertThat(task.getPayloadAsString()).isEqualTo(StandardJsonConfig.toJson(hendelse));
        assertThat(task.getPropertyValue(KlargjørHendelseTask.PROPERTY_UID)).isEqualTo(HENDELSE_ID);
        assertThat(task.getPropertyValue(KlargjørHendelseTask.PROPERTY_HENDELSE_TYPE)).isEqualTo("FØDSEL");

        Set<String> abacAttributterAktør = (new HendelserRestTjeneste.HendelseWrapperDtoAbacDataSupplier())
            .apply(new HendelseWrapperDto(hendelse))
            .getVerdier(AppAbacAttributtType.AKTØR_ID);
        assertThat(abacAttributterAktør).hasSize(2).containsExactlyInAnyOrder(forelder1.getId(), forelder2.getId());
    }

    @Test
    void skal_ta_imot_dødfødselhendelse_og_opprette_prosesstask() {
        var forelder1 = AktørId.dummy();
        var forelder2 = AktørId.dummy();
        var aktørIdForeldre = List.of(forelder1, forelder2);
        var dødfødseldato = LocalDate.now();
        var hendelse = lagDødfødselHendelse(aktørIdForeldre, dødfødseldato);

        hendelserRestTjeneste.mottaHendelse(new HendelseWrapperDto(hendelse));

        assertThat(hendelsemottakRepository.hendelseErNy(HENDELSE_ID)).isFalse();
        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste).lagre(captor.capture());
        var tasks = captor.getAllValues();
        var task = tasks.stream().filter(d -> Objects.equals(HENDELSE_TASK, d.taskType())).findFirst().orElseThrow();
        assertThat(task.taskType()).isEqualTo(HENDELSE_TASK);
        assertThat(task.getPayloadAsString()).isEqualTo(StandardJsonConfig.toJson(hendelse));
        assertThat(task.getPropertyValue(KlargjørHendelseTask.PROPERTY_UID)).isEqualTo(HENDELSE_ID);
        assertThat(task.getPropertyValue(KlargjørHendelseTask.PROPERTY_HENDELSE_TYPE)).isEqualTo("DØDFØDSEL");

        Set<String> abacAttributterAktør = (new HendelserRestTjeneste.HendelseWrapperDtoAbacDataSupplier())
            .apply(new HendelseWrapperDto(hendelse))
            .getVerdier(AppAbacAttributtType.AKTØR_ID);
        assertThat(abacAttributterAktør).hasSize(2).containsExactlyInAnyOrder(forelder1.getId(), forelder2.getId());
    }

    @Test
    void skal_ikke_opprette_prosess_task_når_hendelse_med_samme_uid_tidligere_er_mottatt() {
        hendelsemottakRepository.registrerMottattHendelse(HENDELSE_ID);
        var aktørIdForeldre = List.of(AktørId.dummy(), AktørId.dummy());
        var fødselsdato = LocalDate.now();

        hendelserRestTjeneste.mottaHendelse(new HendelseWrapperDto(lagFødselHendelse(aktørIdForeldre, fødselsdato)));

        verifyNoInteractions(taskTjeneste);
    }

    @Test
    void skal_returnere_tom_liste_når_aktørId_ikke_er_registrert_eller_mangler_sak() {
        when(sorteringRepository.hentEksisterendeAktørIderMedSak(anyList())).thenReturn(Collections.emptyList());

        var resultat = hendelserRestTjeneste.grovSorter(List.of(new AktørIdDto("0000000000000")));

        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_returnere_liste_med_4_aktørIder_som_har_sak() {
        List<AktørId> harSak = new ArrayList<>(List.of(
                AktørId.dummy(),
                AktørId.dummy(),
                AktørId.dummy(),
                AktørId.dummy()));

        when(sorteringRepository.hentEksisterendeAktørIderMedSak(anyList())).thenReturn(harSak);

        List<AktørIdDto> sorter = new ArrayList<>();
        sorter.add(new AktørIdDto("0000000000000"));
        sorter.add(new AktørIdDto("0000000000001"));
        sorter.add(new AktørIdDto("0000000000002"));

        var resultat = hendelserRestTjeneste.grovSorter(sorter);

        assertThat(resultat)
            .hasSameSizeAs(harSak)
            .isEqualTo(harSak.stream().map(AktørId::getId).toList());

        Set<String> abacAttributterAktør = (new HendelserRestTjeneste.AktørIdDtoAbacDataSupplier())
            .apply(sorter)
            .getVerdier(AppAbacAttributtType.AKTØR_ID);
        assertThat(abacAttributterAktør).hasSize(3)
            .containsExactlyInAnyOrderElementsOf(sorter.stream().map(AktørIdDto::getAktørId).toList());
    }

    private FødselHendelseDto lagFødselHendelse(List<AktørId> aktørIdForeldre, LocalDate fødselsdato) {
        var hendelse = new FødselHendelseDto();
        hendelse.setId(HENDELSE_ID);
        hendelse.setAktørIdForeldre(aktørIdForeldre.stream().map(AktørId::getId).map(AktørIdDto::new).toList());
        hendelse.setFødselsdato(fødselsdato);
        return hendelse;
    }

    private DødfødselHendelseDto lagDødfødselHendelse(List<AktørId> aktørIdForeldre, LocalDate dødfødseldato) {
        var hendelse = new DødfødselHendelseDto();
        hendelse.setId(HENDELSE_ID);
        hendelse.setAktørId(aktørIdForeldre.stream().map(AktørId::getId).map(AktørIdDto::new).toList());
        hendelse.setDødfødselsdato(dødfødseldato);
        return hendelse;
    }

}

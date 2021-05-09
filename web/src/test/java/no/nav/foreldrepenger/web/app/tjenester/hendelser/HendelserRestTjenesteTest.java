package no.nav.foreldrepenger.web.app.tjenester.hendelser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.hendelser.HendelseSorteringRepository;
import no.nav.foreldrepenger.behandlingslager.hendelser.HendelsemottakRepository;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.AktørIdDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.pdl.DødfødselHendelseDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.pdl.FødselHendelseDto;
import no.nav.foreldrepenger.mottak.hendelser.KlargjørHendelseTask;
import no.nav.foreldrepenger.web.app.tjenester.hendelser.HendelserRestTjeneste.AbacAktørIdDto;
import no.nav.foreldrepenger.web.app.tjenester.hendelser.HendelserRestTjeneste.AbacHendelseWrapperDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskRepositoryImpl;

@ExtendWith(MockitoExtension.class)
@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class HendelserRestTjenesteTest {

    private static final String HENDELSE_ID = "1337";

    @Mock
    private HendelseSorteringRepository sorteringRepository;
    private HendelserRestTjeneste hendelserRestTjeneste;
    private HendelsemottakRepository hendelsemottakRepository;
    private ProsessTaskRepositoryImpl prosessTaskRepository;

    @BeforeEach
    public void before(EntityManager entityManager) {
        hendelsemottakRepository = new HendelsemottakRepository(entityManager);
        prosessTaskRepository = new ProsessTaskRepositoryImpl(entityManager, null, null);
        hendelserRestTjeneste = new HendelserRestTjeneste(sorteringRepository, hendelsemottakRepository,
            prosessTaskRepository);
    }

    @Test
    public void skal_ta_imot_fødselshendelse_og_opprette_prosesstask() {
        var aktørIdForeldre = List.of(AktørId.dummy(), AktørId.dummy());
        var fødselsdato = LocalDate.now();
        var hendelse = lagFødselHendelse(aktørIdForeldre, fødselsdato);

        hendelserRestTjeneste.mottaHendelse(new AbacHendelseWrapperDto(hendelse));

        assertThat(hendelsemottakRepository.hendelseErNy(HENDELSE_ID)).isFalse();
        var tasks = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        var task = tasks.stream().filter(d -> Objects.equals(KlargjørHendelseTask.TASKTYPE, d.getTaskType())).findFirst().orElseThrow();
        assertThat(task.getTaskType()).isEqualTo(KlargjørHendelseTask.TASKTYPE);
        assertThat(task.getPayloadAsString()).isEqualTo(StandardJsonConfig.toJson(hendelse));
        assertThat(task.getPropertyValue(KlargjørHendelseTask.PROPERTY_UID)).isEqualTo(HENDELSE_ID);
        assertThat(task.getPropertyValue(KlargjørHendelseTask.PROPERTY_HENDELSE_TYPE)).isEqualTo("FØDSEL");
    }

    @Test
    public void skal_ta_imot_dødfødselhendelse_og_opprette_prosesstask() {
        var aktørIdForeldre = List.of(AktørId.dummy(), AktørId.dummy());
        var dødfødseldato = LocalDate.now();
        var hendelse = lagDødfødselHendelse(aktørIdForeldre, dødfødseldato);

        hendelserRestTjeneste.mottaHendelse(new AbacHendelseWrapperDto(hendelse));

        assertThat(hendelsemottakRepository.hendelseErNy(HENDELSE_ID)).isFalse();
        var tasks = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        var task = tasks.stream().filter(d -> Objects.equals(KlargjørHendelseTask.TASKTYPE, d.getTaskType())).findFirst().orElseThrow();
        assertThat(task.getTaskType()).isEqualTo(KlargjørHendelseTask.TASKTYPE);
        assertThat(task.getPayloadAsString()).isEqualTo(StandardJsonConfig.toJson(hendelse));
        assertThat(task.getPropertyValue(KlargjørHendelseTask.PROPERTY_UID)).isEqualTo(HENDELSE_ID);
        assertThat(task.getPropertyValue(KlargjørHendelseTask.PROPERTY_HENDELSE_TYPE)).isEqualTo("DØDFØDSEL");
    }

    @Test
    public void skal_ikke_opprette_prosess_task_når_hendelse_med_samme_uid_tidligere_er_mottatt() {
        hendelsemottakRepository.registrerMottattHendelse(HENDELSE_ID);
        var aktørIdForeldre = List.of(AktørId.dummy(), AktørId.dummy());
        var fødselsdato = LocalDate.now();

        hendelserRestTjeneste.mottaHendelse(new AbacHendelseWrapperDto(lagFødselHendelse(aktørIdForeldre, fødselsdato)));

        var tasks = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        assertThat(tasks).allSatisfy(d -> assertThat(d.getTaskType()).isNotEqualTo(KlargjørHendelseTask.TASKTYPE));
    }

    @Test
    public void skal_returnere_tom_liste_når_aktørId_ikke_er_registrert_eller_mangler_sak() {
        when(sorteringRepository.hentEksisterendeAktørIderMedSak(anyList())).thenReturn(Collections.emptyList());

        var resultat = hendelserRestTjeneste.grovSorter(List.of(new AbacAktørIdDto("0000000000000")));

        assertThat(resultat).isEmpty();
    }

    @Test
    public void skal_returnere_liste_med_4_aktørIder_som_har_sak() {
        List<AktørId> harSak = new ArrayList<>(List.of(
                AktørId.dummy(),
                AktørId.dummy(),
                AktørId.dummy(),
                AktørId.dummy()));

        when(sorteringRepository.hentEksisterendeAktørIderMedSak(anyList())).thenReturn(harSak);

        List<AbacAktørIdDto> sorter = new ArrayList<>();
        sorter.add(new AbacAktørIdDto("0000000000000"));
        sorter.add(new AbacAktørIdDto("0000000000001"));
        sorter.add(new AbacAktørIdDto("0000000000002"));

        var resultat = hendelserRestTjeneste.grovSorter(sorter);

        assertThat(resultat).hasSameSizeAs(harSak);
        assertThat(resultat).isEqualTo(harSak.stream().map(AktørId::getId).collect(Collectors.toList()));
    }

    private FødselHendelseDto lagFødselHendelse(List<AktørId> aktørIdForeldre, LocalDate fødselsdato) {
        var hendelse = new FødselHendelseDto();
        hendelse.setId(HENDELSE_ID);
        hendelse.setAktørIdForeldre(aktørIdForeldre.stream().map(AktørId::getId).map(AktørIdDto::new).collect(Collectors.toList()));
        hendelse.setFødselsdato(fødselsdato);
        return hendelse;
    }

    private DødfødselHendelseDto lagDødfødselHendelse(List<AktørId> aktørIdForeldre, LocalDate dødfødseldato) {
        var hendelse = new DødfødselHendelseDto();
        hendelse.setId(HENDELSE_ID);
        hendelse.setAktørId(aktørIdForeldre.stream().map(AktørId::getId).map(AktørIdDto::new).collect(Collectors.toList()));
        hendelse.setDødfødselsdato(dødfødseldato);
        return hendelse;
    }

}

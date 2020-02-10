package no.nav.foreldrepenger.domene.risikoklassifisering.impl;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.MDC;
import org.threeten.extra.Interval;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.HendelseVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.risikoklassifisering.Risikoklassifisering;
import no.nav.foreldrepenger.domene.risikoklassifisering.task.RisikoklassifiseringUtførTask;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.RisikovurderingTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.skjæringstidspunkt.OpplysningsPeriodeTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskRepositoryImpl;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

@RunWith(CdiRunner.class)
public class RisikoklassifiseringTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    @Rule
    public RepositoryRule repoRule = new UnittestRepositoryRule();

    private ProsessTaskRepository prosessTaskRepository = spy(new ProsessTaskRepositoryImpl(repoRule.getEntityManager(), null, null));

    @Mock
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    @Mock
    private RisikovurderingTjeneste risikovurderingTjeneste;

    @Mock
    private OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste;

    @Mock
    private FamilieHendelseRepository familieHendelseRepository;

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    private static final AktørId ANNEN_PART_AKTØR_ID = AktørId.dummy();

    private static final LocalDate BARN_TERMINDATO = LocalDate.now();

    private static final LocalDate BARN_FØDSELSDATO = LocalDate.now();

    private Risikoklassifisering risikoklassifisering;

    private static final String TASKTYPE = "risiko.klassifisering";

    private static final String KONSUMENT_ID = "konsumentId";

    private static final String RISIKOKLASSIFISERING_JSON = "risikoklassifisering.request.json";

    private static final ObjectMapper OM;
    static {
        OM = new ObjectMapper();
        OM.registerModule(new JavaTimeModule());
        OM.registerModule(new Jdk8Module());
        OM.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        OM.setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE);
        OM.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        OM.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OM.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        OM.setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY);
    }

    @Before
    public void init(){
        MockitoAnnotations.initMocks(this);
        risikoklassifisering = new Risikoklassifisering( prosessTaskRepository,  skjæringstidspunktTjeneste,
             risikovurderingTjeneste,  opplysningsPeriodeTjeneste,
            repositoryProvider.getPersonopplysningRepository(),  familieHendelseRepository);
    }

    @Test
    public void skal_opprette_prosess_task_og_request_json_uten_annenpart() throws IOException {
        Behandling behandling = getBehandling(false);
        forberedelse(behandling);
        risikoklassifisering.opprettProsesstaskForRisikovurdering(behandling);
        ProsessTaskData prosessTaskData = verifyProcessTask();
        verifyRequestData(prosessTaskData, false);
    }

    @Test
    public void skal_opprette_prosess_task_og_request_json_med_annenpart() throws IOException {
        Behandling behandling = getBehandling(true);
        forberedelse(behandling);
        risikoklassifisering.opprettProsesstaskForRisikovurdering(behandling);
        ProsessTaskData prosessTaskData = verifyProcessTask();
        verifyRequestData(prosessTaskData, true);
    }

    @Test
    public void skal_ikke_opprette_prosess_task_hvis_behandling_allrede_har_klassifisert(){
        Behandling behandling = getBehandling(true);
        forberedelse(behandling);
        when(risikovurderingTjeneste.behandlingHarBlittRisikoklassifisert(behandling.getId())).thenReturn(true);
        risikoklassifisering.opprettProsesstaskForRisikovurdering(behandling);
        List<ProsessTaskData> prosessTaskDataList = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        assertThat(prosessTaskDataList).allSatisfy(d -> assertThat(d.getTaskType()).isNotEqualTo(RisikoklassifiseringUtførTask.TASKTYPE));
    }

    private void verifyRequestData(ProsessTaskData prosessTaskData, boolean annenPart) throws IOException {
        ObjectNode objectNode = OM.readValue(prosessTaskData.getProperties().getProperty(RISIKOKLASSIFISERING_JSON),ObjectNode.class);
        assertThat(objectNode.get("callId").asText()).isEqualTo("callId");
        JsonNode request = objectNode.get("request");

        assertThat(request.get("konsumentId").asText()).isEqualTo(prosessTaskData.getProperties().getProperty(KONSUMENT_ID));
        assertThat(request.get("behandlingstema").asText()).isEqualTo("ab0050");
        assertThat(request.get("opplysningsperiode")).isNotNull();
        assertThat(request.get("skjæringstidspunkt").asText()).isEqualTo(LocalDate.now().toString());
        if(annenPart){
            assertThat(request.get("annenPart")).isNotNull();
        }else{
            assertThat(request.get("annenPart")).isNull();
        }
    }

    private ProsessTaskData verifyProcessTask() {
        List<ProsessTaskData> prosessTaskDataList = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        assertThat(prosessTaskDataList).anySatisfy(d -> assertThat(d.getTaskType()).isEqualTo(RisikoklassifiseringUtførTask.TASKTYPE));
        ProsessTaskData prosessTaskData = prosessTaskDataList.stream().filter(d -> Objects.equals(d.getTaskType(), RisikoklassifiseringUtførTask.TASKTYPE)).findFirst().orElseThrow();

        verify(prosessTaskRepository).lagre(prosessTaskData);
        assertThat(prosessTaskData.getTaskType()).isEqualTo(TASKTYPE);
        assertThat(prosessTaskData.getProperties().getProperty(KONSUMENT_ID)).isNotBlank();
        assertThat(prosessTaskData.getProperties().getProperty(RISIKOKLASSIFISERING_JSON)).isNotEmpty();
        return prosessTaskData;
    }

    private void forberedelse(Behandling behandling) {
        Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId())).thenReturn(skjæringstidspunkt);
        when(opplysningsPeriodeTjeneste.beregn(behandling.getId(),behandling.getFagsakYtelseType())).thenReturn(Interval.of(Instant.now(), Instant.now()));
        FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag = byggFødselGrunnlag(BARN_TERMINDATO.minusDays(15), BARN_FØDSELSDATO.minusDays(10));
        when(familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())).thenReturn(Optional.of(familieHendelseGrunnlag));
        MDC.put("callId", "callId");
    }

    public static final FamilieHendelseGrunnlagEntitet byggFødselGrunnlag(LocalDate termindato, LocalDate fødselsdato) {
        final FamilieHendelseBuilder hendelseBuilder = FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.SØKNAD);
        if (termindato != null) {
            hendelseBuilder.medTerminbekreftelse(hendelseBuilder.getTerminbekreftelseBuilder()
                .medUtstedtDato(termindato.minusDays(40))
                .medTermindato(termindato)
                .medNavnPå("NAVN"));
        }
        if (fødselsdato != null) {
            hendelseBuilder.medFødselsDato(fødselsdato);
        }
        return FamilieHendelseGrunnlagBuilder.oppdatere(Optional.empty())
            .medSøknadVersjon(hendelseBuilder)
            .build();
    }
    private Behandling getBehandling(boolean annenPart) {
        LocalDate terminDato = LocalDate.now().minusDays(70);

        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel()
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
        if(annenPart)
            scenario.medSøknadAnnenPart().medAktørId(ANNEN_PART_AKTØR_ID);

        return scenario.lagre(repositoryProvider);
    }
}

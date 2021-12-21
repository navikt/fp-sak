package no.nav.foreldrepenger.domene.risikoklassifisering;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.HendelseVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.risikoklassifisering.task.RisikoklassifiseringUtførTask;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.RisikovurderingTjeneste;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.skjæringstidspunkt.OpplysningsPeriodeTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

@ExtendWith(MockitoExtension.class)
public class RisikoklassifiseringTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();

    @Mock
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    @Mock
    private RisikovurderingTjeneste risikovurderingTjeneste;

    @Mock
    private OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste;

    @Mock
    private FamilieHendelseRepository familieHendelseRepository;

    @Mock
    private PersonopplysningRepository personopplysningRepository;

    @Mock
    private ProsessTaskTjeneste taskTjeneste;

    private static final AktørId ANNEN_PART_AKTØR_ID = AktørId.dummy();

    private static final LocalDate BARN_TERMINDATO = LocalDate.now();

    private static final LocalDate BARN_FØDSELSDATO = LocalDate.now();

    private Risikoklassifisering risikoklassifisering;

    private static final String KONSUMENT_ID = "konsumentId";

    private static final String RISIKOKLASSIFISERING_JSON = "risikoklassifisering.request.json";

    @BeforeEach
    void setUp() {
        risikoklassifisering = new Risikoklassifisering(taskTjeneste, skjæringstidspunktTjeneste,
            risikovurderingTjeneste, opplysningsPeriodeTjeneste, personopplysningRepository,
            familieHendelseRepository);
    }

    @Test
    public void skal_opprette_prosess_task_og_request_json_uten_annenpart() throws IOException {
        var behandling = getBehandling();
        var annenPart = false;
        forberedelse(behandling, annenPart);
        var task = risikoklassifisering.opprettPotensiellTaskProsesstask(behandling).orElseThrow();
        verifyProcessTask(task);
        verifyRequestData(task, annenPart);
    }

    @Test
    public void skal_opprette_prosess_task_og_request_json_med_annenpart() throws IOException {
        var behandling = getBehandling();
        var annenPart = true;
        forberedelse(behandling, annenPart);
        var task = risikoklassifisering.opprettPotensiellTaskProsesstask(behandling).orElseThrow();
        verifyProcessTask(task);
        verifyRequestData(task, annenPart);
    }

    @Test
    public void skal_ikke_opprette_prosess_task_hvis_behandling_allrede_har_klassifisert() throws IOException {
        var behandling = getBehandling();
        when(risikovurderingTjeneste.behandlingHarBlittRisikoklassifisert(behandling)).thenReturn(true);
        var task = risikoklassifisering.opprettPotensiellTaskProsesstask(behandling);
        assertThat(task).isEmpty();
    }

    private void verifyRequestData(ProsessTaskData prosessTaskData, boolean annenPart) throws IOException {
        var objectNode = StandardJsonConfig.fromJsonAsTree(prosessTaskData.getPayloadAsString());
        assertThat(objectNode.get("callId").asText()).isEqualTo("callId");
        var request = objectNode.get("request");

        assertThat(request.get("konsumentId").asText()).isEqualTo(
            prosessTaskData.getProperties().getProperty(KONSUMENT_ID));
        assertThat(request.get("behandlingstema").asText()).isEqualTo("ab0050");
        assertThat(request.get("opplysningsperiode")).isNotNull();
        assertThat(request.get("skjæringstidspunkt").asText()).isEqualTo(LocalDate.now().toString());
        if (annenPart) {
            assertThat(request.get("annenPart")).isNotNull();
        } else {
            assertThat(request.get("annenPart")).isNull();
        }
    }

    private void verifyProcessTask(ProsessTaskData prosessTaskData) {
        assertThat(prosessTaskData.taskType()).isEqualTo(TaskType.forProsessTask(RisikoklassifiseringUtførTask.class));
        assertThat(prosessTaskData.getProperties().getProperty(KONSUMENT_ID)).isNotBlank();
        assertThat(prosessTaskData.getPayloadAsString()).isNotBlank();
    }

    private void forberedelse(BehandlingReferanse behandling, boolean annenPart) {
        var skjæringstidspunkt = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId())).thenReturn(skjæringstidspunkt);
        when(opplysningsPeriodeTjeneste.beregn(behandling.getId(), behandling.getFagsakYtelseType())).thenReturn(
            SimpleLocalDateInterval.fraOgMedTomNotNull(LocalDate.now(), LocalDate.now()));
        var familieHendelseGrunnlag = byggFødselGrunnlag(BARN_TERMINDATO.minusDays(15),
            BARN_FØDSELSDATO.minusDays(10));
        when(familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())).thenReturn(
            Optional.of(familieHendelseGrunnlag));

        if (annenPart) {
            when(personopplysningRepository.hentOppgittAnnenPartHvisEksisterer(behandling.getBehandlingId()))
                .thenReturn(Optional.of(new OppgittAnnenPartBuilder().medAktørId(ANNEN_PART_AKTØR_ID).build()));
        }
        MDC.put("callId", "callId");
    }

    public static FamilieHendelseGrunnlagEntitet byggFødselGrunnlag(LocalDate termindato, LocalDate fødselsdato) {
        final var hendelseBuilder = FamilieHendelseBuilder.oppdatere(Optional.empty(),
            HendelseVersjonType.SØKNAD);
        if (termindato != null) {
            hendelseBuilder.medTerminbekreftelse(hendelseBuilder.getTerminbekreftelseBuilder()
                .medUtstedtDato(termindato.minusDays(40))
                .medTermindato(termindato)
                .medNavnPå("NAVN"));
        }
        if (fødselsdato != null) {
            hendelseBuilder.medFødselsDato(fødselsdato);
        }
        return FamilieHendelseGrunnlagBuilder.oppdatere(Optional.empty()).medSøknadVersjon(hendelseBuilder).build();
    }

    private BehandlingReferanse getBehandling() {
        var terminDato = LocalDate.now().minusDays(70);

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel()
            .medSøknadDato(terminDato.minusDays(20));
        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse()
                .getTerminbekreftelseBuilder()
                .medNavnPå("Lege Legesen")
                .medTermindato(terminDato)
                .medUtstedtDato(terminDato.minusDays(40)))
            .medAntallBarn(1);

        scenario.medBekreftetHendelse()
            .medTerminbekreftelse(scenario.medBekreftetHendelse()
                .getTerminbekreftelseBuilder()
                .medNavnPå("Lege Legesen")
                .medTermindato(terminDato)
                .medUtstedtDato(terminDato.minusDays(40)))
            .medAntallBarn(1);

        var behandling = scenario.lagMocked();
        return BehandlingReferanse.fra(behandling);
    }
}

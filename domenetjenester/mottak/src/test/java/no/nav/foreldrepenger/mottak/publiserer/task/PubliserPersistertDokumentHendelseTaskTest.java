package no.nav.foreldrepenger.mottak.publiserer.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsmeldingInnsendingsårsak;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.publiserer.producer.DialogHendelseProducer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

class PubliserPersistertDokumentHendelseTaskTest extends EntityManagerAwareTest {

    private static final JournalpostId JOURNALPOST_ID = new JournalpostId("2");
    private static final AktørId AKTØR_ID = new AktørId("0000000000000");
    private static final Long MOTTATT_DOKUMENT_ID = 101L;
    private static final Arbeidsgiver ARBEIDSGIVER = Arbeidsgiver.virksomhet("999999999");
    private static final DokumentTypeId DOKUMENTTYPE = DokumentTypeId.INNTEKTSMELDING;
    private static final Saksnummer SAKSNUMMER = new Saksnummer("14140798");
    private static final String REFERANSE = "AR34567890";
    private static final LocalDateTime INNSENDING = LocalDateTime.now().minusHours(1);
    private static final LocalDate STARTDATO = LocalDate.now().minusWeeks(2);

    private long FAGSAK_ID = 1L;
    private long BEHANDLING_ID = 100L;
    private PubliserPersistertDokumentHendelseTask publiserPersistertDokumentHendelseTask;
    private DialogHendelseProducer dialogHendelseProducer;

    @BeforeEach
    public void before() {
        var mottatteDokumentTjeneste = mock(MottatteDokumentTjeneste.class);
        var inntektsmeldingTjeneste = mock(InntektsmeldingTjeneste.class);
        dialogHendelseProducer = mock(DialogHendelseProducer.class);
        var entityManager = getEntityManager();
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        FAGSAK_ID = repositoryProvider.getFagsakRepository()
            .opprettNy(Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()),
                RelasjonsRolleType.MORA, SAKSNUMMER));
        var fagsak = repositoryProvider.getFagsakRepository().finnEksaktFagsak(FAGSAK_ID);
        var behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        BEHANDLING_ID = repositoryProvider.getBehandlingRepository()
            .lagre(behandling, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));
        var mottattDokument = MottattDokument.Builder.ny()
            .medFagsakId(FAGSAK_ID)
            .medDokumentType(DOKUMENTTYPE)
            .medBehandlingId(BEHANDLING_ID)
            .medJournalPostId(JOURNALPOST_ID)
            .medId(MOTTATT_DOKUMENT_ID)
            .build();
        when(mottatteDokumentTjeneste.hentMottattDokument(MOTTATT_DOKUMENT_ID)).thenReturn(
            Optional.of(mottattDokument));
        var inntektsmelding = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(ARBEIDSGIVER)
            .medInnsendingstidspunkt(INNSENDING)
            .medStartDatoPermisjon(STARTDATO)
            .medKanalreferanse(REFERANSE)
            .medInntektsmeldingaarsak(InntektsmeldingInnsendingsårsak.NY)
            .medJournalpostId(JOURNALPOST_ID)
            .medKildesystem("AltInn Portal")
            .build();
        when(inntektsmeldingTjeneste.hentInntektsMeldingFor(BEHANDLING_ID, JOURNALPOST_ID)).thenReturn(
            Optional.of(inntektsmelding));
        var fagsakRepository = new FagsakRepository(entityManager);
        publiserPersistertDokumentHendelseTask = new PubliserPersistertDokumentHendelseTask(fagsakRepository,
            mottatteDokumentTjeneste, inntektsmeldingTjeneste, dialogHendelseProducer);
    }

    @Test
    void skal_kalle_InnhentDokumentTjeneste_med_argumenter_fra_ProsessTask() {
        // Arrange
        var prosessTask = ProsessTaskData.forProsessTask(PubliserPersistertDokumentHendelseTask.class);
        prosessTask.setBehandling(FAGSAK_ID, BEHANDLING_ID, AKTØR_ID.getId());
        prosessTask.setProperty(PubliserPersistertDokumentHendelseTask.MOTTATT_DOKUMENT_ID_KEY,
            MOTTATT_DOKUMENT_ID.toString());
        var captor = ArgumentCaptor.forClass(String.class);

        // Act
        publiserPersistertDokumentHendelseTask.doTask(prosessTask);

        // Assert
        verify(dialogHendelseProducer).sendJsonMedNøkkel(eq(AKTØR_ID.getId()), captor.capture());
        assertThat(captor.getValue()).contains("\"hendelse\" : \"INNTEKTSMELDING_NY\"");
        assertThat(captor.getValue()).contains("\"saksnummer\" : \"" + SAKSNUMMER.getVerdi() + "\"");
        assertThat(captor.getValue()).contains("\"startDato\" : \"" + STARTDATO.toString() + "\"");
        assertThat(captor.getValue()).contains("\"referanseId\" : \"" + REFERANSE + "\"");
        assertThat(captor.getValue()).contains("\"arbeidsgiverId\" : \"" + ARBEIDSGIVER.getIdentifikator() + "\"");
    }
}

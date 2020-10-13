package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.Period;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.DokumentPersistererTjeneste;
import no.nav.foreldrepenger.mottak.publiserer.publish.MottattDokumentPersistertPubliserer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

public class HåndterMottattDokumentTaskTest {

    private static final JournalpostId JOURNALPOST_ID = new JournalpostId("2");
    private static final DokumentTypeId DOKUMENTTYPE = DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL;
    private static final LocalDate FORSENDELSE_MOTTATT = LocalDate.now();
    private static final String PAYLOAD_XML = "inntektsmelding.xml";
    private static final  AktørId AKTØR_ID = new AktørId("0000000000000");
    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private long FAGSAK_ID = 1L;
    private long BEHANDLING_ID = 100L;
    private InnhentDokumentTjeneste innhentDokumentTjeneste;
    private HåndterMottattDokumentTask håndterMottattDokumentTask;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private MottatteDokumentRepository mottatteDokumentRepository = new MottatteDokumentRepository(repoRule.getEntityManager());
    private DokumentPersistererTjeneste dokumentPersistererTjeneste = new DokumentPersistererTjeneste(mock(MottattDokumentPersistertPubliserer.class));

    @Before
    public void before() {
        var fristInnsendingPeriode = Period.ofWeeks(6);
        innhentDokumentTjeneste = mock(InnhentDokumentTjeneste.class);
        mottatteDokumentTjeneste = new MottatteDokumentTjeneste(fristInnsendingPeriode, dokumentPersistererTjeneste, mottatteDokumentRepository, repositoryProvider);
        håndterMottattDokumentTask = new HåndterMottattDokumentTask(innhentDokumentTjeneste, dokumentPersistererTjeneste, mottatteDokumentTjeneste, repositoryProvider);
        FAGSAK_ID = repositoryProvider.getFagsakRepository().opprettNy(Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, NavBruker.opprettNyNB(AktørId.dummy())));
        var fagsak = repositoryProvider.getFagsakRepository().finnEksaktFagsak(FAGSAK_ID);
        var behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        BEHANDLING_ID = repositoryProvider.getBehandlingRepository().lagre(behandling, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));
    }

    @Test
    public void skal_kalle_InnhentDokumentTjeneste_med_argumenter_fra_ProsessTask() throws Exception {
        // Arrange
        final String xml = new FileToStringUtil().readFile(PAYLOAD_XML);
        MottattDokument mottattDokument = new MottattDokument.Builder()
            .medFagsakId(FAGSAK_ID)
            .medJournalPostId(JOURNALPOST_ID)
            .medDokumentType(DOKUMENTTYPE)
            .medMottattDato(FORSENDELSE_MOTTATT)
            .medXmlPayload(xml)
            .medElektroniskRegistrert(true)
            .build();

        Long dokumentId = mottatteDokumentTjeneste.lagreMottattDokumentPåFagsak(mottattDokument);

        ProsessTaskData prosessTask = new ProsessTaskData(HåndterMottattDokumentTask.TASKTYPE);
        prosessTask.setFagsakId(FAGSAK_ID);
        prosessTask.setProperty(HåndterMottattDokumentTask.MOTTATT_DOKUMENT_ID_KEY, dokumentId.toString());
        prosessTask.setProperty(HåndterMottattDokumentTask.BEHANDLING_ÅRSAK_TYPE_KEY, BehandlingÅrsakType.UDEFINERT.getKode());
        ArgumentCaptor<MottattDokument> captor = ArgumentCaptor.forClass(MottattDokument.class);

        // Act
        håndterMottattDokumentTask.doTask(prosessTask);

        // Assert
        verify(innhentDokumentTjeneste).utfør(captor.capture(), any(BehandlingÅrsakType.class));
    }

    @Test
    public void skal_kalle_OpprettFraTidligereBehandling_med_argumenter_fra_ProsessTask() throws Exception {
        // Arrange
        MottattDokument mottattDokument = new MottattDokument.Builder()
            .medFagsakId(FAGSAK_ID)
            .medBehandlingId(BEHANDLING_ID)
            .medJournalPostId(JOURNALPOST_ID)
            .medDokumentType(DOKUMENTTYPE)
            .medMottattDato(FORSENDELSE_MOTTATT)
            .medXmlPayload(PAYLOAD_XML)
            .medElektroniskRegistrert(true)
            .build();

        Long dokumentId = mottatteDokumentTjeneste.lagreMottattDokumentPåFagsak(mottattDokument);

        ProsessTaskData prosessTask = new ProsessTaskData(HåndterMottattDokumentTask.TASKTYPE);
        prosessTask.setBehandling(FAGSAK_ID, BEHANDLING_ID, AKTØR_ID.getId());
        prosessTask.setProperty(HåndterMottattDokumentTask.MOTTATT_DOKUMENT_ID_KEY, dokumentId.toString());
        prosessTask.setProperty(HåndterMottattDokumentTask.BEHANDLING_ÅRSAK_TYPE_KEY, BehandlingÅrsakType.ETTER_KLAGE.getKode());
        ArgumentCaptor<Long> captorBehandling = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<MottattDokument> captorDokument = ArgumentCaptor.forClass(MottattDokument.class);
        ArgumentCaptor<BehandlingÅrsakType> captorBA = ArgumentCaptor.forClass(BehandlingÅrsakType.class);

        // Act
        håndterMottattDokumentTask.doTask(prosessTask);

        // Assert
        verify(innhentDokumentTjeneste).opprettFraTidligereBehandling(captorBehandling.capture(), captorDokument.capture(), captorBA.capture());
        assertThat(captorBehandling.getValue()).isEqualTo(BEHANDLING_ID);
        MottattDokument md = captorDokument.getValue();
        assertThat(md.getId()).isEqualTo(dokumentId);
        assertThat(md.getBehandlingId()).isEqualTo(BEHANDLING_ID);
        assertThat(captorBA.getValue()).isEqualTo(BehandlingÅrsakType.ETTER_KLAGE);
    }
}

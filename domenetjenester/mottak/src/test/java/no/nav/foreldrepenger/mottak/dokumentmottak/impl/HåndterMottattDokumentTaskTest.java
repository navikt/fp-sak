package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.Period;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import no.nav.foreldrepenger.behandling.BehandlingEventPubliserer;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.MottattDokumentPersisterer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

class HåndterMottattDokumentTaskTest extends EntityManagerAwareTest {

    private static final JournalpostId JOURNALPOST_ID = new JournalpostId("2");
    private static final DokumentTypeId DOKUMENTTYPE = DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL;
    private static final LocalDate FORSENDELSE_MOTTATT = LocalDate.now();
    private static final String PAYLOAD_XML = "inntektsmelding.xml";
    private static final  AktørId AKTØR_ID = new AktørId("0000000000000");

    private InnhentDokumentTjeneste innhentDokumentTjeneste;
    private HåndterMottattDokumentTask håndterMottattDokumentTask;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    private final MottattDokumentPersisterer mottattDokumentPersisterer =
        new MottattDokumentPersisterer(mock(BehandlingEventPubliserer.class));
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;

    @BeforeEach
    public void before() {
        var entityManager = getEntityManager();
        var mottatteDokumentRepository = new MottatteDokumentRepository(entityManager);
        var fristInnsendingPeriode = Period.ofWeeks(6);
        innhentDokumentTjeneste = mock(InnhentDokumentTjeneste.class);
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        mottatteDokumentTjeneste = new MottatteDokumentTjeneste(fristInnsendingPeriode, mottattDokumentPersisterer,
            mottatteDokumentRepository, repositoryProvider);
        håndterMottattDokumentTask = new HåndterMottattDokumentTask(innhentDokumentTjeneste, mottattDokumentPersisterer,
            mottatteDokumentTjeneste, repositoryProvider);
        fagsakRepository = new FagsakRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
    }

    private Long opprettBehandling(Fagsak fagsak) {
        var behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        return behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
    }

    private Fagsak opprettFagsak() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, NavBruker.opprettNyNB(AktørId.dummy()), new Saksnummer("9999"));
        fagsakRepository.opprettNy(fagsak);
        return fagsak;
    }

    @Test
    void skal_kalle_InnhentDokumentTjeneste_med_argumenter_fra_ProsessTask() throws Exception {
        // Arrange
        var xml = new FileToStringUtil().readFile(PAYLOAD_XML);
        var fagsak = opprettFagsak();
        var mottattDokument = new MottattDokument.Builder()
            .medFagsakId(fagsak.getId())
            .medJournalPostId(JOURNALPOST_ID)
            .medDokumentType(DOKUMENTTYPE)
            .medMottattDato(FORSENDELSE_MOTTATT)
            .medXmlPayload(xml)
            .medElektroniskRegistrert(true)
            .build();

        var dokumentId = mottatteDokumentTjeneste.lagreMottattDokumentPåFagsak(mottattDokument);

        var prosessTask = ProsessTaskData.forProsessTask(HåndterMottattDokumentTask.class);
        prosessTask.setFagsak(fagsak.getSaksnummer().getVerdi(), fagsak.getId());
        prosessTask.setProperty(HåndterMottattDokumentTask.MOTTATT_DOKUMENT_ID_KEY, dokumentId.toString());
        prosessTask.setProperty(HåndterMottattDokumentTask.BEHANDLING_ÅRSAK_TYPE_KEY, BehandlingÅrsakType.UDEFINERT.getKode());
        var captor = ArgumentCaptor.forClass(MottattDokument.class);

        // Act
        håndterMottattDokumentTask.doTask(prosessTask);

        // Assert
        verify(innhentDokumentTjeneste).utfør(captor.capture(), any(BehandlingÅrsakType.class));
    }

    @Test
    void skal_kalle_OpprettFraTidligereBehandling_med_argumenter_fra_ProsessTask() {
        // Arrange
        var fagsak = opprettFagsak();
        var behandlingId = opprettBehandling(fagsak);
        var mottattDokument = new MottattDokument.Builder()
            .medFagsakId(fagsak.getId())
            .medBehandlingId(behandlingId)
            .medJournalPostId(JOURNALPOST_ID)
            .medDokumentType(DOKUMENTTYPE)
            .medMottattDato(FORSENDELSE_MOTTATT)
            .medXmlPayload(PAYLOAD_XML)
            .medElektroniskRegistrert(true)
            .build();

        var dokumentId = mottatteDokumentTjeneste.lagreMottattDokumentPåFagsak(mottattDokument);

        var prosessTask = ProsessTaskData.forProsessTask(HåndterMottattDokumentTask.class);
        prosessTask.setBehandling(fagsak.getSaksnummer().getVerdi(), fagsak.getId(), behandlingId);
        prosessTask.setProperty(HåndterMottattDokumentTask.MOTTATT_DOKUMENT_ID_KEY, dokumentId.toString());
        prosessTask.setProperty(HåndterMottattDokumentTask.BEHANDLING_ÅRSAK_TYPE_KEY, BehandlingÅrsakType.ETTER_KLAGE.getKode());
        var captorBehandling = ArgumentCaptor.forClass(Long.class);
        var captorDokument = ArgumentCaptor.forClass(MottattDokument.class);
        var captorBA = ArgumentCaptor.forClass(BehandlingÅrsakType.class);

        // Act
        håndterMottattDokumentTask.doTask(prosessTask);

        // Assert
        verify(innhentDokumentTjeneste).opprettFraTidligereBehandling(captorBehandling.capture(), captorDokument.capture(), captorBA.capture());
        assertThat(captorBehandling.getValue()).isEqualTo(behandlingId);
        var md = captorDokument.getValue();
        assertThat(md.getId()).isEqualTo(dokumentId);
        assertThat(md.getBehandlingId()).isEqualTo(behandlingId);
        assertThat(captorBA.getValue()).isEqualTo(BehandlingÅrsakType.ETTER_KLAGE);
    }
}

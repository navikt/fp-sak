package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageMedholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.SaksbehandlingDokumentmottakTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.HåndterMottattDokumentTask;
import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

@CdiDbAwareTest
class OpprettNyFørstegangsbehandlingTest {

    private static final long MOTTATT_DOKUMENT_ID = 5L;
    private static final long MOTTATT_DOKUMENT_PAPIR_SØKNAD_ID = 3L;
    private static final long MOTTATT_DOKUMENT_EL_SØKNAD_ID = 1L;
    private static final long MOTTATT_DOKUMENT_IM_ID = 5L;

    private Behandling behandling;

    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private KlageRepository klageRepository;
    @Mock
    private ProsessTaskTjeneste taskTjeneste;
    private SaksbehandlingDokumentmottakTjeneste saksbehandlingDokumentmottakTjeneste;
    private BehandlingsoppretterTjeneste behandlingsoppretterTjeneste;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    MottattDokument.Builder md1 = new MottattDokument.Builder()
            .medJournalPostId(new JournalpostId("123"))
            .medDokumentType(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_ADOPSJON)
            .medMottattDato(LocalDate.now())
            .medElektroniskRegistrert(true)
            .medXmlPayload("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>") // Skal bare være en string slik at XmlPayLoad ikke er
                                                                                            // null
            .medId(MOTTATT_DOKUMENT_EL_SØKNAD_ID);
    MottattDokument.Builder md2 = new MottattDokument.Builder() // Annet dokument som ikke er søknad
            .medJournalPostId(new JournalpostId("456"))
            .medDokumentType(DokumentTypeId.UDEFINERT)
            .medMottattDato(LocalDate.now())
            .medElektroniskRegistrert(false)
            .medId(2L);
    MottattDokument.Builder md3 = new MottattDokument.Builder()
            .medJournalPostId(new JournalpostId("789"))
            .medDokumentType(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL)
            .medMottattDato(LocalDate.now())
            .medElektroniskRegistrert(false)
            .medId(MOTTATT_DOKUMENT_PAPIR_SØKNAD_ID);
    MottattDokument.Builder md4 = new MottattDokument.Builder()
            .medJournalPostId(new JournalpostId("789"))
            .medDokumentType(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL)
            .medMottattDato(LocalDate.now())
            .medElektroniskRegistrert(false)
            .medXmlPayload("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>") // Skal bare være en string slik at XmlPayLoad ikke er
                                                                                            // null
            .medId(4L);
    MottattDokument.Builder md5 = new MottattDokument.Builder()
            .medJournalPostId(new JournalpostId("357"))
            .medDokumentType(DokumentTypeId.INNTEKTSMELDING)
            .medMottattDato(LocalDate.now())
            .medElektroniskRegistrert(false)
            .medXmlPayload("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>") // Skal bare være en string slik at XmlPayLoad ikke er
                                                                                            // null
            .medId(MOTTATT_DOKUMENT_IM_ID);

    private Behandling opprettOgLagreBehandling() {
        return ScenarioMorSøkerEngangsstønad.forFødsel().lagre(repositoryProvider);
    }

    @BeforeEach
    void setup(EntityManager em) {
        mottatteDokumentTjeneste = mock(MottatteDokumentTjeneste.class);
        lenient().when(mottatteDokumentTjeneste.lagreMottattDokumentPåFagsak(any(MottattDokument.class))).thenReturn(MOTTATT_DOKUMENT_ID);

        repositoryProvider = Mockito.spy(new BehandlingRepositoryProvider(em));
        behandling = opprettOgLagreBehandling();
    }

    private void mockResterende() {
        saksbehandlingDokumentmottakTjeneste = new SaksbehandlingDokumentmottakTjeneste(taskTjeneste, mottatteDokumentTjeneste);

        behandlingsoppretterTjeneste = new BehandlingsoppretterTjeneste(
                repositoryProvider,
                saksbehandlingDokumentmottakTjeneste,
                null);
    }

    private void mockMottatteDokumentRepository(BehandlingRepositoryProvider repositoryProvider) {
        var mottatteDokumentRepository = mock(MottatteDokumentRepository.class);
        lenient().when(mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(behandling.getFagsakId())).thenAnswer(invocation -> {
            List<MottattDokument> mottatteDokumentList = new ArrayList<>();
            var md1d = md1.medFagsakId(behandling.getFagsakId()).medBehandlingId(behandling.getId()).build();
            md1d.setOpprettetTidspunkt(LocalDateTime.now().minusSeconds(1L));
            mottatteDokumentList.add(md1d);
            var md2d = md2.medFagsakId(behandling.getFagsakId()).medBehandlingId(behandling.getId()).build();
            md2d.setOpprettetTidspunkt(LocalDateTime.now().minusSeconds(1L));
            mottatteDokumentList.add(md2d);
            var md3d = md3.medFagsakId(behandling.getFagsakId()).medBehandlingId(behandling.getId()).build();
            md3d.setOpprettetTidspunkt(LocalDateTime.now());
            mottatteDokumentList.add(md3d);
            var md4d = md4.medFagsakId(behandling.getFagsakId()).medBehandlingId(behandling.getId()).build();
            md4d.setOpprettetTidspunkt(LocalDateTime.now().plusSeconds(1L));
            mottatteDokumentList.add(md4d);
            return mottatteDokumentList;
        });
        when(repositoryProvider.getMottatteDokumentRepository()).thenReturn(mottatteDokumentRepository);
        mockResterende();
    }

    private void mockMottatteDokumentRepositoryElsokMedBehandling(BehandlingRepositoryProvider repositoryProvider) {
        var mottatteDokumentRepository = mock(MottatteDokumentRepository.class);
        when(mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(behandling.getFagsakId())).thenAnswer(invocation -> {
            List<MottattDokument> mottatteDokumentList = new ArrayList<>();
            var md1d = md1.medFagsakId(behandling.getFagsakId()).medBehandlingId(behandling.getId()).build();
            md1d.setOpprettetTidspunkt(LocalDateTime.now().minusSeconds(1L));
            mottatteDokumentList.add(md1d);
            var md2d = md2.medFagsakId(behandling.getFagsakId()).medBehandlingId(behandling.getId()).build();
            md2d.setOpprettetTidspunkt(LocalDateTime.now().minusSeconds(1L));
            mottatteDokumentList.add(md2d);
            return mottatteDokumentList;
        });
        when(repositoryProvider.getMottatteDokumentRepository()).thenReturn(mottatteDokumentRepository);
        mockResterende();
    }

    private void mockMottatteDokumentRepositoryElsokUtenBehandling(BehandlingRepositoryProvider repositoryProvider) {
        var mottatteDokumentRepository = mock(MottatteDokumentRepository.class);
        when(mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(behandling.getFagsakId())).thenAnswer(invocation -> {
            List<MottattDokument> mottatteDokumentList = new ArrayList<>();
            var md1d = md1.medFagsakId(behandling.getFagsakId()).build();
            md1d.setOpprettetTidspunkt(LocalDateTime.now().minusSeconds(1L));
            mottatteDokumentList.add(md1d);
            return mottatteDokumentList;
        });
        when(repositoryProvider.getMottatteDokumentRepository()).thenReturn(mottatteDokumentRepository);
        mockResterende();
    }

    private void mockMottatteDokumentRepositoryImMedBehandling(BehandlingRepositoryProvider repositoryProvider) {
        var mottatteDokumentRepository = mock(MottatteDokumentRepository.class);
        when(mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(behandling.getFagsakId())).thenAnswer(invocation -> {
            List<MottattDokument> mottatteDokumentList = new ArrayList<>();
            var md5d = md5.medFagsakId(behandling.getFagsakId()).medBehandlingId(behandling.getId()).build();
            md5d.setOpprettetTidspunkt(LocalDateTime.now().minusSeconds(1L));
            mottatteDokumentList.add(md5d);
            return mottatteDokumentList;
        });
        when(repositoryProvider.getMottatteDokumentRepository()).thenReturn(mottatteDokumentRepository);
        mockResterende();
    }

    private void mockMottatteDokumentRepositoryUtenSøknadEllerIm(BehandlingRepositoryProvider repositoryProvider) {
        var mottatteDokumentRepository = mock(MottatteDokumentRepository.class);
        when(mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(behandling.getFagsakId())).thenAnswer(invocation -> {
            List<MottattDokument> mottatteDokumentList = new ArrayList<>();
            var md2d = md2.medFagsakId(behandling.getFagsakId()).medBehandlingId(behandling.getId()).build();
            md2d.setOpprettetTidspunkt(LocalDateTime.now().minusSeconds(1L));
            mottatteDokumentList.add(md2d);
            return mottatteDokumentList;
        });
        when(repositoryProvider.getMottatteDokumentRepository()).thenReturn(mottatteDokumentRepository);
        mockResterende();
    }

    @Test
    void skal_kaste_exception_når_behandling_fortsatt_er_åpen() {
        mockMottatteDokumentRepository(repositoryProvider);
        // Act and expect Exception
        var fagsakId = behandling.getFagsakId();
        var saksnummer = behandling.getSaksnummer();
        assertThrows(FunksjonellException.class, () -> behandlingsoppretterTjeneste.opprettNyFørstegangsbehandling(fagsakId, saksnummer, false));
    }

    @Test
    void skal_kaste_exception_når_behandling_ikke_eksisterer() {
        mockMottatteDokumentRepository(repositoryProvider);
        // Act and expect Exception
        var saksnummer = new Saksnummer("050");
        assertThrows(FunksjonellException.class, () -> behandlingsoppretterTjeneste.opprettNyFørstegangsbehandling(-1L, saksnummer, false));
    }

    @Test
    void skal_opprette_etter_klagebehandling() {
        // Arrange
        mockMottatteDokumentRepository(repositoryProvider);
        behandling.avsluttBehandling();

        var klage = Behandling.forKlage(behandling.getFagsak()).build();
        var behandlingRepository = repositoryProvider.getBehandlingRepository();
        var lås = behandlingRepository.taSkriveLås(klage);
        behandlingRepository.lagre(klage, lås);
        klageRepository.hentEvtOpprettKlageResultat(klage.getId());

        klageRepository.lagreVurderingsResultat(klage, KlageVurderingResultat.builder()
                .medKlageVurdertAv(KlageVurdertAv.NFP).medKlageMedholdÅrsak(KlageMedholdÅrsak.NYE_OPPLYSNINGER)
                .medKlageVurdering(KlageVurdering.MEDHOLD_I_KLAGE)
                .medBegrunnelse("bla bla"));
        klage.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(klage, repositoryProvider.getBehandlingRepository().taSkriveLås(klage));

        // Act
        behandlingsoppretterTjeneste.opprettNyFørstegangsbehandling(behandling.getFagsakId(), behandling.getSaksnummer(), true);

        // Assert
        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste, times(1)).lagre(captor.capture());
        var prosessTaskData = captor.getValue();
        verifiserProsessTaskData(behandling, prosessTaskData, MOTTATT_DOKUMENT_ID, false);
    }

    @Test
    void skal_opprette_uten_behandling() {
        // Arrange
        behandling.avsluttBehandling();
        mockMottatteDokumentRepositoryElsokUtenBehandling(repositoryProvider);

        // Act
        behandlingsoppretterTjeneste.opprettNyFørstegangsbehandling(behandling.getFagsakId(), behandling.getSaksnummer(), false);

        // Assert
        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste, times(1)).lagre(captor.capture());
        var prosessTaskData = captor.getValue();
        verifiserProsessTaskData(behandling, prosessTaskData, MOTTATT_DOKUMENT_EL_SØKNAD_ID, false);
    }

    @Test
    void skal_opprette_med_behandling() {
        // Arrange
        behandling.avsluttBehandling();
        mockMottatteDokumentRepositoryElsokMedBehandling(repositoryProvider);

        // Act
        behandlingsoppretterTjeneste.opprettNyFørstegangsbehandling(behandling.getFagsakId(), behandling.getSaksnummer(), false);

        // Assert
        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste, times(1)).lagre(captor.capture());
        var prosessTaskData = captor.getValue();
        verifiserProsessTaskData(behandling, prosessTaskData, MOTTATT_DOKUMENT_EL_SØKNAD_ID, true);
    }

    @Test
    void skal_opprette_med_behandling_basert_på_inntektsmelding_når_søknad_mangler() {
        // Arrange
        behandling.avsluttBehandling();
        mockMottatteDokumentRepositoryImMedBehandling(repositoryProvider);

        // Act
        behandlingsoppretterTjeneste.opprettNyFørstegangsbehandling(behandling.getFagsakId(), behandling.getSaksnummer(), false);

        // Assert
        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste, times(1)).lagre(captor.capture());
        var prosessTaskData = captor.getValue();
        verifiserProsessTaskData(behandling, prosessTaskData, MOTTATT_DOKUMENT_IM_ID, true);
    }

    @Test
    void skal_feile_når_det_verken_er_søknad_eller_inntektsmelding_i_mottatte_dokument() {
        // Arrange
        behandling.avsluttBehandling();
        mockMottatteDokumentRepositoryUtenSøknadEllerIm(repositoryProvider);

        // Act
        var fagsakId = behandling.getFagsakId();
        var saksnummer = behandling.getSaksnummer();
        assertThrows(FunksjonellException.class, () -> behandlingsoppretterTjeneste.opprettNyFørstegangsbehandling(fagsakId, saksnummer, false));
    }

    @Test
    void skal_feile_uten_tidligere_klagebehandling() {
        // Arrange
        mockMottatteDokumentRepository(repositoryProvider);
        behandling.avsluttBehandling();

        // Act
        var fagsakId = behandling.getFagsakId();
        var saksnummer = behandling.getSaksnummer();
        assertThrows(FunksjonellException.class, () -> behandlingsoppretterTjeneste.opprettNyFørstegangsbehandling(fagsakId, saksnummer, true));
    }

    @Test
    void skal_opprette_ny_førstegangsbehandling_når_behandlingen_er_åpen() {
        // Act
        mockMottatteDokumentRepository(repositoryProvider);
        behandlingsoppretterTjeneste.henleggÅpenFørstegangsbehandlingOgOpprettNy(behandling.getFagsakId(), behandling.getSaksnummer());

        // Assert
        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste, times(1)).lagre(captor.capture());
        var prosessTaskData = captor.getValue();
        verifiserProsessTaskData(behandling, prosessTaskData, MOTTATT_DOKUMENT_ID, false);
    }

    @Test
    void skal_opprette_ny_førstegangsbehandling_når_behandlingen_er_åpen_elektronisk() {
        // Act
        mockMottatteDokumentRepositoryElsokMedBehandling(repositoryProvider);
        behandlingsoppretterTjeneste.henleggÅpenFørstegangsbehandlingOgOpprettNy(behandling.getFagsakId(), behandling.getSaksnummer());

        // Assert
        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste, times(1)).lagre(captor.capture());
        var prosessTaskData = captor.getValue();
        verifiserProsessTaskData(behandling, prosessTaskData, MOTTATT_DOKUMENT_EL_SØKNAD_ID, true);
    }

    @Test
    void skal_kaste_funksjonell_feil_når_behandlingen_er_lukket() {
        // Arrange
        mockMottatteDokumentRepository(repositoryProvider);
        behandling.avsluttBehandling();

        // Act
        var fagsakId = behandling.getFagsakId();
        var saksnummer = behandling.getSaksnummer();
        assertThrows(FunksjonellException.class, () -> behandlingsoppretterTjeneste
                .henleggÅpenFørstegangsbehandlingOgOpprettNy(fagsakId, saksnummer));

        // Assert
        verify(taskTjeneste, times(0)).lagre(any(ProsessTaskData.class));
    }

    // Verifiserer at den opprettede prosesstasken stemmer overens med
    // MottattDokument-mock
    private void verifiserProsessTaskData(Behandling behandling, ProsessTaskData prosessTaskData, Long ventetDokument, boolean skalhabehandling) {

        assertThat(prosessTaskData.taskType()).isEqualTo(TaskType.forProsessTask(HåndterMottattDokumentTask.class));
        assertThat(prosessTaskData.getFagsakId()).isEqualTo(behandling.getFagsakId());
        assertThat(prosessTaskData.getSaksnummer()).isEqualTo(behandling.getSaksnummer().getVerdi());
        if (skalhabehandling) {
            assertThat(prosessTaskData.getBehandlingIdAsLong()).isEqualTo(behandling.getId());
        } else {
            assertThat(prosessTaskData.getBehandlingIdAsLong()).isNull();
        }
        assertThat(prosessTaskData.getPropertyValue(HåndterMottattDokumentTask.MOTTATT_DOKUMENT_ID_KEY))
                .isEqualTo(ventetDokument.toString());
    }
}

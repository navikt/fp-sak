package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageMedholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.SaksbehandlingDokumentmottakTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.HåndterMottattDokumentTask;
import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskEventPubliserer;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskRepositoryImpl;
import no.nav.vedtak.felles.testutilities.Whitebox;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@SuppressWarnings("deprecation")
@RunWith(CdiRunner.class)
public class OpprettNyFørstegangsbehandlingTest {

    private static final long MOTTATT_DOKUMENT_ID = 5L;
    private static final long MOTTATT_DOKUMENT_PAPIR_SØKNAD_ID = 3L;
    private static final long MOTTATT_DOKUMENT_EL_SØKNAD_ID = 1L;
    private static final long MOTTATT_DOKUMENT_IM_ID = 5L;

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private Behandling behandling;

    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private KlageRepository klageRepository;

    private ProsessTaskRepository prosessTaskRepository;
    private SaksbehandlingDokumentmottakTjeneste saksbehandlingDokumentmottakTjeneste;
    private BehandlingsoppretterApplikasjonTjeneste behandlingsoppretterApplikasjonTjeneste;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    MottattDokument.Builder md1 = new MottattDokument.Builder()
        .medJournalPostId(new JournalpostId("123"))
        .medDokumentType(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_ADOPSJON)
        .medMottattDato(LocalDate.now())
        .medElektroniskRegistrert(true)
        .medXmlPayload("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>") // Skal bare være en string slik at XmlPayLoad ikke er null
        .medId(MOTTATT_DOKUMENT_EL_SØKNAD_ID);
    MottattDokument.Builder md2 = new MottattDokument.Builder() //Annet dokument som ikke er søknad
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
        .medXmlPayload("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>") // Skal bare være en string slik at XmlPayLoad ikke er null
        .medId(4L);
    MottattDokument.Builder md5 = new MottattDokument.Builder()
        .medJournalPostId(new JournalpostId("357"))
        .medDokumentType(DokumentTypeId.INNTEKTSMELDING)
        .medMottattDato(LocalDate.now())
        .medElektroniskRegistrert(false)
        .medXmlPayload("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>") // Skal bare være en string slik at XmlPayLoad ikke er null
        .medId(MOTTATT_DOKUMENT_IM_ID);

    private Behandling opprettOgLagreBehandling() {
        return ScenarioMorSøkerEngangsstønad.forFødsel().lagre(repositoryProvider);
    }

    @Before
    public void setup() {
        ProsessTaskEventPubliserer prosessTaskEventPubliserer = Mockito.mock(ProsessTaskEventPubliserer.class);
        Mockito.doNothing().when(prosessTaskEventPubliserer).fireEvent(Mockito.any(ProsessTaskData.class), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        prosessTaskRepository = Mockito.spy(new ProsessTaskRepositoryImpl(repoRule.getEntityManager(), null, prosessTaskEventPubliserer));
        mottatteDokumentTjeneste = mock(MottatteDokumentTjeneste.class);
        when(mottatteDokumentTjeneste.lagreMottattDokumentPåFagsak(any(MottattDokument.class))).thenReturn(MOTTATT_DOKUMENT_ID);

        repositoryProvider = Mockito.spy(new BehandlingRepositoryProvider(repoRule.getEntityManager()));
        behandling = opprettOgLagreBehandling();
    }

    private void mockResterende() {
        saksbehandlingDokumentmottakTjeneste = new SaksbehandlingDokumentmottakTjeneste(prosessTaskRepository, mottatteDokumentTjeneste);

        behandlingsoppretterApplikasjonTjeneste = new BehandlingsoppretterApplikasjonTjeneste(
            repositoryProvider,
            saksbehandlingDokumentmottakTjeneste,
            null,
            null,
            null,
            null);
    }

    private void mockMottatteDokumentRepository(BehandlingRepositoryProvider repositoryProvider) {
        MottatteDokumentRepository mottatteDokumentRepository = mock(MottatteDokumentRepository.class);
        when(mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(behandling.getFagsakId())).thenAnswer(invocation -> {
            List<MottattDokument> mottatteDokumentList = new ArrayList<>();
            MottattDokument md1d = md1.medFagsakId(behandling.getFagsakId()).medBehandlingId(behandling.getId()).build();
            Whitebox.setInternalState(md1d, "opprettetTidspunkt", LocalDateTime.now().minusSeconds(1L));
            mottatteDokumentList.add(md1d);
            MottattDokument md2d = md2.medFagsakId(behandling.getFagsakId()).medBehandlingId(behandling.getId()).build();
            Whitebox.setInternalState(md2d, "opprettetTidspunkt", LocalDateTime.now().minusSeconds(1L));
            mottatteDokumentList.add(md2d);
            MottattDokument md3d = md3.medFagsakId(behandling.getFagsakId()).medBehandlingId(behandling.getId()).build();
            Whitebox.setInternalState(md3d, "opprettetTidspunkt", LocalDateTime.now());
            mottatteDokumentList.add(md3d);
            MottattDokument md4d = md4.medFagsakId(behandling.getFagsakId()).medBehandlingId(behandling.getId()).build();
            Whitebox.setInternalState(md4d, "opprettetTidspunkt", LocalDateTime.now().plusSeconds(1L));
            mottatteDokumentList.add(md4d);
            return mottatteDokumentList;
        });
        when(repositoryProvider.getMottatteDokumentRepository()).thenReturn(mottatteDokumentRepository);
        mockResterende();
    }

    private void mockMottatteDokumentRepositoryElsokMedBehandling(BehandlingRepositoryProvider repositoryProvider) {
        MottatteDokumentRepository mottatteDokumentRepository = mock(MottatteDokumentRepository.class);
        when(mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(behandling.getFagsakId())).thenAnswer(invocation -> {
            List<MottattDokument> mottatteDokumentList = new ArrayList<>();
            MottattDokument md1d = md1.medFagsakId(behandling.getFagsakId()).medBehandlingId(behandling.getId()).build();
            Whitebox.setInternalState(md1d, "opprettetTidspunkt", LocalDateTime.now().minusSeconds(1L));
            mottatteDokumentList.add(md1d);
            MottattDokument md2d = md2.medFagsakId(behandling.getFagsakId()).medBehandlingId(behandling.getId()).build();
            Whitebox.setInternalState(md2d, "opprettetTidspunkt", LocalDateTime.now().minusSeconds(1L));
            mottatteDokumentList.add(md2d);
            return mottatteDokumentList;
        });
        when(repositoryProvider.getMottatteDokumentRepository()).thenReturn(mottatteDokumentRepository);
        mockResterende();
    }

    private void mockMottatteDokumentRepositoryElsokUtenBehandling(BehandlingRepositoryProvider repositoryProvider) {
        MottatteDokumentRepository mottatteDokumentRepository = mock(MottatteDokumentRepository.class);
        when(mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(behandling.getFagsakId())).thenAnswer(invocation -> {
            List<MottattDokument> mottatteDokumentList = new ArrayList<>();
            MottattDokument md1d = md1.medFagsakId(behandling.getFagsakId()).build();
            Whitebox.setInternalState(md1d, "opprettetTidspunkt", LocalDateTime.now().minusSeconds(1L));
            mottatteDokumentList.add(md1d);
            return mottatteDokumentList;
        });
        when(repositoryProvider.getMottatteDokumentRepository()).thenReturn(mottatteDokumentRepository);
        mockResterende();
    }

    private void mockMottatteDokumentRepositoryImMedBehandling(BehandlingRepositoryProvider repositoryProvider) {
        MottatteDokumentRepository mottatteDokumentRepository = mock(MottatteDokumentRepository.class);
        when(mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(behandling.getFagsakId())).thenAnswer(invocation -> {
            List<MottattDokument> mottatteDokumentList = new ArrayList<>();
            MottattDokument md5d = md5.medFagsakId(behandling.getFagsakId()).medBehandlingId(behandling.getId()).build();
            Whitebox.setInternalState(md5d, "opprettetTidspunkt", LocalDateTime.now().minusSeconds(1L));
            mottatteDokumentList.add(md5d);
            return mottatteDokumentList;
        });
        when(repositoryProvider.getMottatteDokumentRepository()).thenReturn(mottatteDokumentRepository);
        mockResterende();
    }

    private void mockMottatteDokumentRepositoryUtenSøknadEllerIm(BehandlingRepositoryProvider repositoryProvider) {
        MottatteDokumentRepository mottatteDokumentRepository = mock(MottatteDokumentRepository.class);
        when(mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(behandling.getFagsakId())).thenAnswer(invocation -> {
            List<MottattDokument> mottatteDokumentList = new ArrayList<>();
            MottattDokument md2d = md2.medFagsakId(behandling.getFagsakId()).medBehandlingId(behandling.getId()).build();
            Whitebox.setInternalState(md2d, "opprettetTidspunkt", LocalDateTime.now().minusSeconds(1L));
            mottatteDokumentList.add(md2d);
            return mottatteDokumentList;
        });
        when(repositoryProvider.getMottatteDokumentRepository()).thenReturn(mottatteDokumentRepository);
        mockResterende();
    }

    @Test(expected = FunksjonellException.class)
    public void skal_kaste_exception_når_behandling_fortsatt_er_åpen() {
        mockMottatteDokumentRepository(repositoryProvider);
        //Act and expect Exception
        behandlingsoppretterApplikasjonTjeneste.opprettNyFørstegangsbehandling(behandling.getFagsakId(), behandling.getFagsak().getSaksnummer(), false);
    }

    @Test(expected = FunksjonellException.class)
    public void skal_kaste_exception_når_behandling_ikke_eksisterer() {
        mockMottatteDokumentRepository(repositoryProvider);
        //Act and expect Exception
        behandlingsoppretterApplikasjonTjeneste.opprettNyFørstegangsbehandling(-1L, new Saksnummer("50"), false);
    }

    @Test
    public void skal_opprette_etter_klagebehandling() {
        //Arrange
        mockMottatteDokumentRepository(repositoryProvider);
        behandling.avsluttBehandling();

        Behandling klage = Behandling.forKlage(behandling.getFagsak()).build();
        BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
        BehandlingLås lås = behandlingRepository.taSkriveLås(klage);
        behandlingRepository.lagre(klage, lås);
        klageRepository.leggTilKlageResultat(klage);

        klageRepository.lagreVurderingsResultat(klage,KlageVurderingResultat.builder()
                .medKlageVurdertAv(KlageVurdertAv.NFP).medKlageMedholdÅrsak(KlageMedholdÅrsak.NYE_OPPLYSNINGER).medKlageVurdering(KlageVurdering.MEDHOLD_I_KLAGE)
                .medBegrunnelse("bla bla").medVedtaksdatoPåklagdBehandling(LocalDate.now()));
        klage.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(klage, repositoryProvider.getBehandlingRepository().taSkriveLås(klage));

        //Act
        behandlingsoppretterApplikasjonTjeneste.opprettNyFørstegangsbehandling(behandling.getFagsakId(), behandling.getFagsak().getSaksnummer(),true);

        // Assert
        ArgumentCaptor<ProsessTaskData> captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskRepository, times(1)).lagre(captor.capture());
        ProsessTaskData prosessTaskData = captor.getValue();
        verifiserProsessTaskData(behandling, prosessTaskData, MOTTATT_DOKUMENT_ID,false);
    }

    @Test
    public void skal_opprette_uten_behandling() {
        //Arrange
        behandling.avsluttBehandling();
        mockMottatteDokumentRepositoryElsokUtenBehandling(repositoryProvider);

        //Act
        behandlingsoppretterApplikasjonTjeneste.opprettNyFørstegangsbehandling(behandling.getFagsakId(), behandling.getFagsak().getSaksnummer(),false);

        // Assert
        ArgumentCaptor<ProsessTaskData> captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskRepository, times(1)).lagre(captor.capture());
        ProsessTaskData prosessTaskData = captor.getValue();
        verifiserProsessTaskData(behandling, prosessTaskData, MOTTATT_DOKUMENT_EL_SØKNAD_ID, false);
    }

    @Test
    public void skal_opprette_med_behandling() {
        //Arrange
        behandling.avsluttBehandling();
        mockMottatteDokumentRepositoryElsokMedBehandling(repositoryProvider);

        //Act
        behandlingsoppretterApplikasjonTjeneste.opprettNyFørstegangsbehandling(behandling.getFagsakId(), behandling.getFagsak().getSaksnummer(),false);

        // Assert
        ArgumentCaptor<ProsessTaskData> captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskRepository, times(1)).lagre(captor.capture());
        ProsessTaskData prosessTaskData = captor.getValue();
        verifiserProsessTaskData(behandling, prosessTaskData, MOTTATT_DOKUMENT_EL_SØKNAD_ID, true);
    }

    @Test
    public void skal_opprette_med_behandling_basert_på_inntektsmelding_når_søknad_mangler() {
        // Arrange
        behandling.avsluttBehandling();
        mockMottatteDokumentRepositoryImMedBehandling(repositoryProvider);

        // Act
        behandlingsoppretterApplikasjonTjeneste.opprettNyFørstegangsbehandling(behandling.getFagsakId(), behandling.getFagsak().getSaksnummer(),false);

        // Assert
        ArgumentCaptor<ProsessTaskData> captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskRepository, times(1)).lagre(captor.capture());
        ProsessTaskData prosessTaskData = captor.getValue();
        verifiserProsessTaskData(behandling, prosessTaskData, MOTTATT_DOKUMENT_IM_ID, true);
    }

    @Test(expected = FunksjonellException.class)
    public void skal_feile_når_det_verken_er_søknad_eller_inntektsmelding_i_mottatte_dokument() {
        // Arrange
        behandling.avsluttBehandling();
        mockMottatteDokumentRepositoryUtenSøknadEllerIm(repositoryProvider);

        // Act
        behandlingsoppretterApplikasjonTjeneste.opprettNyFørstegangsbehandling(behandling.getFagsakId(), behandling.getFagsak().getSaksnummer(),false);
    }

    @Test(expected = FunksjonellException.class)
    public void skal_feile_uten_tidligere_klagebehandling() {
        //Arrange
        mockMottatteDokumentRepository(repositoryProvider);
        behandling.avsluttBehandling();

        //Act
        behandlingsoppretterApplikasjonTjeneste.opprettNyFørstegangsbehandling(behandling.getFagsakId(), behandling.getFagsak().getSaksnummer(),true);
    }

    @Test
    public void skal_opprette_ny_førstegangsbehandling_når_behandlingen_er_åpen() {
        //Act
        mockMottatteDokumentRepository(repositoryProvider);
        behandlingsoppretterApplikasjonTjeneste.henleggÅpenFørstegangsbehandlingOgOpprettNy(behandling.getFagsakId(), behandling.getFagsak().getSaksnummer());

        //Assert
        ArgumentCaptor<ProsessTaskData> captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskRepository, times(1)).lagre(captor.capture());
        ProsessTaskData prosessTaskData = captor.getValue();
        verifiserProsessTaskData(behandling, prosessTaskData, MOTTATT_DOKUMENT_ID,false);
    }

    @Test
    public void skal_opprette_ny_førstegangsbehandling_når_behandlingen_er_åpen_elektronisk() {
        //Act
        mockMottatteDokumentRepositoryElsokMedBehandling(repositoryProvider);
        behandlingsoppretterApplikasjonTjeneste.henleggÅpenFørstegangsbehandlingOgOpprettNy(behandling.getFagsakId(), behandling.getFagsak().getSaksnummer());

        //Assert
        ArgumentCaptor<ProsessTaskData> captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskRepository, times(1)).lagre(captor.capture());
        ProsessTaskData prosessTaskData = captor.getValue();
        verifiserProsessTaskData(behandling, prosessTaskData, MOTTATT_DOKUMENT_EL_SØKNAD_ID,true);
    }

    @Test(expected = FunksjonellException.class)
    public void skal_kaste_funksjonell_feil_når_behandlingen_er_lukket() {
        //Arrange
        mockMottatteDokumentRepository(repositoryProvider);
        behandling.avsluttBehandling();

        //Act
        behandlingsoppretterApplikasjonTjeneste.henleggÅpenFørstegangsbehandlingOgOpprettNy(behandling.getFagsakId(), behandling.getFagsak().getSaksnummer());

        //Assert
        verify(prosessTaskRepository, times(0)).lagre(any(ProsessTaskData.class));
    }

    //Verifiserer at den opprettede prosesstasken stemmer overens med MottattDokument-mock
    private void verifiserProsessTaskData(Behandling behandling, ProsessTaskData prosessTaskData, Long ventetDokument, boolean skalhabehandling) {

        assertThat(prosessTaskData.getTaskType()).isEqualTo(HåndterMottattDokumentTask.TASKTYPE);
        assertThat(prosessTaskData.getFagsakId()).isEqualTo(behandling.getFagsakId());
        if (skalhabehandling) {
            assertThat(prosessTaskData.getBehandlingId()).isEqualTo(behandling.getId());
        } else {
            assertThat(prosessTaskData.getBehandlingId()).isNull();
        }
        assertThat(prosessTaskData.getPropertyValue(HåndterMottattDokumentTask.MOTTATT_DOKUMENT_ID_KEY))
            .isEqualTo(ventetDokument.toString());
    }
}

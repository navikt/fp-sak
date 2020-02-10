package no.nav.foreldrepenger.mottak.dokumentmottak.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.mottak.dokumentmottak.InngåendeSaksdokument;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.SaksbehandlingDokumentmottakTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.HåndterMottattDokumentTaskProperties;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

public class SaksbehandlingDokumentmottakTjenesteTest {

    private static final Long FAGSAK_ID = 1L;
    private static final JournalpostId JOURNALPOST_ID = new JournalpostId("2");
    private static final DokumentTypeId DOKUMENTTYPE = DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL;
    private static final DokumentKategori DOKUMENTKATEGORI = DokumentKategori.SØKNAD;
    private static final LocalDate FORSENDELSE_MOTTATT = LocalDate.now();
    private static final Boolean ELEKTRONISK_SØKNAD = Boolean.TRUE;
    private static final String PAYLOAD_XML = "<test></test>";

    private BehandlingTema behandlingTema = BehandlingTema.ENGANGSSTØNAD_FØDSEL;

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private ProsessTaskRepository prosessTaskRepository;
    private SaksbehandlingDokumentmottakTjeneste saksbehandlingDokumentmottakTjeneste;

    @Before
    public void before() {
        prosessTaskRepository = mock(ProsessTaskRepository.class);
        MottatteDokumentTjeneste mottatteDokumentTjeneste = mock(MottatteDokumentTjeneste.class);
        saksbehandlingDokumentmottakTjeneste = new SaksbehandlingDokumentmottakTjeneste(prosessTaskRepository, mottatteDokumentTjeneste);
    }

    @Test
    public void skal_ta_imot_ankommet_saksdokument_og_opprette_prosesstask() {
        // Arrange
        InngåendeSaksdokument saksdokument = InngåendeSaksdokument.builder()
                .medFagsakId(FAGSAK_ID)
                .medJournalpostId(JOURNALPOST_ID)
                .medBehandlingTema(behandlingTema)
                .medDokumentTypeId(DOKUMENTTYPE.getKode())
                .medDokumentKategori(DOKUMENTKATEGORI)
                .medForsendelseMottatt(FORSENDELSE_MOTTATT)
            .medForsendelseMottatt(LocalDateTime.now())
                .medElektroniskSøknad(ELEKTRONISK_SØKNAD)
                .medPayloadXml(PAYLOAD_XML)
                .build();
        ArgumentCaptor<ProsessTaskData> captor = ArgumentCaptor.forClass(ProsessTaskData.class);

        // Act
        saksbehandlingDokumentmottakTjeneste.dokumentAnkommet(saksdokument);

        // Assert
        verify(prosessTaskRepository).lagre(captor.capture());
        ProsessTaskData prosessTaskData = captor.getValue();
        assertThat(prosessTaskData.getTaskType()).isEqualTo(HåndterMottattDokumentTaskProperties.TASKTYPE);
        assertThat(prosessTaskData.getFagsakId()).isEqualTo(FAGSAK_ID);
    }

    @Test
    public void skal_støtte_at_journalpostId_er_null() {
        // Arrange
        InngåendeSaksdokument saksdokument = InngåendeSaksdokument.builder()
                .medFagsakId(FAGSAK_ID)
                .medJournalpostId(null)
                .medBehandlingTema(behandlingTema)
                .medDokumentTypeId(DOKUMENTTYPE.getKode())
                .medDokumentKategori(DOKUMENTKATEGORI)
                .medForsendelseMottatt(FORSENDELSE_MOTTATT)
            .medForsendelseMottatt(LocalDateTime.now())
                .medElektroniskSøknad(ELEKTRONISK_SØKNAD)
                .medPayloadXml(PAYLOAD_XML)
                .build();
        ArgumentCaptor<ProsessTaskData> captor = ArgumentCaptor.forClass(ProsessTaskData.class);

        // Act
        saksbehandlingDokumentmottakTjeneste.dokumentAnkommet(saksdokument);

        // Assert
        verify(prosessTaskRepository).lagre(captor.capture());
    }

    @Test
    public void motta_ubehandlet() {
        // Arrange
        MottattDokument md1 = new MottattDokument.Builder()
            .medJournalPostId(new JournalpostId("123"))
            .medFagsakId(456L)
            .medDokumentType(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL)
            .medMottattDato(LocalDate.now())
            .medElektroniskRegistrert(true)
            .medXmlPayload("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>") // Skal bare være en string slik at XmlPayLoad ikke er null
            .medId(1L).build();
        ArgumentCaptor<ProsessTaskData> captor = ArgumentCaptor.forClass(ProsessTaskData.class);

        // Act
        saksbehandlingDokumentmottakTjeneste.mottaUbehandletSøknad(md1, BehandlingÅrsakType.UDEFINERT);

        // Assert
        verify(prosessTaskRepository).lagre(captor.capture());
        ProsessTaskData data = captor.getValue();
        assertThat(data.getFagsakId()).isEqualTo(456L);
        assertThat(data.getBehandlingId()).isNull();
        assertThat(data.getPropertyValue(HåndterMottattDokumentTaskProperties.MOTTATT_DOKUMENT_ID_KEY)).isEqualTo("1");
    }

    @Test
    public void motta_behandlet() {
        // Arrange
        var behandling = mock(Behandling.class);
        when(behandling.getId()).thenReturn(789L);
        when(behandling.getFagsakId()).thenReturn(456L);
        when(behandling.getAktørId()).thenReturn(new AktørId("0000000000000"));
        MottattDokument md1 = new MottattDokument.Builder()
            .medJournalPostId(new JournalpostId("123"))
            .medFagsakId(456L)
            .medBehandlingId(789L)
            .medDokumentType(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL)
            .medMottattDato(LocalDate.now())
            .medElektroniskRegistrert(true)
            .medXmlPayload("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>") // Skal bare være en string slik at XmlPayLoad ikke er null
            .medId(1L).build();
        ArgumentCaptor<ProsessTaskData> captor = ArgumentCaptor.forClass(ProsessTaskData.class);

        // Act
        saksbehandlingDokumentmottakTjeneste.opprettFraTidligereBehandling(md1, behandling, BehandlingÅrsakType.ETTER_KLAGE);

        // Assert
        verify(prosessTaskRepository).lagre(captor.capture());
        ProsessTaskData data = captor.getValue();
        assertThat(data.getFagsakId()).isEqualTo(456L);
        assertThat(data.getBehandlingId()).isEqualTo(789L);
        assertThat(data.getPropertyValue(HåndterMottattDokumentTaskProperties.MOTTATT_DOKUMENT_ID_KEY)).isEqualTo("1");
        assertThat(data.getPropertyValue(HåndterMottattDokumentTaskProperties.BEHANDLING_ÅRSAK_TYPE_KEY)).isEqualToIgnoringCase(BehandlingÅrsakType.ETTER_KLAGE.getKode());
    }
}

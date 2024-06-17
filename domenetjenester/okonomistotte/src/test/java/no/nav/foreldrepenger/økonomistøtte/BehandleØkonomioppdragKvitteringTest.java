package no.nav.foreldrepenger.økonomistøtte;

import static no.nav.foreldrepenger.økonomistøtte.OppdragTestDataHelper.lagOppdrag110;
import static no.nav.foreldrepenger.økonomistøtte.OppdragTestDataHelper.oppdragskontrollUtenOppdrag;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.OppdragKvittering;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.Alvorlighetsgrad;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomioppdragRepository;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

public class BehandleØkonomioppdragKvitteringTest {

    private static final Long PROSESSTASKID = 33L;

    public static final Long FAGSYSTEMID_BRUKER = 124L;

    public static final Long FAGSYSTEMID_ARBEIDSGIVER = 256L;

    public static final KodeEndring KODEENDRING = KodeEndring.NY;

    public static final KodeFagområde KODEFAGOMRADE_ES = KodeFagområde.REFUTG;

    public static final KodeFagområde KODEFAGOMRADE_FP = KodeFagområde.FP;

    public static final KodeFagområde KODEFAGOMRADE_FPREF = KodeFagområde.FPREF;

    public static final String OPPDRAGGJELDERID = "01010101010";

    public static final String SAKSBEHID = "aa000000";

    public static final Integer GRAD = 100;

    public static final String REFUNDERES_ID = "123456789";

    public static final String VEDTAKID = "VedtakId";

    private static final Alvorlighetsgrad KVITTERING_OK = Alvorlighetsgrad.OK;

    private static final String KVITTERING_MELDING_OK = "Oppdrag utført";

    public static final Long BEHANDLINGID_ES = 126L;

    public static final Long BEHANDLINGID_FP = 237L;

    private static final Alvorlighetsgrad KVITTERING_FEIL = Alvorlighetsgrad.FEIL;

    private static final String KVITTERING_MELDING_FEIL = "Oppdrag ikke utført";

    private static final String KVITTERING_MELDINGKODE_FEIL = "QWERTY12";

    private BehandleØkonomioppdragKvittering behandleØkonomioppdragKvittering;

    private ProsessTaskTjeneste taskTjeneste;
    private ØkonomioppdragRepository økonomioppdragRepository;
    private BehandleNegativeKvitteringTjeneste behandleHendelseØkonomioppdrag;

    private ProsessTaskData prosessTaskData;

    @BeforeEach
    void setUp() {
        taskTjeneste = mock(ProsessTaskTjeneste.class);
        økonomioppdragRepository = mock(ØkonomioppdragRepository.class);
        behandleHendelseØkonomioppdrag = mock(BehandleNegativeKvitteringTjeneste.class);
        behandleØkonomioppdragKvittering = new BehandleØkonomioppdragKvittering(taskTjeneste, økonomioppdragRepository,
            behandleHendelseØkonomioppdrag);

        var testTask = ProsessTaskData.forTaskType(new TaskType("testTask"));
        testTask.setStatus(ProsessTaskStatus.VENTER_SVAR);
        testTask.setId(PROSESSTASKID);
        prosessTaskData = testTask;
    }

    @Test
    void skal_motta_hendelse_når_positiv_kvittering_ES() {
        // Arrange
        var oppdrag = oppdragskontrollUtenOppdrag(new Saksnummer("3500"), BEHANDLINGID_ES, PROSESSTASKID);
        lagOppdrag110(oppdrag, 1L, KodeFagområde.REFUTG, true, true, false);

        when(økonomioppdragRepository.hentOppdragUtenKvittering(FAGSYSTEMID_BRUKER, BEHANDLINGID_ES)).thenReturn(oppdrag.getOppdrag110Liste().get(0));
        when(taskTjeneste.finn(PROSESSTASKID)).thenReturn(prosessTaskData);
        var kvittering = opprettKvittering(KVITTERING_OK, null, KVITTERING_MELDING_OK, FAGSYSTEMID_BRUKER, false);

        // Act
        behandleØkonomioppdragKvittering.behandleKvittering(kvittering);

        // Assert
        assertThat(oppdrag.getVenterKvittering()).isFalse();
        verify(økonomioppdragRepository).lagre(oppdrag);
        verify(økonomioppdragRepository, times(1)).lagre(any(OppdragKvittering.class));
        verify(taskTjeneste).mottaHendelse(eq(prosessTaskData), eq(BehandleØkonomioppdragKvittering.ØKONOMI_OPPDRAG_KVITTERING), any());
    }

    @Test
    void skal_ikke_motta_hendelse_når_negativ_kvittering_ES() {
        // Arrange
        var oppdrag = oppdragskontrollUtenOppdrag(new Saksnummer("3500"), BEHANDLINGID_ES, PROSESSTASKID);
        lagOppdrag110(oppdrag, 1L, KodeFagområde.REFUTG, true, true, false);

        when(økonomioppdragRepository.hentOppdragUtenKvittering(FAGSYSTEMID_BRUKER, BEHANDLINGID_ES)).thenReturn(oppdrag.getOppdrag110Liste().get(0));
        when(taskTjeneste.finn(PROSESSTASKID)).thenReturn(prosessTaskData);
        var kvittering = opprettKvittering(KVITTERING_FEIL, KVITTERING_MELDINGKODE_FEIL, KVITTERING_MELDING_FEIL, FAGSYSTEMID_BRUKER, false);

        // Act
        behandleØkonomioppdragKvittering.behandleKvittering(kvittering);

        // Assert
        assertThat(oppdrag.getVenterKvittering()).isFalse();
        verify(økonomioppdragRepository).lagre(oppdrag);
        verify(økonomioppdragRepository, times(1)).lagre(any(OppdragKvittering.class));
        verify(taskTjeneste, never()).mottaHendelse(any(), any(), any());
    }

    @Test
    void skal_motta_hendelse_når_positiv_kvittering_FP() {
        // Arrange
        var oppdrag = oppdragskontrollUtenOppdrag(new Saksnummer("3500"), BEHANDLINGID_FP, PROSESSTASKID);
        var oppdragBruker = OppdragTestDataHelper.lagOppdrag110FPBruker(oppdrag, FAGSYSTEMID_BRUKER);
        var oppdragAg = OppdragTestDataHelper.lagOppdrag110FPArbeidsgiver(oppdrag, FAGSYSTEMID_ARBEIDSGIVER);

        when(økonomioppdragRepository.hentOppdragUtenKvittering(FAGSYSTEMID_BRUKER, BEHANDLINGID_FP)).thenReturn(oppdragBruker);
        when(økonomioppdragRepository.hentOppdragUtenKvittering(FAGSYSTEMID_ARBEIDSGIVER, BEHANDLINGID_FP)).thenReturn(oppdragAg);
        when(taskTjeneste.finn(PROSESSTASKID)).thenReturn(prosessTaskData);
        var kvittering_1 = opprettKvittering(KVITTERING_OK, null, KVITTERING_MELDING_OK, FAGSYSTEMID_BRUKER, true);
        var kvittering_2 = opprettKvittering(KVITTERING_OK, null, KVITTERING_MELDING_OK, FAGSYSTEMID_ARBEIDSGIVER, true);

        // Act
        behandleØkonomioppdragKvittering.behandleKvittering(kvittering_1);
        behandleØkonomioppdragKvittering.behandleKvittering(kvittering_2);

        // Assert
        assertThat(oppdrag.getVenterKvittering()).isFalse();
        verify(økonomioppdragRepository, times(1)).lagre(oppdrag);
        verify(økonomioppdragRepository, times(2)).lagre(any(OppdragKvittering.class));
        verify(taskTjeneste).mottaHendelse(eq(prosessTaskData), eq(BehandleØkonomioppdragKvittering.ØKONOMI_OPPDRAG_KVITTERING), any());
        oppdrag.getOppdrag110Liste().forEach(o110 -> {
            assertThat(o110.getOppdragKvittering()).isNotNull();
            assertThat(o110.getOppdragKvittering().getOppdrag110()).isNotNull();
        });
    }

    @Test
    void skal_kaste_exception_hvis_ingen_opp110_uten_kvittering_finnes_FP() {
        // Arrange
        var oppdrag = oppdragskontrollUtenOppdrag(new Saksnummer("3500"), BEHANDLINGID_FP, PROSESSTASKID);

        var oppdrag110 = OppdragTestDataHelper.lagOppdrag110FPBruker(oppdrag, FAGSYSTEMID_BRUKER);
        OppdragKvittering.builder().medAlvorlighetsgrad(KVITTERING_OK).medOppdrag110(oppdrag110).build();

        when(økonomioppdragRepository.hentOppdragUtenKvittering(FAGSYSTEMID_BRUKER, BEHANDLINGID_FP)).thenReturn(oppdrag110);
        when(taskTjeneste.finn(PROSESSTASKID)).thenReturn(prosessTaskData);
        var kvittering_1 = opprettKvittering(KVITTERING_OK, null, KVITTERING_MELDING_OK, FAGSYSTEMID_BRUKER, true);

        // Act
        assertThatThrownBy(() -> behandleØkonomioppdragKvittering.behandleKvittering(kvittering_1)).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Mottat økonomi kvittering kan ikke overskrive en allerede eksisterende kvittering!");
    }

    @Test
    void skal_finne_riktig_oppdrag_hvis_to_med_identisk_fagsystemid_finnes_men_kun_en_uten_kvittering_FP() {
        // Arrange
        var oppdrag = oppdragskontrollUtenOppdrag(new Saksnummer("3500"), BEHANDLINGID_FP, PROSESSTASKID);

        var oppdrag110 = OppdragTestDataHelper.lagOppdrag110FPBruker(oppdrag, FAGSYSTEMID_BRUKER);
        oppdrag110.setOpprettetTidspunkt(LocalDateTime.now().minusDays(1));
        OppdragKvittering.builder().medAlvorlighetsgrad(KVITTERING_OK).medOppdrag110(oppdrag110).build();
        var oppdragBruker2 = OppdragTestDataHelper.lagOppdrag110FPBruker(oppdrag, FAGSYSTEMID_BRUKER);
        oppdragBruker2.setOpprettetTidspunkt(LocalDateTime.now());
        when(økonomioppdragRepository.hentOppdragUtenKvittering(FAGSYSTEMID_BRUKER, BEHANDLINGID_FP)).thenReturn(oppdragBruker2);
        var kvittering_1 = opprettKvittering(KVITTERING_OK, null, KVITTERING_MELDING_OK, FAGSYSTEMID_BRUKER, true);
        when(taskTjeneste.finn(PROSESSTASKID)).thenReturn(prosessTaskData);

        // Act
        behandleØkonomioppdragKvittering.behandleKvittering(kvittering_1);

        // Assert
        assertThat(oppdrag.getVenterKvittering()).isFalse();
        verify(økonomioppdragRepository, times(1)).lagre(oppdrag);
        verify(taskTjeneste).mottaHendelse(eq(prosessTaskData), eq(BehandleØkonomioppdragKvittering.ØKONOMI_OPPDRAG_KVITTERING), any());
        oppdrag.getOppdrag110Liste().forEach(o110 -> {
            assertThat(o110.getOppdragKvittering()).isNotNull();
            assertThat(o110.getOppdragKvittering().getOppdrag110()).isNotNull();
        });
    }

    @Test
    void skal_kaste_exception_hvis_flere_opp110_med_samme_fagsystemId_uten_kvittering_finnes_FP() {
        // Arrange
        var oppdrag = oppdragskontrollUtenOppdrag(new Saksnummer("3500"), BEHANDLINGID_FP, PROSESSTASKID);
        var oppdragBruker1 = OppdragTestDataHelper.lagOppdrag110FPBruker(oppdrag, FAGSYSTEMID_BRUKER);
        OppdragTestDataHelper.lagOppdrag110FPBruker(oppdrag, FAGSYSTEMID_BRUKER);

        when(økonomioppdragRepository.hentOppdragUtenKvittering(FAGSYSTEMID_BRUKER, BEHANDLINGID_FP)).thenReturn(oppdragBruker1);
        when(taskTjeneste.finn(PROSESSTASKID)).thenReturn(prosessTaskData);
        var kvittering_1 = opprettKvittering(KVITTERING_OK, null, KVITTERING_MELDING_OK, FAGSYSTEMID_BRUKER, true);
        var kvittering_2 = opprettKvittering(KVITTERING_OK, null, KVITTERING_MELDING_OK, FAGSYSTEMID_BRUKER, true);

        // Act
        behandleØkonomioppdragKvittering.behandleKvittering(kvittering_1);
        assertThatThrownBy(() -> behandleØkonomioppdragKvittering.behandleKvittering(kvittering_2)).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Mottat økonomi kvittering kan ikke overskrive en allerede eksisterende kvittering!");
    }

    @Test
    void skal_ikke_motta_hendelse_når_negativ_kvittering_FP() {
        // Arrange
        var oppdrag = oppdragskontrollUtenOppdrag(new Saksnummer("3500"), BEHANDLINGID_FP, PROSESSTASKID);
        var oppdragBruker = OppdragTestDataHelper.lagOppdrag110FPBruker(oppdrag, FAGSYSTEMID_BRUKER);
        var oppdragAg = OppdragTestDataHelper.lagOppdrag110FPArbeidsgiver(oppdrag, FAGSYSTEMID_ARBEIDSGIVER);

        when(økonomioppdragRepository.hentOppdragUtenKvittering(FAGSYSTEMID_BRUKER, BEHANDLINGID_FP)).thenReturn(oppdragBruker);
        when(økonomioppdragRepository.hentOppdragUtenKvittering(FAGSYSTEMID_ARBEIDSGIVER, BEHANDLINGID_FP)).thenReturn(oppdragAg);
        when(taskTjeneste.finn(PROSESSTASKID)).thenReturn(prosessTaskData);
        var kvittering_1 = opprettKvittering(KVITTERING_OK, null, KVITTERING_MELDING_OK, FAGSYSTEMID_BRUKER, true);
        var kvittering_2 = opprettKvittering(KVITTERING_FEIL, KVITTERING_MELDINGKODE_FEIL, KVITTERING_MELDING_FEIL, FAGSYSTEMID_ARBEIDSGIVER, true);

        // Act
        behandleØkonomioppdragKvittering.behandleKvittering(kvittering_1);
        behandleØkonomioppdragKvittering.behandleKvittering(kvittering_2);

        // Assert
        assertThat(oppdrag.getVenterKvittering()).isFalse();
        verify(økonomioppdragRepository, times(1)).lagre(oppdrag);
        verify(taskTjeneste, never()).mottaHendelse(any(), any(), any());
        verify(økonomioppdragRepository, times(2)).lagre(any(OppdragKvittering.class));
        verify(behandleHendelseØkonomioppdrag).nullstilleØkonomioppdragTask(any());
    }

    @Test
    void testEttersendingAvOppdragSomHarFåttNegativKvittering_ok() {
        var oppdrag = oppdragskontrollUtenOppdrag(new Saksnummer("3500"), BEHANDLINGID_FP, PROSESSTASKID);
        var oppdragNegativ = OppdragTestDataHelper.lagOppdrag110FPBruker(oppdrag, FAGSYSTEMID_BRUKER);
        oppdragNegativ.setOpprettetTidspunkt(LocalDateTime.now().minusDays(1));
        OppdragKvittering.builder()
            .medOppdrag110(oppdragNegativ)
            .medBeskrMelding(KVITTERING_MELDING_FEIL)
            .medAlvorlighetsgrad(KVITTERING_FEIL)
            .build();
        var oppdragPositiv = OppdragTestDataHelper.lagOppdrag110FPBruker(oppdrag, FAGSYSTEMID_BRUKER);
        oppdragPositiv.setOpprettetTidspunkt(LocalDateTime.now());
        when(økonomioppdragRepository.hentOppdragUtenKvittering(FAGSYSTEMID_BRUKER, BEHANDLINGID_FP)).thenReturn(oppdragPositiv);
        var kvittering_2 = opprettKvittering(KVITTERING_OK, null, null, FAGSYSTEMID_BRUKER, true);
        when(taskTjeneste.finn(PROSESSTASKID)).thenReturn(prosessTaskData);

        // Act
        behandleØkonomioppdragKvittering.behandleKvittering(kvittering_2);

        // Assert
        assertThat(oppdrag.getVenterKvittering()).isFalse();
        verify(økonomioppdragRepository, times(1)).lagre(oppdrag);
        verify(økonomioppdragRepository, times(1)).lagre(any(OppdragKvittering.class));
        verify(taskTjeneste).mottaHendelse(any(), any(), any());
    }

    @Test
    void testEttersendingAvOppdragSomHarFåttNegativKvittering_nok() {
        var oppdrag = oppdragskontrollUtenOppdrag(new Saksnummer("3500"), BEHANDLINGID_FP, PROSESSTASKID);
        var oppdragNegativ = OppdragTestDataHelper.lagOppdrag110FPBruker(oppdrag, FAGSYSTEMID_BRUKER);
        oppdragNegativ.setOpprettetTidspunkt(LocalDateTime.now().minusDays(1));
        OppdragKvittering.builder()
            .medOppdrag110(oppdragNegativ)
            .medBeskrMelding(KVITTERING_MELDING_FEIL)
            .medAlvorlighetsgrad(KVITTERING_FEIL)
            .build();
        var oppdragNegativ2 = OppdragTestDataHelper.lagOppdrag110FPBruker(oppdrag, FAGSYSTEMID_BRUKER);
        oppdragNegativ2.setOpprettetTidspunkt(LocalDateTime.now());

        when(økonomioppdragRepository.hentOppdragUtenKvittering(FAGSYSTEMID_BRUKER, BEHANDLINGID_FP)).thenReturn(oppdragNegativ2);
        var kvittering_2 = opprettKvittering(KVITTERING_FEIL, KVITTERING_MELDINGKODE_FEIL, KVITTERING_MELDINGKODE_FEIL, FAGSYSTEMID_BRUKER, true);
        when(taskTjeneste.finn(PROSESSTASKID)).thenReturn(prosessTaskData);

        // Act
        behandleØkonomioppdragKvittering.behandleKvittering(kvittering_2);

        // Assert
        assertThat(oppdrag.getVenterKvittering()).isFalse();
        verify(økonomioppdragRepository, times(1)).lagre(oppdrag);
        verify(økonomioppdragRepository, times(1)).lagre(any(OppdragKvittering.class));
        verify(taskTjeneste, never()).mottaHendelse(any(), any(), any());
    }

    @Test
    void skal_ikke_motta_hendelse_når_negativ_kvittering_bruker_og_arbeidsgiver_FP() {
        // Arrange
        var oppdrag = oppdragskontrollUtenOppdrag(new Saksnummer("3500"), BEHANDLINGID_FP, PROSESSTASKID);
        var oppdragBruker = OppdragTestDataHelper.lagOppdrag110FPBruker(oppdrag, FAGSYSTEMID_BRUKER);
        var oppdragAg = OppdragTestDataHelper.lagOppdrag110FPArbeidsgiver(oppdrag, FAGSYSTEMID_ARBEIDSGIVER);

        when(økonomioppdragRepository.hentOppdragUtenKvittering(FAGSYSTEMID_BRUKER, BEHANDLINGID_FP)).thenReturn(oppdragBruker);
        when(økonomioppdragRepository.hentOppdragUtenKvittering(FAGSYSTEMID_ARBEIDSGIVER, BEHANDLINGID_FP)).thenReturn(oppdragAg);
        var kvittering_1 = opprettKvittering(KVITTERING_FEIL, KVITTERING_MELDINGKODE_FEIL, KVITTERING_MELDING_FEIL, FAGSYSTEMID_BRUKER, true);
        var kvittering_2 = opprettKvittering(KVITTERING_FEIL, KVITTERING_MELDINGKODE_FEIL, KVITTERING_MELDING_FEIL, FAGSYSTEMID_ARBEIDSGIVER, true);
        when(taskTjeneste.finn(PROSESSTASKID)).thenReturn(prosessTaskData);

        // Act
        behandleØkonomioppdragKvittering.behandleKvittering(kvittering_1);
        behandleØkonomioppdragKvittering.behandleKvittering(kvittering_2);

        // Assert
        assertThat(oppdrag.getVenterKvittering()).isFalse();
        verify(økonomioppdragRepository, times(1)).lagre(oppdrag);
        verify(økonomioppdragRepository, times(2)).lagre(any(OppdragKvittering.class));
        verify(taskTjeneste, never()).mottaHendelse(any(), any(), any());
    }

    private ØkonomiKvittering opprettKvittering(Alvorlighetsgrad alvorlighetsgrad,
                                                String meldingKode,
                                                String beskrMelding,
                                                Long fagsystemId,
                                                Boolean gjelderFP) {
        var kvittering = new ØkonomiKvittering();
        kvittering.setAlvorlighetsgrad(alvorlighetsgrad);
        kvittering.setMeldingKode(meldingKode);
        kvittering.setBehandlingId(gjelderFP ? BEHANDLINGID_FP : BEHANDLINGID_ES);
        kvittering.setBeskrMelding(beskrMelding);
        kvittering.setFagsystemId(fagsystemId);
        return kvittering;
    }
}

package no.nav.foreldrepenger.økonomi.økonomistøtte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.OppdragKvittering;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomi.økonomistøtte.AlleMottakereHarPositivKvitteringProvider;
import no.nav.foreldrepenger.økonomi.økonomistøtte.BehandleNegativeKvitteringTjeneste;
import no.nav.foreldrepenger.økonomi.økonomistøtte.BehandleØkonomioppdragKvittering;
import no.nav.foreldrepenger.økonomi.økonomistøtte.ØkonomiKvittering;
import no.nav.foreldrepenger.økonomi.økonomistøtte.ØkonomioppdragRepository;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp.AlleMottakereHarPositivKvitteringImpl;
import no.nav.foreldrepenger.økonomi.økonomistøtte.kontantytelse.es.AlleMottakereHarPositivKvitteringEngangsstønad;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHendelse;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHendelseMottak;

public class BehandleØkonomioppdragKvitteringImplTest {

    private static final Long PROSESSTASKID = 33L;

    public static final Long FAGSYSTEMID_BRUKER = 124L;

    public static final Long FAGSYSTEMID_ARBEIDSGIVER = 256L;

    public static final String KODEAKSJON = "1";

    public static final String KODEENDRING = "NY";

    public static final String KODEFAGOMRADE_ES = "REFUTG";

    public static final String KODEFAGOMRADE_FP = "FP";

    public static final String KODEFAGOMRADE_FPREF = "FPREF";

    public static final String UTBETFREKVENS = "ENG";

    public static final String OPPDRAGGJELDERID = "01010101010";

    public static final String SAKSBEHID = "aa000000";

    public static final String TYPEENHET = "BOS";

    public static final String ENHET = "ENHET";

    public static final String KODEENDRINGLINJE = "NY";

    public static final String KODEKLASSIFIK_ES = "FPENFOD-OP";

    public static final String KODEKLASSIFIK_FP = "FPATORD";

    public static final Long SATS = 654L;

    public static final String FRADRAGTILLEGG = "T";

    public static final String TYPESATS_ES = "ENG";

    public static final String TYPESATS_FP = "DAG";

    public static final Integer GRAD = 100;

    public static final String TYPE_GRAD = "UFOR";

    public static final String REFUNDERES_ID = "123456789";

    public static final String VEDTAKID = "VedtakId";

    private static final String KVITTERING_OK = "00";

    private static final String KVITTERING_MELDING_OK = "Oppdrag utført";

    public static final Long BEHANDLINGID_ES = 126L;

    public static final Long BEHANDLINGID_FP = 237L;

    private static final String KVITTERING_FEIL = "12";

    private static final String KVITTERING_MELDING_FEIL = "Oppdrag ikke utført";

    private static final String KVITTERING_MELDINGKODE_FEIL = "QWERTY12";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private ProsessTaskHendelseMottak hendelsesmottak;

    private BehandleØkonomioppdragKvittering behandleØkonomioppdragKvittering;

    private Oppdragskontroll oppdrag;

    @Mock
    private ØkonomioppdragRepository økonomioppdragRepository;

    @Mock
    private AlleMottakereHarPositivKvitteringProvider alleMottakereHarPositivKvitteringProvider;

    @Mock
    private BehandleNegativeKvitteringTjeneste behandleHendelseØkonomioppdrag;


    @Before
    public void setUp() {
        behandleØkonomioppdragKvittering = new BehandleØkonomioppdragKvittering(
            alleMottakereHarPositivKvitteringProvider,
            hendelsesmottak,
            økonomioppdragRepository,
            behandleHendelseØkonomioppdrag);
    }

    @Test
    public void skal_motta_hendelse_når_positiv_kvittering_ES() {
        // Arrange
        oppdrag = OppdragTestDataHelper.buildOppdragskontroll(new Saksnummer("35"), BEHANDLINGID_ES, PROSESSTASKID);
        when(alleMottakereHarPositivKvitteringProvider.getTjeneste(anyLong())).thenReturn(new AlleMottakereHarPositivKvitteringEngangsstønad());
        ØkonomiOppdragUtils.setupOppdrag110(oppdrag, OppdragTestDataHelper.buildAvstemming115(), false);
        when(økonomioppdragRepository.finnVentendeOppdrag(BEHANDLINGID_ES)).thenReturn(oppdrag);
        ØkonomiKvittering kvittering = opprettKvittering(KVITTERING_OK, null, KVITTERING_MELDING_OK, FAGSYSTEMID_BRUKER, false);

        // Act
        behandleØkonomioppdragKvittering.behandleKvittering(kvittering);

        // Assert
        assertThat(oppdrag.getVenterKvittering()).isFalse();
        verify(økonomioppdragRepository).lagre(oppdrag);
        verify(hendelsesmottak).mottaHendelse(PROSESSTASKID, ProsessTaskHendelse.ØKONOMI_OPPDRAG_KVITTERING);
    }

    @Test
    public void skal_ikke_motta_hendelse_når_negativ_kvittering_ES() {
        // Arrange
        oppdrag = OppdragTestDataHelper.buildOppdragskontroll(new Saksnummer("35"), BEHANDLINGID_ES, PROSESSTASKID);
        when(alleMottakereHarPositivKvitteringProvider.getTjeneste(anyLong())).thenReturn(new AlleMottakereHarPositivKvitteringEngangsstønad());
        ØkonomiOppdragUtils.setupOppdrag110(oppdrag, OppdragTestDataHelper.buildAvstemming115(), false);

        when(økonomioppdragRepository.finnVentendeOppdrag(BEHANDLINGID_ES)).thenReturn(oppdrag);
        ØkonomiKvittering kvittering = opprettKvittering(KVITTERING_FEIL, KVITTERING_MELDINGKODE_FEIL, KVITTERING_MELDING_FEIL, FAGSYSTEMID_BRUKER, false);


        // Act
        behandleØkonomioppdragKvittering.behandleKvittering(kvittering);

        // Assert
        assertThat(oppdrag.getVenterKvittering()).isFalse();
        verify(økonomioppdragRepository).lagre(oppdrag);
        verify(hendelsesmottak, never()).mottaHendelse(any(), any());
    }

    @Test
    public void skal_motta_hendelse_når_positiv_kvittering_FP() {
        // Arrange
        oppdrag = OppdragTestDataHelper.buildOppdragskontroll(new Saksnummer("35"), BEHANDLINGID_FP, PROSESSTASKID);
        when(alleMottakereHarPositivKvitteringProvider.getTjeneste(anyLong())).thenReturn(new AlleMottakereHarPositivKvitteringImpl());
        OppdragTestDataHelper.buildOppdrag110FPBruker(oppdrag, FAGSYSTEMID_BRUKER);
        OppdragTestDataHelper.buildOppdrag110FPArbeidsgiver(oppdrag, FAGSYSTEMID_ARBEIDSGIVER);

        when(økonomioppdragRepository.finnVentendeOppdrag(BEHANDLINGID_FP)).thenReturn(oppdrag);
        ØkonomiKvittering kvittering_1 = opprettKvittering(KVITTERING_OK, null, KVITTERING_MELDING_OK, FAGSYSTEMID_BRUKER, true);
        ØkonomiKvittering kvittering_2 = opprettKvittering(KVITTERING_OK, null, KVITTERING_MELDING_OK, FAGSYSTEMID_ARBEIDSGIVER, true);

        // Act
        behandleØkonomioppdragKvittering.behandleKvittering(kvittering_1);
        behandleØkonomioppdragKvittering.behandleKvittering(kvittering_2);

        // Assert
        assertThat(oppdrag.getVenterKvittering()).isFalse();
        verify(økonomioppdragRepository, times(2)).lagre(oppdrag);
        verify(hendelsesmottak).mottaHendelse(PROSESSTASKID, ProsessTaskHendelse.ØKONOMI_OPPDRAG_KVITTERING);
        oppdrag.getOppdrag110Liste().forEach(o110 -> {
            assertThat(o110.getOppdragKvittering()).isNotNull();
            assertThat(o110.getOppdragKvittering().getOppdrag110()).isNotNull();
        });
    }

    @Test
    public void skal_kaste_exception_hvis_ingen_opp110_uten_kvittering_finnes_FP() {
        // Assert
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(containsString("Finnes ikke oppdrag for kvittering med fagsystemId: "));

        // Arrange
        oppdrag = OppdragTestDataHelper.buildOppdragskontroll(new Saksnummer("35"), BEHANDLINGID_FP, PROSESSTASKID);
        when(alleMottakereHarPositivKvitteringProvider.getTjeneste(anyLong())).thenReturn(new AlleMottakereHarPositivKvitteringImpl());

        Oppdrag110 oppdrag110 = OppdragTestDataHelper.buildOppdrag110FPBruker(oppdrag, FAGSYSTEMID_BRUKER);
        OppdragKvittering.builder().medAlvorlighetsgrad(KVITTERING_OK).medOppdrag110(oppdrag110).build();

        when(økonomioppdragRepository.finnVentendeOppdrag(BEHANDLINGID_FP)).thenReturn(oppdrag);
        ØkonomiKvittering kvittering_1 = opprettKvittering(KVITTERING_OK, null, KVITTERING_MELDING_OK, FAGSYSTEMID_BRUKER, true);

        // Act
        behandleØkonomioppdragKvittering.behandleKvittering(kvittering_1);

        // Assert
        fail();
    }

    @Test
    public void skal_finne_riktig_oppdrag_hvis_to_med_identisk_fagsystemid_finnes_men_kun_en_uten_kvittering_FP() {
        // Arrange
        oppdrag = OppdragTestDataHelper.buildOppdragskontroll(new Saksnummer("35"), BEHANDLINGID_FP, PROSESSTASKID);
        when(alleMottakereHarPositivKvitteringProvider.getTjeneste(anyLong())).thenReturn(new AlleMottakereHarPositivKvitteringImpl());

        Oppdrag110 oppdrag110 = OppdragTestDataHelper.buildOppdrag110FPBruker(oppdrag, FAGSYSTEMID_BRUKER);
        OppdragKvittering.builder().medAlvorlighetsgrad(KVITTERING_OK).medOppdrag110(oppdrag110).build();
        OppdragTestDataHelper.buildOppdrag110FPBruker(oppdrag, FAGSYSTEMID_BRUKER);

        when(økonomioppdragRepository.finnVentendeOppdrag(BEHANDLINGID_FP)).thenReturn(oppdrag);
        ØkonomiKvittering kvittering_1 = opprettKvittering(KVITTERING_OK, null, KVITTERING_MELDING_OK, FAGSYSTEMID_BRUKER, true);

        // Act
        behandleØkonomioppdragKvittering.behandleKvittering(kvittering_1);

        // Assert
        assertThat(oppdrag.getVenterKvittering()).isFalse();
        verify(økonomioppdragRepository, times(1)).lagre(oppdrag);
        verify(hendelsesmottak).mottaHendelse(PROSESSTASKID, ProsessTaskHendelse.ØKONOMI_OPPDRAG_KVITTERING);
        oppdrag.getOppdrag110Liste().forEach(o110 -> {
            assertThat(o110.getOppdragKvittering()).isNotNull();
            assertThat(o110.getOppdragKvittering().getOppdrag110()).isNotNull();
        });
    }

    @Test
    public void skal_kaste_exception_hvis_flere_opp110_med_samme_fagsystemId_uten_kvittering_finnes_FP() {
        // Assert
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(containsString("Finnes flere oppdrag uten kvittering med samme fagsystemId: "));

        // Arrange
        oppdrag = OppdragTestDataHelper.buildOppdragskontroll(new Saksnummer("35"), BEHANDLINGID_FP, PROSESSTASKID);
        when(alleMottakereHarPositivKvitteringProvider.getTjeneste(anyLong())).thenReturn(new AlleMottakereHarPositivKvitteringImpl());
        OppdragTestDataHelper.buildOppdrag110FPBruker(oppdrag, FAGSYSTEMID_BRUKER);
        OppdragTestDataHelper.buildOppdrag110FPBruker(oppdrag, FAGSYSTEMID_BRUKER);

        when(økonomioppdragRepository.finnVentendeOppdrag(BEHANDLINGID_FP)).thenReturn(oppdrag);
        ØkonomiKvittering kvittering_1 = opprettKvittering(KVITTERING_OK, null, KVITTERING_MELDING_OK, FAGSYSTEMID_BRUKER, true);
        ØkonomiKvittering kvittering_2 = opprettKvittering(KVITTERING_OK, null, KVITTERING_MELDING_OK, FAGSYSTEMID_BRUKER, true);

        // Act
        behandleØkonomioppdragKvittering.behandleKvittering(kvittering_1);
        behandleØkonomioppdragKvittering.behandleKvittering(kvittering_2);

        // Assert
        assertThat(oppdrag.getVenterKvittering()).isFalse();
        verify(økonomioppdragRepository, times(2)).lagre(oppdrag);
        verify(hendelsesmottak).mottaHendelse(PROSESSTASKID, ProsessTaskHendelse.ØKONOMI_OPPDRAG_KVITTERING);
        oppdrag.getOppdrag110Liste().forEach(o110 -> {
            assertThat(o110.getOppdragKvittering()).isNotNull();
            assertThat(o110.getOppdragKvittering().getOppdrag110()).isNotNull();
        });
    }

    @Test
    public void skal_ikke_motta_hendelse_når_negativ_kvittering_FP() {
        // Arrange
        when(alleMottakereHarPositivKvitteringProvider.getTjeneste(anyLong())).thenReturn(new AlleMottakereHarPositivKvitteringImpl());
        oppdrag = OppdragTestDataHelper.buildOppdragskontroll(new Saksnummer("35"), BEHANDLINGID_FP, PROSESSTASKID);
        OppdragTestDataHelper.buildOppdrag110FPBruker(oppdrag, FAGSYSTEMID_BRUKER);
        OppdragTestDataHelper.buildOppdrag110FPArbeidsgiver(oppdrag, FAGSYSTEMID_ARBEIDSGIVER);

        when(økonomioppdragRepository.finnVentendeOppdrag(BEHANDLINGID_FP)).thenReturn(oppdrag);
        ØkonomiKvittering kvittering_1 = opprettKvittering(KVITTERING_OK, null, KVITTERING_MELDING_OK, FAGSYSTEMID_BRUKER, true);
        ØkonomiKvittering kvittering_2 = opprettKvittering(KVITTERING_FEIL, KVITTERING_MELDINGKODE_FEIL, KVITTERING_MELDING_FEIL, FAGSYSTEMID_ARBEIDSGIVER, true);

        // Act
        behandleØkonomioppdragKvittering.behandleKvittering(kvittering_1);
        behandleØkonomioppdragKvittering.behandleKvittering(kvittering_2);

        // Assert
        assertThat(oppdrag.getVenterKvittering()).isFalse();
        verify(økonomioppdragRepository, times(2)).lagre(oppdrag);
        verify(hendelsesmottak, never()).mottaHendelse(any(), any());
        verify(behandleHendelseØkonomioppdrag).nullstilleØkonomioppdragTask(any());
    }

    @Test
    public void skal_ikke_motta_hendelse_når_negativ_kvittering_bruker_og_arbeidsgiver_FP() {
        // Arrange
        when(alleMottakereHarPositivKvitteringProvider.getTjeneste(anyLong())).thenReturn(new AlleMottakereHarPositivKvitteringImpl());
        oppdrag = OppdragTestDataHelper.buildOppdragskontroll(new Saksnummer("35"), BEHANDLINGID_FP, PROSESSTASKID);
        OppdragTestDataHelper.buildOppdrag110FPBruker(oppdrag, FAGSYSTEMID_BRUKER);
        OppdragTestDataHelper.buildOppdrag110FPArbeidsgiver(oppdrag, FAGSYSTEMID_ARBEIDSGIVER);

        when(økonomioppdragRepository.finnVentendeOppdrag(BEHANDLINGID_FP)).thenReturn(oppdrag);
        ØkonomiKvittering kvittering_1 = opprettKvittering(KVITTERING_FEIL, KVITTERING_MELDINGKODE_FEIL, KVITTERING_MELDING_FEIL, FAGSYSTEMID_BRUKER, true);
        ØkonomiKvittering kvittering_2 = opprettKvittering(KVITTERING_FEIL, KVITTERING_MELDINGKODE_FEIL, KVITTERING_MELDING_FEIL, FAGSYSTEMID_ARBEIDSGIVER, true);

        // Act
        behandleØkonomioppdragKvittering.behandleKvittering(kvittering_1);
        behandleØkonomioppdragKvittering.behandleKvittering(kvittering_2);

        // Assert
        assertThat(oppdrag.getVenterKvittering()).isFalse();
        verify(økonomioppdragRepository, times(2)).lagre(oppdrag);
        verify(hendelsesmottak, never()).mottaHendelse(any(), any());
    }

    private ØkonomiKvittering opprettKvittering(String alvorlighetsgrad, String meldingKode, String beskrMelding, Long fagsystemId, Boolean gjelderFP) {
        ØkonomiKvittering kvittering = new ØkonomiKvittering();
        kvittering.setAlvorlighetsgrad(alvorlighetsgrad);
        kvittering.setMeldingKode(meldingKode);
        kvittering.setBehandlingId(gjelderFP ? BEHANDLINGID_FP : BEHANDLINGID_ES);
        kvittering.setBeskrMelding(beskrMelding);
        kvittering.setFagsystemId(fagsystemId);
        return kvittering;
    }
}

package no.nav.foreldrepenger.behandling.kabal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentBestiltEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageHjemmel;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ExtendWith(MockitoExtension.class)
class KabalDokumenterTest {

    private static final Saksnummer SAKSNR = new Saksnummer("999");

    @Mock
    private MottatteDokumentRepository mottatteDokumentRepository;
    @Mock
    private BehandlingDokumentRepository behandlingDokumentRepository;
    @Mock
    private DokumentArkivTjeneste dokumentArkivTjeneste;
    @Mock
    private HistorikkinnslagRepository historikkRepository;

    private KabalDokumenter kabalTjeneste;

    @BeforeEach
    void setUp() {
        kabalTjeneste = new KabalDokumenter(dokumentArkivTjeneste, mottatteDokumentRepository,
            behandlingDokumentRepository, historikkRepository);
    }

    @Test
    void testFinnerKlageOversendtFraBehandlingDokument() {
        var behandlingId = 1234L;
        var journalpost = new JournalpostId("12345");
        var behandlingDokument = opprettBehandlingDokumentEntitet(behandlingId, journalpost, DokumentMalType.KLAGE_OVERSENDT);
        when(behandlingDokumentRepository.hentHvisEksisterer(behandlingId)).thenReturn(Optional.of(behandlingDokument));

        var dokumentReferanses = kabalTjeneste.finnDokumentReferanserForKlage(behandlingId, SAKSNR,
            KlageResultatEntitet.builder().medKlageBehandlingId(behandlingId).build(), KlageHjemmel.ENGANGS);

        assertThat(dokumentReferanses).hasSize(1);
        assertThat(dokumentReferanses.getFirst().journalpostId()).isEqualTo(journalpost.getVerdi());
        assertThat(dokumentReferanses.getFirst().type()).isEqualTo(TilKabalDto.DokumentReferanseType.OVERSENDELSESBREV);
    }

    @Test
    void hivExceptionHvisOversendelsesbrevMangler() {
        var behandlingId = 1234L;
        var journalpost = new JournalpostId("12345");
        var behandlingDokument = opprettBehandlingDokumentEntitet(behandlingId, journalpost, DokumentMalType.FORELDREPENGER_INNVILGELSE);
        when(behandlingDokumentRepository.hentHvisEksisterer(behandlingId)).thenReturn(Optional.of(behandlingDokument));

        var klageResultatEntitet = KlageResultatEntitet.builder().medKlageBehandlingId(behandlingId).build();
        assertThatThrownBy(() -> kabalTjeneste.finnDokumentReferanserForKlage(behandlingId, SAKSNR, klageResultatEntitet, KlageHjemmel.ENGANGS))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void finnerVedtakFraBehandlingDokument() {
        var behandlingId = 1234L;
        var påKlagdBehandlingId = 4321L;
        var journalpost = new JournalpostId("23456");
        var behandlingDokument = opprettBehandlingDokumentEntitet(påKlagdBehandlingId, journalpost, DokumentMalType.FORELDREPENGER_INNVILGELSE);
        var behandlingDokumentKlageOversendt = opprettBehandlingDokumentEntitet(behandlingId, journalpost, DokumentMalType.KLAGE_OVERSENDT);
        when(behandlingDokumentRepository.hentHvisEksisterer(behandlingId)).thenReturn(Optional.of(behandlingDokumentKlageOversendt));
        when(behandlingDokumentRepository.hentHvisEksisterer(påKlagdBehandlingId)).thenReturn(Optional.of(behandlingDokument));

        var dokumentReferanses = kabalTjeneste.finnDokumentReferanserForKlage(behandlingId, SAKSNR,
            KlageResultatEntitet.builder().medKlageBehandlingId(behandlingId).medPåKlagdBehandlingId(påKlagdBehandlingId).build(), KlageHjemmel.ENGANGS);

        assertThat(dokumentReferanses).hasSize(2);
        assertThat(dokumentReferanses.get(0).journalpostId()).isEqualTo(journalpost.getVerdi());
        assertThat(dokumentReferanses.get(0).type()).isEqualTo(TilKabalDto.DokumentReferanseType.OVERSENDELSESBREV);
        assertThat(dokumentReferanses.get(1).journalpostId()).isEqualTo(journalpost.getVerdi());
        assertThat(dokumentReferanses.get(1).type()).isEqualTo(TilKabalDto.DokumentReferanseType.OPPRINNELIG_VEDTAK);
    }

    @Test
    void finnerVedtakFritektsFraBehandlingDokument() {
        var behandlingId = 1234L;
        var påKlagdBehandlingId = 4321L;
        var journalpost = new JournalpostId("23456");
        var behandlingDokument = BehandlingDokumentEntitet.Builder.ny().medBehandling(behandlingId).build();
        var dokumentBestilt = new BehandlingDokumentBestiltEntitet.Builder()
            .medDokumentMalType(DokumentMalType.VEDTAKSBREV_FRITEKST_HTML.getKode())
            .medOpprinneligDokumentMal(DokumentMalType.FORELDREPENGER_INNVILGELSE.getKode())
            .medBehandlingDokument(behandlingDokument)
            .medBestillingUuid(UUID.randomUUID())
            .medJournalpostId(journalpost)
            .build();
        behandlingDokument.leggTilBestiltDokument(dokumentBestilt);
        var behandlingDokumentKlageOversendt = opprettBehandlingDokumentEntitet(behandlingId, journalpost, DokumentMalType.KLAGE_OVERSENDT);
        when(behandlingDokumentRepository.hentHvisEksisterer(behandlingId)).thenReturn(Optional.of(behandlingDokumentKlageOversendt));
        when(behandlingDokumentRepository.hentHvisEksisterer(påKlagdBehandlingId)).thenReturn(Optional.of(behandlingDokument));

        var dokumentReferanses = kabalTjeneste.finnDokumentReferanserForKlage(behandlingId, SAKSNR,
            KlageResultatEntitet.builder().medKlageBehandlingId(behandlingId).medPåKlagdBehandlingId(påKlagdBehandlingId).build(), KlageHjemmel.ENGANGS);

        assertThat(dokumentReferanses).hasSize(2);
        assertThat(dokumentReferanses.get(0).journalpostId()).isEqualTo(journalpost.getVerdi());
        assertThat(dokumentReferanses.get(0).type()).isEqualTo(TilKabalDto.DokumentReferanseType.OVERSENDELSESBREV);
        assertThat(dokumentReferanses.get(1).journalpostId()).isEqualTo(journalpost.getVerdi());
        assertThat(dokumentReferanses.get(1).type()).isEqualTo(TilKabalDto.DokumentReferanseType.OPPRINNELIG_VEDTAK);
    }


    @Test
    void finnerKlageDokumentFraMottatteDokumenter() {
        var behandlingId = 1234L;
        var journalpost = new JournalpostId("76543");
        var dokumentMottatt = opprettMottattDokument(DokumentTypeId.KLAGE_DOKUMENT, behandlingId, journalpost);
        var behandlingDokumentKlageOversendt = opprettBehandlingDokumentEntitet(behandlingId, journalpost, DokumentMalType.KLAGE_OVERSENDT);
        when(behandlingDokumentRepository.hentHvisEksisterer(behandlingId)).thenReturn(Optional.of(behandlingDokumentKlageOversendt));
        when(mottatteDokumentRepository.hentMottatteDokument(behandlingId)).thenReturn(List.of(dokumentMottatt));

        var dokumentReferanses = kabalTjeneste.finnDokumentReferanserForKlage(behandlingId, SAKSNR,
            KlageResultatEntitet.builder().medKlageBehandlingId(behandlingId).build(), KlageHjemmel.ENGANGS);

        assertThat(dokumentReferanses).hasSize(2);
        assertThat(dokumentReferanses.get(0).journalpostId()).isEqualTo(journalpost.getVerdi());
        assertThat(dokumentReferanses.get(0).type()).isEqualTo(TilKabalDto.DokumentReferanseType.OVERSENDELSESBREV);
        assertThat(dokumentReferanses.get(1).journalpostId()).isEqualTo(journalpost.getVerdi());
        assertThat(dokumentReferanses.get(1).type()).isEqualTo(TilKabalDto.DokumentReferanseType.BRUKERS_KLAGE);
    }

    @Test
    void finnerSøknadDokumentFraMottatteDokumenter() {
        var behandlingId = 1234L;
        var påKlagdBehandling = 5432L;
        var journalpost = new JournalpostId("87654");
        var dokumentMottatt = opprettMottattDokument(DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL, påKlagdBehandling, journalpost);
        var behandlingDokumentKlageOversendt = opprettBehandlingDokumentEntitet(behandlingId, journalpost, DokumentMalType.KLAGE_OVERSENDT);
        when(behandlingDokumentRepository.hentHvisEksisterer(behandlingId)).thenReturn(Optional.of(behandlingDokumentKlageOversendt));
        when(mottatteDokumentRepository.hentMottatteDokument(påKlagdBehandling)).thenReturn(List.of(dokumentMottatt));
        when(mottatteDokumentRepository.hentMottatteDokument(behandlingId)).thenReturn(List.of());

        var dokumentReferanses = kabalTjeneste.finnDokumentReferanserForKlage(behandlingId, SAKSNR,
            KlageResultatEntitet.builder().medKlageBehandlingId(behandlingId).medPåKlagdBehandlingId(påKlagdBehandling).build(), KlageHjemmel.ENGANGS);

        assertThat(dokumentReferanses).hasSize(2);
        assertThat(dokumentReferanses.get(0).journalpostId()).isEqualTo(journalpost.getVerdi());
        assertThat(dokumentReferanses.get(0).type()).isEqualTo(TilKabalDto.DokumentReferanseType.OVERSENDELSESBREV);
        assertThat(dokumentReferanses.get(1).journalpostId()).isEqualTo(journalpost.getVerdi());
        assertThat(dokumentReferanses.get(1).type()).isEqualTo(TilKabalDto.DokumentReferanseType.BRUKERS_SOEKNAD);
    }

    private MottattDokument opprettMottattDokument(DokumentTypeId søknadForeldrepengerFødsel, long påKlagdBehandling, JournalpostId journalpost) {
        return new MottattDokument.Builder().medDokumentType(søknadForeldrepengerFødsel)
            .medBehandlingId(påKlagdBehandling)
            .medFagsakId(9876L)
            .medJournalPostId(journalpost)
            .build();
    }

    private static BehandlingDokumentEntitet opprettBehandlingDokumentEntitet(long behandlingId, JournalpostId journalpost, DokumentMalType dokumentMalType) {
        var behandlingDokument = BehandlingDokumentEntitet.Builder.ny().medBehandling(behandlingId).build();
        var bestiltDokument = new BehandlingDokumentBestiltEntitet.Builder()
            .medDokumentMalType(dokumentMalType.getKode())
            .medBehandlingDokument(behandlingDokument)
            .medBestillingUuid(UUID.randomUUID())
            .medJournalpostId(journalpost)
            .build();
        behandlingDokument.leggTilBestiltDokument(bestiltDokument);
        return behandlingDokument;
    }
}

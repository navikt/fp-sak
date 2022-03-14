package no.nav.foreldrepenger.behandling.kabal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.anke.AnkeVurderingTjeneste;
import no.nav.foreldrepenger.behandling.klage.KlageVurderingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentBestiltEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.JournalpostId;

@ExtendWith(MockitoExtension.class)
class KabalTjenesteTest {

    @Mock
    private AnkeVurderingTjeneste ankeVurderingTjeneste;
    @Mock
    private KlageVurderingTjeneste klageVurderingTjeneste;
    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private MottatteDokumentRepository mottatteDokumentRepository;
    @Mock
    private BehandlingDokumentRepository behandlingDokumentRepository;
    @Mock
    private VergeRepository vergeRepository;
    @Mock
    private PersoninfoAdapter personinfoAdapter;
    @Mock
    private HistorikkRepository historikkRepository;
    @Mock
    private KabalKlient kabalKlient;

    private KabalTjeneste kabalTjeneste;

    @BeforeEach
    void setUp() {
        kabalTjeneste = new KabalTjeneste(personinfoAdapter, kabalKlient, behandlingRepository, mottatteDokumentRepository,
            behandlingDokumentRepository, vergeRepository, ankeVurderingTjeneste, klageVurderingTjeneste, historikkRepository);
    }

    @Test
    void testFinnerKlageOversendtFraBehandlingDokument() {
        var behandlingId = 1234L;
        var journalpost = new JournalpostId("12345");

        BehandlingDokumentEntitet behandlingDokument = opprettBehandlingDokumentEntitet(behandlingId, journalpost, DokumentMalType.KLAGE_OVERSENDT);

        when(behandlingDokumentRepository.hentHvisEksisterer(behandlingId)).thenReturn(Optional.of(behandlingDokument));

        var dokumentReferanses = kabalTjeneste.finnDokumentReferanser(behandlingId,
            KlageResultatEntitet.builder().medKlageBehandlingId(behandlingId).build());

        sjekkResultat(dokumentReferanses, journalpost, TilKabalDto.DokumentReferanseType.OVERSENDELSESBREV);
    }

    @Test
    void testFinnerKlageOversendtFraHistorikkInnslag() {
        var behandlingId = 1234L;
        var journalpost = new JournalpostId("54321");

        Historikkinnslag historikkInnslag = opprettHistorikkinnslag(behandlingId, journalpost, DokumentMalType.KLAGE_OVERSENDT);

        when(historikkRepository.hentHistorikk(behandlingId)).thenReturn(List.of(historikkInnslag));
        when(behandlingDokumentRepository.hentHvisEksisterer(behandlingId)).thenReturn(Optional.empty());

        var dokumentReferanses = kabalTjeneste.finnDokumentReferanser(behandlingId,
            KlageResultatEntitet.builder().medKlageBehandlingId(behandlingId).build());

        sjekkResultat(dokumentReferanses, journalpost, TilKabalDto.DokumentReferanseType.OVERSENDELSESBREV);
    }

    @Test
    void finnerVedtakFraBehandlingDokument() {
        var behandlingId = 1234L;
        var påKlagdBehandlingId = 4321L;
        var journalpost = new JournalpostId("23456");

        BehandlingDokumentEntitet behandlingDokument = opprettBehandlingDokumentEntitet(påKlagdBehandlingId, journalpost,
            DokumentMalType.FORELDREPENGER_INNVILGELSE);

        when(behandlingDokumentRepository.hentHvisEksisterer(behandlingId)).thenReturn(Optional.empty());
        when(behandlingDokumentRepository.hentHvisEksisterer(påKlagdBehandlingId)).thenReturn(Optional.of(behandlingDokument));

        var dokumentReferanses = kabalTjeneste.finnDokumentReferanser(behandlingId,
            KlageResultatEntitet.builder().medKlageBehandlingId(behandlingId).medPåKlagdBehandlingId(påKlagdBehandlingId).build());

        sjekkResultat(dokumentReferanses, journalpost, TilKabalDto.DokumentReferanseType.OPPRINNELIG_VEDTAK);
    }

    @Test
    void finnerVedtakFraHistorikkInnslag() {
        var behandlingId = 1234L;
        var påKlagdBehandlingId = 4321L;
        var journalpost = new JournalpostId("65432");

        Historikkinnslag historikkInnslag = opprettHistorikkinnslag(påKlagdBehandlingId, journalpost, DokumentMalType.FORELDREPENGER_INNVILGELSE);

        when(historikkRepository.hentHistorikk(behandlingId)).thenReturn(List.of());
        when(historikkRepository.hentHistorikk(påKlagdBehandlingId)).thenReturn(List.of(historikkInnslag));

        var dokumentReferanses = kabalTjeneste.finnDokumentReferanser(behandlingId,
            KlageResultatEntitet.builder().medKlageBehandlingId(behandlingId).medPåKlagdBehandlingId(påKlagdBehandlingId).build());

        sjekkResultat(dokumentReferanses, journalpost, TilKabalDto.DokumentReferanseType.OPPRINNELIG_VEDTAK);
    }

    @Test
    void finnerKlageDokumentFraMottatteDokumenter() {
        var behandlingId = 1234L;
        var journalpost = new JournalpostId("76543");

        MottattDokument dokumentMottatt = opprettMottattDokument(DokumentTypeId.KLAGE_DOKUMENT, behandlingId, journalpost);

        when(mottatteDokumentRepository.hentMottatteDokument(behandlingId)).thenReturn(List.of(dokumentMottatt));

        var dokumentReferanses = kabalTjeneste.finnDokumentReferanser(behandlingId,
            KlageResultatEntitet.builder().medKlageBehandlingId(behandlingId).build());

        sjekkResultat(dokumentReferanses, journalpost, TilKabalDto.DokumentReferanseType.BRUKERS_KLAGE);
    }

    @Test
    void finnerSøknadDokumentFraMottatteDokumenter() {
        var behandlingId = 1234L;
        var påKlagdBehandling = 1234L;
        var journalpost = new JournalpostId("87654");

        MottattDokument dokumentMottatt = opprettMottattDokument(DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL, påKlagdBehandling, journalpost);

        when(mottatteDokumentRepository.hentMottatteDokument(påKlagdBehandling)).thenReturn(List.of(dokumentMottatt));

        var dokumentReferanses = kabalTjeneste.finnDokumentReferanser(behandlingId,
            KlageResultatEntitet.builder().medKlageBehandlingId(behandlingId).medPåKlagdBehandlingId(påKlagdBehandling).build());

        sjekkResultat(dokumentReferanses, journalpost, TilKabalDto.DokumentReferanseType.BRUKERS_SOEKNAD);
    }

    private void sjekkResultat(List<TilKabalDto.DokumentReferanse> dokumentReferanses,
                               JournalpostId journalpost,
                               TilKabalDto.DokumentReferanseType oversendelsesbrev) {
        assertThat(dokumentReferanses).isNotNull();
        assertThat(dokumentReferanses).hasSize(1);
        assertThat(dokumentReferanses.get(0).journalpostId()).isEqualTo(journalpost.getVerdi());
        assertThat(dokumentReferanses.get(0).type()).isEqualTo(oversendelsesbrev);
    }

    private MottattDokument opprettMottattDokument(DokumentTypeId søknadForeldrepengerFødsel, long påKlagdBehandling, JournalpostId journalpost) {
        return new MottattDokument.Builder().medDokumentType(søknadForeldrepengerFødsel)
            .medBehandlingId(påKlagdBehandling)
            .medFagsakId(9876L)
            .medJournalPostId(journalpost)
            .build();
    }

    private BehandlingDokumentEntitet opprettBehandlingDokumentEntitet(long behandlingId,
                                                                       JournalpostId journalpost,
                                                                       DokumentMalType dokumentMalType) {
        var behandlingDokument = BehandlingDokumentEntitet.Builder.ny().medBehandling(behandlingId).build();

        var dokumentBestilt = new BehandlingDokumentBestiltEntitet.Builder().medDokumentMalType(dokumentMalType.getKode())
            .medBehandlingDokument(behandlingDokument)
            .medBestillingUuid(UUID.randomUUID())
            .medJournalpostId(journalpost)
            .build();

        behandlingDokument.leggTilBestiltDokument(dokumentBestilt);
        return behandlingDokument;
    }

    private Historikkinnslag opprettHistorikkinnslag(long behandlingId, JournalpostId journalpost, DokumentMalType dokumentMalType) {
        var dokumentLink = new HistorikkinnslagDokumentLink();
        dokumentLink.setJournalpostId(journalpost);
        dokumentLink.setLinkTekst(dokumentMalType.getNavn());

        var historikkInnslag = new Historikkinnslag();
        historikkInnslag.setType(HistorikkinnslagType.BREV_SENT);
        historikkInnslag.setBehandlingId(behandlingId);
        historikkInnslag.setDokumentLinker(List.of(dokumentLink));
        return historikkInnslag;
    }
}

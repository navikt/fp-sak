package no.nav.foreldrepenger.dokumentbestiller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.DokumentMalType;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.MellomlagringRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.MellomlagringType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.typer.JournalpostId;

@CdiDbAwareTest
class DokumentBehandlingTjenesteTest {
    private static final String VEDTAK_FRITEKST = "Begrunnelse";

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Mock
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;

    private AbstractTestScenario<?> scenario;
    private BehandlingDokumentRepository behandlingDokumentRepository;
    private MellomlagringRepository mellomlagringRepository;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    private Behandling behandling;
    private BehandlingRepository behandlingRepository;
    private int fristUker = 6;

    @BeforeEach
    void setUp() {
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        behandlingDokumentRepository = new BehandlingDokumentRepository(repositoryProvider.getEntityManager());
        mellomlagringRepository = new MellomlagringRepository(repositoryProvider.getEntityManager());
        dokumentBehandlingTjeneste = new DokumentBehandlingTjeneste(repositoryProvider, behandlingProsesseringTjeneste, behandlingDokumentRepository,
            mellomlagringRepository);
        this.scenario = ScenarioMorSøkerEngangsstønad
                .forFødsel()
                .medFødselAdopsjonsdato(Collections.singletonList(LocalDate.now().minusDays(3)));
    }

    @Test
    void skal_finne_behandlingsfrist_fra_manuel() {
        lagBehandling();
        assertThat(dokumentBehandlingTjeneste.finnNyFristManuelt(behandling.getType()))
                .isEqualTo(LocalDate.now().plusWeeks(fristUker));
    }

    @Test
    void skal_finne_behandlingsfrist_fra_manuelt_medlemskap_ingen_terminbekreftelse() {
        lagBehandling();
        assertThat(dokumentBehandlingTjeneste.utledFristMedlemskap(behandling))
                .isEqualTo(LocalDate.now().plusWeeks(fristUker));
    }

    @Test
    void skal_finne_behandlingsfrist_fra_manuelt_medlemskap_med_terminbekreftelse() {
        var fødselsdato = LocalDate.now().minusDays(3);
        this.scenario = ScenarioMorSøkerEngangsstønad
                .forFødsel()
                .medFødselAdopsjonsdato(Collections.singletonList(fødselsdato))
                .medDefaultSøknadTerminbekreftelse();
        lagBehandling();
        assertThat(dokumentBehandlingTjeneste.utledFristMedlemskap(behandling))
                .isEqualTo(LocalDate.now().plusWeeks(fristUker));
    }

    @Test
    void skal_finne_behandlingsfrist_fra_manuelt_medlemskap_med_terminbekreftelse_etter_ap() {
        var fødselsdato = LocalDate.now().plusDays(3);
        this.scenario = ScenarioMorSøkerEngangsstønad
                .forFødsel()
                .medFødselAdopsjonsdato(Collections.singletonList(fødselsdato))
                .medDefaultSøknadTerminbekreftelse();
        lagBehandling();
        assertThat(dokumentBehandlingTjeneste.utledFristMedlemskap(behandling))
                .isEqualTo(fødselsdato.plusWeeks(fristUker));
    }

    @Test
    void skal_finne_behandlingsfrist_fra_manuelt_medlemskap_med_terminbekreftelse_i_fortiden() {
        this.scenario = ScenarioMorSøkerEngangsstønad
                .forFødsel()
                .medFødselAdopsjonsdato(Collections.singletonList(LocalDate.now().minusDays(3)));
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(LocalDate.now().minusWeeks(6)));
        lagBehandling();
        assertThat(dokumentBehandlingTjeneste.utledFristMedlemskap(behandling))
                .isEqualTo(LocalDate.now().plusWeeks(fristUker));
    }

    private void lagBehandling() {
        behandling = Mockito.spy(scenario.medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD).lagre(repositoryProvider));
    }

    @Test
    void skal_lagre_ny_frist() {
        behandling = scenario.lagre(repositoryProvider);
        dokumentBehandlingTjeneste.oppdaterBehandlingMedNyFrist(behandling, LocalDate.now());
        assertThat(behandlingRepository.hentBehandling(behandling.getId()).getBehandlingstidFrist()).isEqualTo(LocalDate.now());
    }

    @Test
    void skal_logge_i_repo_at_dokument_er_bestilt() {
        // Arrange
        behandling = scenario.lagre(repositoryProvider);

        var bestilling = lagBestilling(DokumentMalType.INNHENTE_OPPLYSNINGER, null);

        // Act
        dokumentBehandlingTjeneste.lagreDokumentBestilt(behandling, bestilling);

        // Assert
        var behandlingDokument = behandlingDokumentRepository.hentHvisEksisterer(behandling.getId());
        assertThat(behandlingDokument).isPresent();
        assertThat(behandlingDokument.get().getBestilteDokumenter()).hasSize(1);
        assertThat(behandlingDokument.get().getBestilteDokumenter().getFirst().getDokumentMalType()).isEqualTo(DokumentMalType.INNHENTE_OPPLYSNINGER);
        assertThat(behandlingDokument.get().getBestilteDokumenter().getFirst().getBestillingUuid()).isNotNull();
    }

    @Test
    void skal_returnere_true_når_dokument_er_bestilt() {
        // Arrange
        behandling = scenario.lagre(repositoryProvider);
        var bestilling = lagBestilling(DokumentMalType.INNHENTE_OPPLYSNINGER, null);

        dokumentBehandlingTjeneste.lagreDokumentBestilt(behandling, bestilling);

        // Act+Assert
        assertThat(dokumentBehandlingTjeneste.erDokumentBestilt(behandling.getId(), DokumentMalType.INNHENTE_OPPLYSNINGER)).isTrue();
    }

    @Test
    void skal_returnere_false_når_dokument_ikke_er_bestilt() {
        // Arrange
        behandling = scenario.lagre(repositoryProvider);

        var bestilling = lagBestilling(DokumentMalType.ETTERLYS_INNTEKTSMELDING, DokumentMalType.ETTERLYS_INNTEKTSMELDING);

        dokumentBehandlingTjeneste.lagreDokumentBestilt(behandling, bestilling);

        // Act+Assert
        assertThat(dokumentBehandlingTjeneste.erDokumentBestilt(behandling.getId(), DokumentMalType.INNHENTE_OPPLYSNINGER)).isFalse();
    }

    @Test
    void skal_nullstille_vedtak_fritekst() {
        // Arrange
        behandling = scenario.lagre(repositoryProvider);
        var behandlingDokumentBuilder = BehandlingDokumentEntitet.Builder.ny()
            .medBehandling(behandling.getId())
            .medUtfyllendeTekstAutomatiskVedtaksbrev(VEDTAK_FRITEKST);
        behandlingDokumentRepository.lagreOgFlush(behandlingDokumentBuilder.build());

        var behandlingDokumentFør = behandlingDokumentRepository.hentHvisEksisterer(behandling.getId());
        assertThat(behandlingDokumentFør).isPresent();
        assertThat(behandlingDokumentFør.get().getVedtakFritekst()).isEqualTo(VEDTAK_FRITEKST);

        // Act
        dokumentBehandlingTjeneste.nullstillVedtakFritekstHvisFinnes(behandling.getId());

        // Assert
        var behandlingDokumentEtter = behandlingDokumentRepository.hentHvisEksisterer(behandling.getId());
        assertThat(behandlingDokumentEtter).isPresent();
        assertThat(behandlingDokumentEtter.get().getVedtakFritekst()).isNull();
    }

    @Test
    void skal_finne_overtyrt_brev_ved_mellomlagring() {
        // Arrange
        behandling = scenario.lagre(repositoryProvider);
        var overstyrtBrev = "<div><h1>TITTELEN ER GOD</h1><p>body</p></div>";
        mellomlagringRepository.lagreEllerOppdater(behandling.getId(), MellomlagringType.VEDTAKSBREV, overstyrtBrev);

        // Assert
        var mellomlagretOverstyring = dokumentBehandlingTjeneste.hentMellomlagretOverstyring(behandling.getId());
        assertThat(mellomlagretOverstyring).isPresent();
        assertThat(mellomlagretOverstyring.get()).contains(overstyrtBrev);
    }

    @Test
    void skal_fallbacke_til_gammel_tabell_hvis_ikke_i_mellomlagring() {
        // Arrange
        var overstyrtBrev = "<div><h1>TITTELEN ER GOD</h1><p>body</p></div>";
        behandling = scenario.lagre(repositoryProvider);
        var behandlingDokumentBuilder = BehandlingDokumentEntitet.Builder.ny()
            .medBehandling(behandling.getId())
            .medOverstyrtBrevFritekstHtml(overstyrtBrev)
            .medUtfyllendeTekstAutomatiskVedtaksbrev(VEDTAK_FRITEKST);
        behandlingDokumentRepository.lagreOgFlush(behandlingDokumentBuilder.build());

        // Assert - finner fra gammel tabell
        var mellomlagretOverstyring = dokumentBehandlingTjeneste.hentMellomlagretOverstyring(behandling.getId());
        assertThat(mellomlagretOverstyring).isPresent();
        assertThat(mellomlagretOverstyring.get()).contains(overstyrtBrev);
    }

    @Test
    void harMellomlagretOverstyring_returnerer_true_for_ny_tabell() {
        behandling = scenario.lagre(repositoryProvider);
        mellomlagringRepository.lagreEllerOppdater(behandling.getId(), MellomlagringType.VEDTAKSBREV, "<p>vedtak</p>");

        assertThat(dokumentBehandlingTjeneste.harMellomlagretOverstyring(behandling.getId())).isTrue();
    }

    @Test
    void harMellomlagretOverstyring_returnerer_false_uten_data() {
        behandling = scenario.lagre(repositoryProvider);

        assertThat(dokumentBehandlingTjeneste.harMellomlagretOverstyring(behandling.getId())).isFalse();
    }

    @Test
    void harRedigertVedtaksbrev_returnerer_true_for_mellomlagret_vedtaksbrev() {
        behandling = scenario.lagre(repositoryProvider);
        mellomlagringRepository.lagreEllerOppdater(behandling.getId(), MellomlagringType.VEDTAKSBREV, "<p>redigert vedtaksbrev</p>");

        assertThat(dokumentBehandlingTjeneste.harRedigertVedtaksbrev(behandling.getId())).isTrue();
    }

    @Test
    void harRedigertVedtaksbrev_returnerer_true_for_bestilt_redigert_vedtaksbrev() {
        behandling = scenario.lagre(repositoryProvider);
        dokumentBehandlingTjeneste.lagreDokumentBestilt(behandling,
            lagBestilling(DokumentMalType.FRITEKST_HTML, DokumentMalType.ENGANGSSTØNAD_INNVILGELSE));

        assertThat(dokumentBehandlingTjeneste.harRedigertVedtaksbrev(behandling.getId())).isTrue();
    }

    @Test
    void harRedigertVedtaksbrev_returnerer_false_for_bestilt_fritekst_som_ikke_er_vedtaksbrev() {
        behandling = scenario.lagre(repositoryProvider);
        dokumentBehandlingTjeneste.lagreDokumentBestilt(behandling,
            lagBestilling(DokumentMalType.FRITEKST_HTML, DokumentMalType.INNHENTE_OPPLYSNINGER));

        assertThat(dokumentBehandlingTjeneste.harRedigertVedtaksbrev(behandling.getId())).isFalse();
    }

    @Test
    void finnJournalpostIdForRedigertVedtaksbrev_returnerer_journalpost_for_bestilt_redigert_vedtaksbrev() {
        behandling = scenario.lagre(repositoryProvider);
        var journalpostId = new JournalpostId("123456789");
        lagreBestiltRedigertVedtaksbrev(DokumentMalType.FORELDREPENGER_INNVILGELSE, journalpostId);

        assertThat(dokumentBehandlingTjeneste.finnJournalpostIdForRedigertVedtaksbrev(behandling.getId())).contains(journalpostId);
    }

    @Test
    void finnJournalpostIdForRedigertVedtaksbrev_returnerer_tomt_for_bestilt_redigert_vedtaksbrev_uten_journalpost() {
        behandling = scenario.lagre(repositoryProvider);
        dokumentBehandlingTjeneste.lagreDokumentBestilt(behandling,
            lagBestilling(DokumentMalType.FRITEKST_HTML, DokumentMalType.FORELDREPENGER_INNVILGELSE));

        assertThat(dokumentBehandlingTjeneste.finnJournalpostIdForRedigertVedtaksbrev(behandling.getId())).isEmpty();
    }

    @Test
    void finnJournalpostIdForRedigertVedtaksbrev_filtrerer_bort_fritekst_som_ikke_er_vedtaksbrev() {
        behandling = scenario.lagre(repositoryProvider);
        lagreBestiltRedigertVedtaksbrev(DokumentMalType.INNHENTE_OPPLYSNINGER, new JournalpostId("123456789"));

        assertThat(dokumentBehandlingTjeneste.finnJournalpostIdForRedigertVedtaksbrev(behandling.getId())).isEmpty();
    }

    @Test
    void skal_håndtere_at_det_ikke_finnes_behandling_dokument_ved_nullstilling_av_vedtak_fritekst() {
        // Arrange
        behandling = scenario.lagre(repositoryProvider);

        // Act
        dokumentBehandlingTjeneste.nullstillVedtakFritekstHvisFinnes(behandling.getId());

        // Assert
        var behandlingDokument = behandlingDokumentRepository.hentHvisEksisterer(behandling.getId());
        assertThat(behandlingDokument).isNotPresent();
    }

    @Test
    void kvittering_skal_slette_vedtaksbrev_mellomlagring() {
        // Arrange
        behandling = scenario.lagre(repositoryProvider);
        mellomlagringRepository.lagreEllerOppdater(behandling.getId(), MellomlagringType.VEDTAKSBREV, "<p>redigert vedtaksbrev</p>");

        var bestilling = lagBestilling(DokumentMalType.FRITEKST_HTML, DokumentMalType.FORELDREPENGER_INNVILGELSE);
        dokumentBehandlingTjeneste.lagreDokumentBestilt(behandling, bestilling);
        var bestiltDokument = behandlingDokumentRepository.hentHvisEksisterer(behandling.getId()).orElseThrow().getBestilteDokumenter().getFirst();

        // Act
        dokumentBehandlingTjeneste.kvitterSendtBrev(new DokumentKvittering(
            behandling.getUuid(), bestiltDokument.getBestillingUuid(), "123456789", "dok1"));

        // Assert - mellomlagring skal være slettet
        assertThat(mellomlagringRepository.hentMellomlagring(behandling.getId(), MellomlagringType.VEDTAKSBREV)).isEmpty();
    }

    @Test
    void kvittering_for_annet_brev_skal_ikke_slette_vedtaksbrev_mellomlagring() {
        // Arrange
        behandling = scenario.lagre(repositoryProvider);
        mellomlagringRepository.lagreEllerOppdater(behandling.getId(), MellomlagringType.VEDTAKSBREV, "<p>redigert vedtaksbrev</p>");
        mellomlagringRepository.lagreEllerOppdater(behandling.getId(), MellomlagringType.INNHENT_OPPLYSNINGER, "<p>redigert innhent</p>");

        var bestilling = lagBestilling(DokumentMalType.FRITEKST_HTML, DokumentMalType.INNHENTE_OPPLYSNINGER);
        dokumentBehandlingTjeneste.lagreDokumentBestilt(behandling, bestilling);
        var bestiltDokument = behandlingDokumentRepository.hentHvisEksisterer(behandling.getId()).orElseThrow().getBestilteDokumenter().getFirst();

        // Act
        dokumentBehandlingTjeneste.kvitterSendtBrev(new DokumentKvittering(
            behandling.getUuid(), bestiltDokument.getBestillingUuid(), "123456789", "dok1"));

        // Assert - vedtaksbrev mellomlagring skal IKKE slettes, men innhent opplysninger skal
        assertThat(mellomlagringRepository.hentMellomlagring(behandling.getId(), MellomlagringType.VEDTAKSBREV)).isPresent();
        assertThat(mellomlagringRepository.hentMellomlagring(behandling.getId(), MellomlagringType.INNHENT_OPPLYSNINGER)).isEmpty();
    }

    private DokumentBestilling lagBestilling(DokumentMalType dokumentMal, DokumentMalType journalførSomMal) {
        return DokumentBestilling.builder()
            .medBehandlingUuid(behandling.getUuid())
            .medSaksnummer(behandling.getSaksnummer())
            .medDokumentMal(dokumentMal)
            .medJournalførSom(journalførSomMal)
            .medFritekst("test")
            .build();
    }

    private void lagreBestiltRedigertVedtaksbrev(DokumentMalType opprinneligDokumentMal, JournalpostId journalpostId) {
        dokumentBehandlingTjeneste.lagreDokumentBestilt(behandling, lagBestilling(DokumentMalType.FRITEKST_HTML, opprinneligDokumentMal));
        var bestiltDokument = behandlingDokumentRepository.hentHvisEksisterer(behandling.getId()).orElseThrow().getBestilteDokumenter().getFirst();
        bestiltDokument.setJournalpostId(journalpostId);
        behandlingDokumentRepository.lagreOgFlush(bestiltDokument);
    }
}

package no.nav.foreldrepenger.dokumentbestiller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;

@CdiDbAwareTest
class DokumentBehandlingTjenesteTest {
    private static final String VEDTAK_FRITEKST = "Begrunnelse";

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Mock
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    private AbstractTestScenario<?> scenario;
    private BehandlingDokumentRepository behandlingDokumentRepository;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    private Behandling behandling;
    private BehandlingRepository behandlingRepository;
    private int fristUker = 6;

    @BeforeEach
    public void setUp(EntityManager em) {
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        behandlingDokumentRepository = new BehandlingDokumentRepository(em);
        dokumentBehandlingTjeneste = new DokumentBehandlingTjeneste(repositoryProvider, behandlingskontrollTjeneste, behandlingDokumentRepository);
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
        dokumentBehandlingTjeneste.loggDokumentBestilt(behandling, bestilling);

        // Assert
        var behandlingDokument = behandlingDokumentRepository.hentHvisEksisterer(behandling.getId());
        assertThat(behandlingDokument).isPresent();
        assertThat(behandlingDokument.get().getBestilteDokumenter()).hasSize(1);
        assertThat(behandlingDokument.get().getBestilteDokumenter().getFirst().getDokumentMalType()).isEqualTo(DokumentMalType.INNHENTE_OPPLYSNINGER.getKode());
        assertThat(behandlingDokument.get().getBestilteDokumenter().getFirst().getBestillingUuid()).isNotNull();
    }

    @Test
    void skal_returnere_true_når_dokument_er_bestilt() {
        // Arrange
        behandling = scenario.lagre(repositoryProvider);
        var bestilling = lagBestilling(DokumentMalType.INNHENTE_OPPLYSNINGER, null);

        dokumentBehandlingTjeneste.loggDokumentBestilt(behandling, bestilling);

        // Act+Assert
        assertThat(dokumentBehandlingTjeneste.erDokumentBestilt(behandling.getId(), DokumentMalType.INNHENTE_OPPLYSNINGER)).isTrue();
    }

    @Test
    void skal_returnere_false_når_dokument_ikke_er_bestilt() {
        // Arrange
        behandling = scenario.lagre(repositoryProvider);

        var bestilling = lagBestilling(DokumentMalType.ETTERLYS_INNTEKTSMELDING, DokumentMalType.ETTERLYS_INNTEKTSMELDING);

        dokumentBehandlingTjeneste.loggDokumentBestilt(behandling, bestilling);

        // Act+Assert
        assertThat(dokumentBehandlingTjeneste.erDokumentBestilt(behandling.getId(), DokumentMalType.INNHENTE_OPPLYSNINGER)).isFalse();
    }

    @Test
    void skal_nullstille_vedtak_fritekst() {
        // Arrange
        behandling = scenario.lagre(repositoryProvider);
        var behandlingDokumentBuilder = BehandlingDokumentEntitet.Builder.ny()
            .medBehandling(behandling.getId())
            .medVedtakFritekst(VEDTAK_FRITEKST);
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
    void skal_håndtere_at_det_ikke_finnes_behandling_dokument_ved_nullstilling_av_vedtak_fritekst() {
        // Arrange
        behandling = scenario.lagre(repositoryProvider);

        // Act
        dokumentBehandlingTjeneste.nullstillVedtakFritekstHvisFinnes(behandling.getId());

        // Assert
        var behandlingDokument = behandlingDokumentRepository.hentHvisEksisterer(behandling.getId());
        assertThat(behandlingDokument).isNotPresent();
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
}

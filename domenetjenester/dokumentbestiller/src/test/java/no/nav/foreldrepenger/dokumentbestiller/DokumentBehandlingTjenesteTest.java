package no.nav.foreldrepenger.dokumentbestiller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collections;
import java.util.Optional;

import javax.inject.Inject;
import javax.persistence.EntityManager;

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
public class DokumentBehandlingTjenesteTest {
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
    private LocalDate now = LocalDate.now();

    @BeforeEach
    public void setUp(EntityManager em) {
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        behandlingDokumentRepository = new BehandlingDokumentRepository(em);
        dokumentBehandlingTjeneste = new DokumentBehandlingTjeneste(repositoryProvider, null, behandlingskontrollTjeneste, null,
                behandlingDokumentRepository);
        this.scenario = ScenarioMorSøkerEngangsstønad
                .forFødsel()
                .medFødselAdopsjonsdato(Collections.singletonList(LocalDate.now().minusDays(3)));
    }

    @Test
    public void skal_finne_behandlingsfrist_fra_manuel() {
        lagBehandling();
        assertThat(dokumentBehandlingTjeneste.finnNyFristManuelt(behandling))
                .isEqualTo(LocalDate.now().plusWeeks(fristUker));
    }

    @Test
    public void skal_finne_behandlingsfrist_fra_manuelt_medlemskap_ingen_terminbekreftelse() {
        lagBehandling();
        assertThat(dokumentBehandlingTjeneste.utledFristMedlemskap(behandling, Period.ofWeeks(3)))
                .isEqualTo(LocalDate.now().plusWeeks(fristUker));
    }

    @Test
    public void skal_finne_behandlingsfrist_fra_manuelt_medlemskap_med_terminbekreftelse() {
        this.scenario = ScenarioMorSøkerEngangsstønad
                .forFødsel()
                .medFødselAdopsjonsdato(Collections.singletonList(LocalDate.now().minusDays(3)))
                .medDefaultBekreftetTerminbekreftelse();
        lagBehandling();
        var aksjonspunktPeriode = Period.ofWeeks(3);
        assertThat(dokumentBehandlingTjeneste.utledFristMedlemskap(behandling, aksjonspunktPeriode))
                .isEqualTo(LocalDate.now().plusWeeks(fristUker));
    }

    @Test
    public void skal_finne_behandlingsfrist_fra_manuelt_medlemskap_med_terminbekreftelse_etter_ap() {
        this.scenario = ScenarioMorSøkerEngangsstønad
                .forFødsel()
                .medFødselAdopsjonsdato(Collections.singletonList(LocalDate.now().minusDays(3)))
                .medDefaultBekreftetTerminbekreftelse();
        this.fristUker = BehandlingType.FØRSTEGANGSSØKNAD.getBehandlingstidFristUker();
        lagBehandling();
        var aksjonspunktPeriode = Period.ofWeeks(0);
        var termindato = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId()).getGjeldendeTerminbekreftelse()
                .get().getTermindato();
        assertThat(dokumentBehandlingTjeneste.utledFristMedlemskap(behandling, aksjonspunktPeriode))
                .isEqualTo(termindato.plus(aksjonspunktPeriode));
    }

    @Test
    public void skal_finne_behandlingsfrist_fra_manuelt_medlemskap_med_terminbekreftelse_i_fortiden() {
        this.scenario = ScenarioMorSøkerEngangsstønad
                .forFødsel()
                .medFødselAdopsjonsdato(Collections.singletonList(LocalDate.now().minusDays(3)));
        scenario.medBekreftetHendelse().medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
                .medTermindato(LocalDate.now().minusWeeks(6)));
        lagBehandling();
        var aksjonspunktPeriode = Period.ofWeeks(3);
        assertThat(dokumentBehandlingTjeneste.utledFristMedlemskap(behandling, aksjonspunktPeriode))
                .isEqualTo(LocalDate.now().plusWeeks(fristUker));
    }

    private void lagBehandling() {
        behandling = Mockito.spy(scenario.medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD).lagre(repositoryProvider));
    }

    @Test
    public void skal_lagre_ny_frist() {
        behandling = scenario.lagre(repositoryProvider);
        dokumentBehandlingTjeneste.oppdaterBehandlingMedNyFrist(behandling, now);
        assertThat(behandlingRepository.hentBehandling(behandling.getId()).getBehandlingstidFrist()).isEqualTo(now);
    }

    @Test
    public void skal_logge_i_repo_at_dokument_er_bestilt() {
        // Arrange
        behandling = scenario.lagre(repositoryProvider);

        // Act
        dokumentBehandlingTjeneste.loggDokumentBestilt(behandling, DokumentMalType.INNHENTE_OPPLYSNINGER_DOK);

        // Assert
        var behandlingDokument = behandlingDokumentRepository.hentHvisEksisterer(behandling.getId());
        assertThat(behandlingDokument).isPresent();
        assertThat(behandlingDokument.get().getBestilteDokumenter()).hasSize(1);
        assertThat(behandlingDokument.get().getBestilteDokumenter().get(0).getDokumentMalType()).isEqualTo(DokumentMalType.INNHENTE_OPPLYSNINGER_DOK.getKode());
    }

    @Test
    public void skal_returnere_true_når_dokument_er_bestilt() {
        // Arrange
        behandling = scenario.lagre(repositoryProvider);
        dokumentBehandlingTjeneste.loggDokumentBestilt(behandling, DokumentMalType.INNHENTE_OPPLYSNINGER_DOK);

        // Act+Assert
        assertThat(dokumentBehandlingTjeneste.erDokumentBestilt(behandling.getId(), DokumentMalType.INNHENTE_OPPLYSNINGER_DOK)).isTrue();
    }

    @Test
    public void skal_returnere_false_når_dokument_ikke_er_bestilt() {
        // Arrange
        behandling = scenario.lagre(repositoryProvider);
        dokumentBehandlingTjeneste.loggDokumentBestilt(behandling, DokumentMalType.ETTERLYS_INNTEKTSMELDING);

        // Act+Assert
        assertThat(dokumentBehandlingTjeneste.erDokumentBestilt(behandling.getId(), DokumentMalType.INNHENTE_OPPLYSNINGER_DOK)).isFalse();
    }

    @Test
    public void skal_nullstille_vedtak_fritekst() {
        // Arrange
        behandling = scenario.lagre(repositoryProvider);
        var behandlingDokumentBuilder = BehandlingDokumentEntitet.Builder.ny()
            .medBehandling(behandling.getId())
            .medVedtakFritekst(VEDTAK_FRITEKST);
        behandlingDokumentRepository.lagreOgFlush(behandlingDokumentBuilder.build());

        Optional<BehandlingDokumentEntitet> behandlingDokumentFør = behandlingDokumentRepository.hentHvisEksisterer(behandling.getId());
        assertThat(behandlingDokumentFør).isPresent();
        assertThat(behandlingDokumentFør.get().getVedtakFritekst()).isEqualTo(VEDTAK_FRITEKST);

        // Act
        dokumentBehandlingTjeneste.nullstillVedtakFritekstHvisFinnes(behandling.getId());

        // Assert
        Optional<BehandlingDokumentEntitet> behandlingDokumentEtter = behandlingDokumentRepository.hentHvisEksisterer(behandling.getId());
        assertThat(behandlingDokumentEtter).isPresent();
        assertThat(behandlingDokumentEtter.get().getVedtakFritekst()).isNull();
    }

    @Test
    public void skal_håndtere_at_det_ikke_finnes_behandling_dokument_ved_nullstilling_av_vedtak_fritekst() {
        // Arrange
        behandling = scenario.lagre(repositoryProvider);

        // Act
        dokumentBehandlingTjeneste.nullstillVedtakFritekstHvisFinnes(behandling.getId());

        // Assert
        Optional<BehandlingDokumentEntitet> behandlingDokument = behandlingDokumentRepository.hentHvisEksisterer(behandling.getId());
        assertThat(behandlingDokument).isNotPresent();
    }
}

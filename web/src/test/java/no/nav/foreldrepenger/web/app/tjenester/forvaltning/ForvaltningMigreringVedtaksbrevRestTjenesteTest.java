package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.MellomlagringRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.MellomlagringType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingIdDto;

@CdiDbAwareTest
class ForvaltningMigreringVedtaksbrevRestTjenesteTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private BehandlingDokumentRepository behandlingDokumentRepository;
    @Inject
    private MellomlagringRepository mellomlagringRepository;

    private ForvaltningMigreringVedtaksbrevRestTjeneste tjeneste;
    private Behandling behandling;
    private BehandlingIdDto behandlingIdDto;

    @BeforeEach
    void setUp() {
        behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        behandlingIdDto = new BehandlingIdDto(behandling.getUuid());
        tjeneste = new ForvaltningMigreringVedtaksbrevRestTjeneste(behandlingRepository, behandlingDokumentRepository, mellomlagringRepository);
    }

    @Test
    void migrerer_vedtaksbrev_til_mellomlagring() {
        // Arrange
        var html = "<html><body><h1>Vedtak om foreldrepenger</h1><p>Innhold</p></body></html>";
        var dokument = BehandlingDokumentEntitet.Builder.ny()
            .medBehandling(behandling.getId())
            .medOverstyrtBrevFritekstHtml(html)
            .build();
        behandlingDokumentRepository.lagreOgFlush(dokument);

        // Act
        var respons = tjeneste.migrerVedtaksbrev(behandlingIdDto);

        // Assert
        assertThat(respons.getStatus()).isEqualTo(200);
        var lagret = mellomlagringRepository.hentMellomlagring(behandling.getId(), MellomlagringType.VEDTAKSBREV);
        assertThat(lagret).isPresent();
        assertThat(lagret.get().getInnhold()).isEqualTo(html);
    }

    @Test
    void returnerer_404_naar_behandling_ikke_finnes() {
        // Arrange
        var ukjentUuid = java.util.UUID.randomUUID();
        var ukjentBehandlingIdDto = new BehandlingIdDto(ukjentUuid);

        // Act
        var respons = tjeneste.migrerVedtaksbrev(ukjentBehandlingIdDto);

        // Assert
        assertThat(respons.getStatus()).isEqualTo(404);
        assertThat(respons.getEntity().toString()).contains("ikke funnet");
    }

    @Test
    void returnerer_400_naar_ingen_overstyrt_brev_finnes() {
        // Arrange
        var dokument = BehandlingDokumentEntitet.Builder.ny()
            .medBehandling(behandling.getId())
            .medOverstyrtBrevFritekstHtml(null)
            .build();
        behandlingDokumentRepository.lagreOgFlush(dokument);

        // Act
        var respons = tjeneste.migrerVedtaksbrev(behandlingIdDto);

        // Assert
        assertThat(respons.getStatus()).isEqualTo(400);
        assertThat(respons.getEntity().toString()).contains("har ikke overstyrt vedtaksbrev å migrere");
    }

    @Test
    void returnerer_400_og_migrerer_ikke_naar_behandling_er_avsluttet() {
        // Arrange
        var dokument = BehandlingDokumentEntitet.Builder.ny()
            .medBehandling(behandling.getId())
            .medOverstyrtBrevFritekstHtml("<html><body><h1>Vedtak</h1></body></html>")
            .build();
        behandlingDokumentRepository.lagreOgFlush(dokument);
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, repositoryProvider.getBehandlingLåsRepository().taLås(behandling.getId()));

        // Act
        var respons = tjeneste.migrerVedtaksbrev(behandlingIdDto);

        // Assert
        assertThat(respons.getStatus()).isEqualTo(400);
        assertThat(respons.getEntity().toString()).contains("Behandling er avsluttet");
        assertThat(mellomlagringRepository.hentMellomlagring(behandling.getId(), MellomlagringType.VEDTAKSBREV)).isEmpty();
    }

    @Test
    void returnerer_409_naar_vedtaksbrev_allerede_finnes_i_mellomlagring() {
        // Arrange
        var html = "<html><body><h1>Vedtak</h1></body></html>";
        var dokument = BehandlingDokumentEntitet.Builder.ny()
            .medBehandling(behandling.getId())
            .medOverstyrtBrevFritekstHtml(html)
            .build();
        behandlingDokumentRepository.lagreOgFlush(dokument);

        mellomlagringRepository.lagreEllerOppdater(behandling.getId(), MellomlagringType.VEDTAKSBREV, "<p>Allerede eksisterende</p>");

        // Act
        var respons = tjeneste.migrerVedtaksbrev(behandlingIdDto);

        // Assert
        assertThat(respons.getStatus()).isEqualTo(409);
        assertThat(respons.getEntity().toString()).contains("har allerede vedtaksbrev i mellomlagring");

        // Verifiser at eksisterende innhold ikke ble overskrevet
        var lagret = mellomlagringRepository.hentMellomlagring(behandling.getId(), MellomlagringType.VEDTAKSBREV);
        assertThat(lagret.get().getInnhold()).isEqualTo("<p>Allerede eksisterende</p>");
    }
}

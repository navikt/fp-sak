package no.nav.foreldrepenger.behandlingslager.behandling.dokument;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.JournalpostId;

class BehandlingDokumentRepositoryTest extends EntityManagerAwareTest {

    private BehandlingDokumentRepository behandlingDokumentRepository;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        behandlingDokumentRepository = new BehandlingDokumentRepository(entityManager);
    }

    @Test
    void lagreBehandlingDokument() {
        // Arrange
        var behandling = opprettBehandling();
        var bestillingUuid = UUID.randomUUID();
        lagreDokumentBestillingFor(behandling, bestillingUuid);

        var behandlingDokumenter = behandlingDokumentRepository.hentHvisEksisterer(behandling.getId());

        assertThat(behandlingDokumenter).isPresent();
        assertThat(behandlingDokumenter.get().getBestilteDokumenter()).hasSize(1);

        var lagretBestilling = behandlingDokumentRepository.hentHvisEksisterer(bestillingUuid);

        assertThat(lagretBestilling).isPresent();
        assertThat(lagretBestilling.get().getBestillingUuid()).isEqualTo(bestillingUuid);
    }

    @Test
    void oppdatereBehandlingDokumentEntitet() {
        // Arrange
        var behandling = opprettBehandling();
        var bestillingUuid = UUID.randomUUID();
        lagreDokumentBestillingFor(behandling, bestillingUuid);

        var lagretBestilling = behandlingDokumentRepository.hentHvisEksisterer(bestillingUuid);

        var journalpostId = "123456789";
        lagretBestilling.ifPresent(bestilling -> {
            bestilling.setJournalpostId(new JournalpostId(journalpostId));
            behandlingDokumentRepository.lagreOgFlush(bestilling);
        });

        var oppdatertBestilling = behandlingDokumentRepository.hentHvisEksisterer(bestillingUuid);
        assertThat(oppdatertBestilling).isPresent();
        assertThat(oppdatertBestilling.get().getJournalpostId().getVerdi()).isEqualTo(journalpostId);
    }

    private void lagreDokumentBestillingFor(Behandling behandling, UUID bestillingUuid) {
        var dokumenter = BehandlingDokumentEntitet.Builder.ny()
            .medBehandling(behandling.getId())
            .build();

        dokumenter.leggTilBestiltDokument(new BehandlingDokumentBestiltEntitet.Builder()
            .medBestillingUuid(bestillingUuid)
            .medDokumentMalType(DokumentMalType.FRITEKSTBREV)
            .medBehandlingDokument(dokumenter)
            .medOpprinneligDokumentMal(DokumentMalType.FORELDREPENGER_INNVILGELSE)
            .build());

        behandlingDokumentRepository.lagreOgFlush(dokumenter);
    }

    private Behandling opprettBehandling() {
        var behandlingBuilder = new BasicBehandlingBuilder(getEntityManager());
        var behandling = behandlingBuilder.opprettOgLagreFørstegangssøknad(FagsakYtelseType.FORELDREPENGER);
        var resultat = Behandlingsresultat.builder().build();
        behandlingBuilder.lagreBehandlingsresultat(behandling.getId(), resultat);
        return behandling;
    }
}

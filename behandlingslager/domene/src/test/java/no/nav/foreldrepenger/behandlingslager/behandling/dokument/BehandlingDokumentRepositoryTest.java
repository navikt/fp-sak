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

    @Test
    void mellomlagretBrev_lagre_og_hent() {
        var behandling = opprettBehandling();
        var behandlingDokument = lagreBehandlingDokumentFor(behandling);

        var mellomlagring = BehandlingBrevMellomlagringEntitet.Builder.ny()
            .medBehandlingDokument(behandlingDokument)
            .medDokumentMalType(DokumentMalType.VARSEL_OM_REVURDERING)
            .medFritekstHtml("<p>Hei</p>")
            .build();
        behandlingDokumentRepository.lagreOgFlush(mellomlagring);

        var funnet = behandlingDokumentRepository.hentMellomlagretBrev(behandling.getId(), DokumentMalType.VARSEL_OM_REVURDERING);
        assertThat(funnet).isPresent();
        assertThat(funnet.get().getFritekstHtml()).isEqualTo("<p>Hei</p>");
    }

    @Test
    void mellomlagretBrev_hent_returnerer_empty_for_ukjent_mal() {
        var behandling = opprettBehandling();
        lagreBehandlingDokumentFor(behandling);

        var funnet = behandlingDokumentRepository.hentMellomlagretBrev(behandling.getId(), DokumentMalType.VARSEL_OM_REVURDERING);
        assertThat(funnet).isEmpty();
    }

    @Test
    void mellomlagretBrev_fjern_én_type() {
        var behandling = opprettBehandling();
        var behandlingDokument = lagreBehandlingDokumentFor(behandling);

        behandlingDokumentRepository.lagreOgFlush(BehandlingBrevMellomlagringEntitet.Builder.ny()
            .medBehandlingDokument(behandlingDokument)
            .medDokumentMalType(DokumentMalType.VARSEL_OM_REVURDERING)
            .medFritekstHtml("<p>Varsel</p>")
            .build());
        behandlingDokumentRepository.lagreOgFlush(BehandlingBrevMellomlagringEntitet.Builder.ny()
            .medBehandlingDokument(behandlingDokument)
            .medDokumentMalType(DokumentMalType.INNHENTE_OPPLYSNINGER)
            .medFritekstHtml("<p>Innhent</p>")
            .build());

        behandlingDokumentRepository.fjernMellomlagretBrev(behandling.getId(), DokumentMalType.VARSEL_OM_REVURDERING);

        assertThat(behandlingDokumentRepository.hentMellomlagretBrev(behandling.getId(), DokumentMalType.VARSEL_OM_REVURDERING)).isEmpty();
        assertThat(behandlingDokumentRepository.hentMellomlagretBrev(behandling.getId(), DokumentMalType.INNHENTE_OPPLYSNINGER)).isPresent();
    }

    @Test
    void mellomlagretBrev_fjern_alle() {
        var behandling = opprettBehandling();
        var behandlingDokument = lagreBehandlingDokumentFor(behandling);

        behandlingDokumentRepository.lagreOgFlush(BehandlingBrevMellomlagringEntitet.Builder.ny()
            .medBehandlingDokument(behandlingDokument)
            .medDokumentMalType(DokumentMalType.VARSEL_OM_REVURDERING)
            .medFritekstHtml("<p>Varsel</p>")
            .build());
        behandlingDokumentRepository.lagreOgFlush(BehandlingBrevMellomlagringEntitet.Builder.ny()
            .medBehandlingDokument(behandlingDokument)
            .medDokumentMalType(DokumentMalType.INNHENTE_OPPLYSNINGER)
            .medFritekstHtml("<p>Innhent</p>")
            .build());

        behandlingDokumentRepository.fjernAlleMellomlagredeBrev(behandling.getId());

        assertThat(behandlingDokumentRepository.hentMellomlagretBrev(behandling.getId(), DokumentMalType.VARSEL_OM_REVURDERING)).isEmpty();
        assertThat(behandlingDokumentRepository.hentMellomlagretBrev(behandling.getId(), DokumentMalType.INNHENTE_OPPLYSNINGER)).isEmpty();
    }

    @Test
    void mellomlagretBrev_oppdaterer_eksisterende_ved_lagre() {
        var behandling = opprettBehandling();
        var behandlingDokument = lagreBehandlingDokumentFor(behandling);

        var mellomlagring = BehandlingBrevMellomlagringEntitet.Builder.ny()
            .medBehandlingDokument(behandlingDokument)
            .medDokumentMalType(DokumentMalType.VARSEL_OM_REVURDERING)
            .medFritekstHtml("<p>Opprinnelig</p>")
            .build();
        behandlingDokumentRepository.lagreOgFlush(mellomlagring);

        mellomlagring.setFritekstHtml("<p>Oppdatert</p>");
        behandlingDokumentRepository.lagreOgFlush(mellomlagring);

        var funnet = behandlingDokumentRepository.hentMellomlagretBrev(behandling.getId(), DokumentMalType.VARSEL_OM_REVURDERING);
        assertThat(funnet).isPresent();
        assertThat(funnet.get().getFritekstHtml()).isEqualTo("<p>Oppdatert</p>");
    }

    private BehandlingDokumentEntitet lagreBehandlingDokumentFor(Behandling behandling) {
        var dokument = BehandlingDokumentEntitet.Builder.ny()
            .medBehandling(behandling.getId())
            .build();
        behandlingDokumentRepository.lagreOgFlush(dokument);
        return dokument;
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

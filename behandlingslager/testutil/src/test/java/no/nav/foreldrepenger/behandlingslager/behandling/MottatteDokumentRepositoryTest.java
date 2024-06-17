package no.nav.foreldrepenger.behandlingslager.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.JournalpostId;

class MottatteDokumentRepositoryTest extends EntityManagerAwareTest {

    private MottatteDokumentRepository mottatteDokumentRepository;
    private BehandlingRepository behandlingRepository;

    @BeforeEach
    void setup() {
        var entityManager = getEntityManager();
        mottatteDokumentRepository = new MottatteDokumentRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
    }

    @Test
    void skal_hente_alle_MottatteDokument_på_behandlingId() {
        var behandling = opprettBehandling(opprettFagsak());
        opprettDokument(behandling);
        //Act
        var mottatteDokumenter = mottatteDokumentRepository.hentMottatteDokument(behandling.getId());

        //Assert
        assertThat(mottatteDokumenter).hasSize(1);
        assertThat(mottatteDokumenter.get(0).getBehandlingId()).isEqualTo(behandling.getId());
    }

    @Test
    void skal_hente_alle_MottatteDokument_på_fagsakId() {
        var fagsak = opprettFagsak();
        var behandling1 = opprettBehandling(fagsak);
        var behandling2 = opprettBehandling(fagsak);
        opprettDokument(behandling1);
        opprettDokument(behandling2);
        //Act
        var mottatteDokumenter = mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(behandling1.getFagsakId());

        //Assert
        assertThat(mottatteDokumenter).hasSize(2);
        assertThat(mottatteDokumenter.stream()
            .allMatch(md -> md.getBehandlingId().equals(behandling1.getId()) || md.getBehandlingId().equals(behandling2.getId()))).isTrue();
    }

    @Test
    void skal_hente_MottattDokument_på_id() {
        var fagsak = opprettFagsak();
        var behandling1 = opprettBehandling(fagsak);
        var behandling2 = opprettBehandling(fagsak);
        var dokument1 = opprettDokument(behandling1);
        var dokument2 = opprettDokument(behandling2);

        //Act
        var mottattDokument1 = mottatteDokumentRepository.hentMottattDokument(dokument1.getId());
        var mottattDokument2 = mottatteDokumentRepository.hentMottattDokument(dokument2.getId());

        //Assert
        assertThat(dokument1).isEqualTo(mottattDokument1.get());
        assertThat(dokument2).isEqualTo(mottattDokument2.get());
    }

    private Fagsak opprettFagsak() {
        var fagsak = FagsakBuilder.nyEngangstønadForMor().build();
        new FagsakRepository(getEntityManager()).opprettNy(fagsak);
        return fagsak;
    }

    private void lagreBehandling(Behandling behandling) {
        var lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);
    }

    public static MottattDokument lagMottatteDokument(long behandlingId, long fagsakId) {
        return new MottattDokument.Builder().medBehandlingId(behandlingId)
            .medJournalPostId(new JournalpostId("123"))
            .medDokumentType(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL)
            .medMottattDato(LocalDate.now())
            .medElektroniskRegistrert(true)
            .medFagsakId(fagsakId)
            .build();
    }

    private Behandling opprettBehandling(Fagsak fagsak) {
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        lagreBehandling(behandling);
        return behandling;
    }

    private MottattDokument opprettDokument(Behandling behandling) {
        var dokument = lagMottatteDokument(behandling.getId(), behandling.getFagsakId());
        mottatteDokumentRepository.lagre(dokument);
        return dokument;
    }

}

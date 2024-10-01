package no.nav.foreldrepenger.behandlingslager.pip;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Journalpost;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;

class PipRepositoryTest extends EntityManagerAwareTest {

    private static final JournalpostId JOURNALPOST_ID = new JournalpostId("42");

    private BehandlingRepository behandlingRepository;
    private PipRepository pipRepository;
    private FagsakRepository fagsakRepository;

    private BasicBehandlingBuilder behandlingBuilder;

    private void lagreBehandling(Behandling behandling) {
        var lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);
    }

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        behandlingRepository = new BehandlingRepository(entityManager);
        pipRepository = new PipRepository(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);
        behandlingBuilder = new BasicBehandlingBuilder(entityManager);
    }

    @Test
    void skal_finne_behandligstatus_og_sakstatus_for_behandling() {
        var behandling = behandlingBuilder.opprettOgLagreFørstegangssøknad(FagsakYtelseType.FORELDREPENGER);
        lagreBehandling(behandling);

        var pipBehandlingsData = pipRepository.hentDataForBehandling(behandling.getId());
        assertThat(pipBehandlingsData).isPresent();
        assertThat(pipBehandlingsData.get()).isNotNull();
        assertThat(pipBehandlingsData.get().getBehandligStatus()).isEqualTo(behandling.getStatus().getKode());
        assertThat(pipBehandlingsData.get().getFagsakStatus()).isEqualTo(behandling.getFagsak().getStatus().getKode());
        assertThat(pipBehandlingsData.get().getFagsakId()).isEqualTo(behandling.getFagsak().getId());
    }

    @Test
    void skal_finne_behandligstatus_og_sakstatus_for_behandlingUuid() {
        var behandling = behandlingBuilder.opprettOgLagreFørstegangssøknad(FagsakYtelseType.FORELDREPENGER);
        lagreBehandling(behandling);

        var pipBehandlingsData = pipRepository.hentDataForBehandlingUuid(behandling.getUuid());
        assertThat(pipBehandlingsData).isPresent();
        assertThat(pipBehandlingsData.get()).isNotNull();
        assertThat(pipBehandlingsData.get().getBehandligStatus()).isEqualTo(behandling.getStatus().getKode());
        assertThat(pipBehandlingsData.get().getFagsakStatus()).isEqualTo(behandling.getFagsak().getStatus().getKode());
        assertThat(pipBehandlingsData.get().getFagsakId()).isEqualTo(behandling.getFagsak().getId());
    }

    @Test
    void skal_returne_tomt_resultat_når_det_søkes_etter_behandling_id_som_ikke_finnes() {
        var pipBehandlingsData = pipRepository.hentDataForBehandling(1241L);
        assertThat(pipBehandlingsData).isNotPresent();
    }

    @Test
    void skal_finne_alle_fagsaker_for_en_søker() {
        var fagsak1 = behandlingBuilder.opprettFagsak(FagsakYtelseType.FORELDREPENGER);
        var aktørId1 = fagsak1.getAktørId();
        var fagsak2 = behandlingBuilder.opprettFagsak(FagsakYtelseType.FORELDREPENGER, aktørId1);
        @SuppressWarnings("unused") var fagsakAnnenAktør = new BasicBehandlingBuilder(getEntityManager()).opprettFagsak(FagsakYtelseType.FORELDREPENGER);

        var resultat = pipRepository.fagsakIderForSøker(Collections.singleton(aktørId1));

        assertThat(resultat).containsOnly(fagsak1.getId(), fagsak2.getId());
    }

    @Test
    void skal_finne_aktoerId_for_fagsak() {
        var aktørId1 = AktørId.dummy();
        var fagsak = behandlingBuilder.opprettFagsak(FagsakYtelseType.FORELDREPENGER, aktørId1);

        var aktørIder = pipRepository.hentAktørIdKnyttetTilFagsaker(Collections.singleton(fagsak.getId()));
        assertThat(aktørIder).containsOnly(aktørId1);
    }

    @Test
    void skal_finne_fagsakId_knyttet_til_journalpostId() {
        var fagsak1 = behandlingBuilder.opprettFagsak(FagsakYtelseType.FORELDREPENGER);
        @SuppressWarnings("unused") var fagsak2 = behandlingBuilder.opprettFagsak(FagsakYtelseType.FORELDREPENGER);
        var journalpost1 = new Journalpost(JOURNALPOST_ID, fagsak1);
        fagsakRepository.lagre(journalpost1);
        var journalpost2 = new Journalpost(new JournalpostId("4444"), fagsak1);
        fagsakRepository.lagre(journalpost2);
        getEntityManager().flush();

        var fagsakId = pipRepository.fagsakIdForJournalpostId(Collections.singleton(JOURNALPOST_ID));
        assertThat(fagsakId).containsOnly(fagsak1.getId());
    }

    @Test
    void skal_finne_aksjonspunktTyper_for_aksjonspunktKoder() {
        var resultat1 = PipRepository.harAksjonspunktTypeOverstyring(Collections.singletonList(AksjonspunktDefinisjon.OVERSTYRING_AV_BEREGNING));
        assertThat(resultat1).isTrue();

        var resultat2 = PipRepository.harAksjonspunktTypeOverstyring(List.of(AksjonspunktDefinisjon.OVERSTYRING_AV_BEREGNING, AksjonspunktDefinisjon.VURDER_MEDLEMSKAPSVILKÅRET));
        assertThat(resultat2).isTrue();

        var resultat3 = PipRepository.harAksjonspunktTypeOverstyring(List.of(AksjonspunktDefinisjon.VURDER_MEDLEMSKAPSVILKÅRET));
        assertThat(resultat3).isFalse();
    }

}

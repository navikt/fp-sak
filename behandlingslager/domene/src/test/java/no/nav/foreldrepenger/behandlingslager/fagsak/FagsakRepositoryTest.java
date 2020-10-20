package no.nav.foreldrepenger.behandlingslager.fagsak;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerRepository;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class FagsakRepositoryTest extends EntityManagerAwareTest {

    private FagsakRepository fagsakRepository;

    @BeforeEach
    void setUp() {
        fagsakRepository = new FagsakRepository(getEntityManager());
    }

    @Test
    public void skal_finne_eksakt_fagsak_gitt_id() {
        AktørId aktørId = AktørId.dummy();
        Saksnummer saksnummer  = new Saksnummer("200");
        Fagsak fagsak = opprettFagsak(saksnummer, aktørId);

        Fagsak resultat = fagsakRepository.finnEksaktFagsak(fagsak.getId());

        assertThat(resultat).isNotNull();
    }

    @Test
    public void skal_finne_unik_fagsak_gitt_id() {
        AktørId aktørId = AktørId.dummy();
        Saksnummer saksnummer  = new Saksnummer("200");
        Fagsak fagsak = opprettFagsak(saksnummer, aktørId);

        Optional<Fagsak> resultat = fagsakRepository.finnUnikFagsak(fagsak.getId());

        assertThat(resultat).isPresent();
    }

    @Test
    public void skal_finne_fagsak_gitt_saksnummer() {
        AktørId aktørId = AktørId.dummy();
        Saksnummer saksnummer  = new Saksnummer("200");

        opprettFagsak(saksnummer, aktørId);
        Optional<Fagsak> optional = fagsakRepository.hentSakGittSaksnummer(saksnummer);

        assertThat(optional).isPresent();
    }

    @Test
    public void skal_finne_fagsak_gitt_aktør_id() {
        AktørId aktørId = AktørId.dummy();
        Saksnummer saksnummer  = new Saksnummer("200");

        opprettFagsak(saksnummer, aktørId);
        List<Fagsak> list = fagsakRepository.hentForBruker(aktørId);

        assertThat(list).hasSize(1);
    }

    @Test
    public void skal_finne_fagsaker_uten_behandling() {
        AktørId aktørId = AktørId.dummy();
        AktørId aktørId1 = AktørId.dummy();
        Saksnummer saksnummer  = new Saksnummer("200");
        Saksnummer saksnummer1  = new Saksnummer("201");

        opprettFagsak(saksnummer, aktørId);
        opprettFagsak(saksnummer1, aktørId1);
        List<Saksnummer> list = fagsakRepository.hentÅpneFagsakerUtenBehandling();

        assertThat(list).hasSize(2);
    }

    @Test
    public void skal_finne_journalpost_gitt_journalpost_id() {
        AktørId aktørId = AktørId.dummy();
        Saksnummer saksnummer  = new Saksnummer("200");
        JournalpostId journalpostId = new JournalpostId("30000");

        opprettFagsakMedJournalpost(journalpostId, saksnummer, aktørId);

        Optional<Journalpost> journalpost = fagsakRepository.hentJournalpost(journalpostId);
        assertThat(journalpost.isPresent()).isTrue();

    }

    private Fagsak opprettFagsak(Saksnummer saksnummer, AktørId aktørId) {
        NavBruker bruker = NavBruker.opprettNyNB(aktørId);

        // Opprett fagsak
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, bruker, null, saksnummer);
        var navBrukerRepository = new NavBrukerRepository(getEntityManager());
        navBrukerRepository.lagre(bruker);
        fagsakRepository.opprettNy(fagsak);
        return fagsak;
    }

    private void opprettFagsakMedJournalpost(JournalpostId journalpostId, Saksnummer saksnummer, AktørId aktørId) {
        Fagsak fagsak = opprettFagsak(saksnummer, aktørId);

        Journalpost journalpost = new Journalpost(journalpostId, fagsak);
        fagsakRepository.lagre(journalpost);
        //Fagsakrepo flusher ikke
        getEntityManager().flush();
    }
}

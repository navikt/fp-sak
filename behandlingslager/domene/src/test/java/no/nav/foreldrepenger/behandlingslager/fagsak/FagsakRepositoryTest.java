package no.nav.foreldrepenger.behandlingslager.fagsak;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerRepository;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

class FagsakRepositoryTest extends EntityManagerAwareTest {

    private FagsakRepository fagsakRepository;

    @BeforeEach
    void setUp() {
        fagsakRepository = new FagsakRepository(getEntityManager());
    }


    @Test
    void skal_finne_eksakt_fagsak_gitt_id() {
        var aktørId = AktørId.dummy();
        var saksnummer  = new Saksnummer("9999");
        var fagsak = opprettFagsak(saksnummer, aktørId);

        var resultat = fagsakRepository.finnEksaktFagsak(fagsak.getId());

        assertThat(resultat).isNotNull();
    }

    @Test
    void skal_finne_unik_fagsak_gitt_id() {
        var aktørId = AktørId.dummy();
        var saksnummer  = new Saksnummer("9999");
        var fagsak = opprettFagsak(saksnummer, aktørId);

        var resultat = fagsakRepository.finnUnikFagsak(fagsak.getId());

        assertThat(resultat).isPresent();
    }

    @Test
    void skal_finne_fagsak_gitt_saksnummer() {
        var aktørId = AktørId.dummy();
        var saksnummer  = new Saksnummer("9999");

        opprettFagsak(saksnummer, aktørId);
        var optional = fagsakRepository.hentSakGittSaksnummer(saksnummer);

        assertThat(optional).isPresent();
    }

    @Test
    void skal_finne_fagsak_gitt_aktør_id() {
        var aktørId = AktørId.dummy();
        var saksnummer  = new Saksnummer("9999");

        opprettFagsak(saksnummer, aktørId);
        var list = fagsakRepository.hentForBruker(aktørId);

        assertThat(list.stream().map(Fagsak::getSaksnummer)).contains(saksnummer);
    }

    @Test
    void skal_finne_fagsaker_uten_behandling() {
        var aktørId = AktørId.dummy();
        var aktørId1 = AktørId.dummy();
        var saksnummer  = new Saksnummer("9999");
        var saksnummer1  = new Saksnummer("100000");

        opprettFagsak(saksnummer, aktørId);
        opprettFagsak(saksnummer1, aktørId1);
        var list = fagsakRepository.hentÅpneFagsakerUtenBehandling();

        assertThat(list).contains(saksnummer, saksnummer1);
    }

    @Test
    void skal_finne_journalpost_gitt_journalpost_id() {
        var aktørId = AktørId.dummy();
        var saksnummer  = new Saksnummer("9999");
        var journalpostId = new JournalpostId("30000");

        opprettFagsakMedJournalpost(journalpostId, saksnummer, aktørId);

        var journalpost = fagsakRepository.hentJournalpost(journalpostId);
        assertThat(journalpost).isPresent();

    }

    private Fagsak opprettFagsak(Saksnummer saksnummer, AktørId aktørId) {
        var bruker = NavBruker.opprettNyNB(aktørId);

        // Opprett fagsak
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, bruker, null, saksnummer);
        var navBrukerRepository = new NavBrukerRepository(getEntityManager());
        navBrukerRepository.lagre(bruker);
        fagsakRepository.opprettNy(fagsak);
        return fagsak;
    }

    private Fagsak opprettFagsak(AktørId aktørId) {
        var bruker = NavBruker.opprettNyNB(aktørId);

        // Opprett fagsak
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, bruker);
        var navBrukerRepository = new NavBrukerRepository(getEntityManager());
        navBrukerRepository.lagre(bruker);
        fagsakRepository.opprettNy(fagsak);
        return fagsak;
    }

    private void opprettFagsakMedJournalpost(JournalpostId journalpostId, Saksnummer saksnummer, AktørId aktørId) {
        var fagsak = opprettFagsak(saksnummer, aktørId);

        var journalpost = new Journalpost(journalpostId, fagsak);
        fagsakRepository.lagre(journalpost);
        //Fagsakrepo flusher ikke
        getEntityManager().flush();
    }
}

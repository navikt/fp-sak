package no.nav.foreldrepenger.behandlingslager.fagsak;

import static java.time.Month.JANUARY;
import static no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn.KVINNE;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.testutilities.db.Repository;

public class FagsakRepositoryImplTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private Repository repository = repoRule.getRepository();
    private FagsakRepository fagsakRepository = new FagsakRepository(repoRule.getEntityManager());

    @Test
    public void skal_finne_eksakt_fagsak_gitt_id() {
        AktørId aktørId = AktørId.dummy();
        Saksnummer saksnummer  = new Saksnummer("200");
        Fagsak fagsak = opprettFagsak(saksnummer, aktørId);

        Fagsak resultat = fagsakRepository.finnEksaktFagsak(fagsak.getId());

        Assertions.assertThat(resultat).isNotNull();
    }

    @Test
    public void skal_finne_unik_fagsak_gitt_id() {
        AktørId aktørId = AktørId.dummy();
        Saksnummer saksnummer  = new Saksnummer("200");
        Fagsak fagsak = opprettFagsak(saksnummer, aktørId);

        Optional<Fagsak> resultat = fagsakRepository.finnUnikFagsak(fagsak.getId());

        Assertions.assertThat(resultat).isPresent();
    }

    @Test
    public void skal_finne_fagsak_gitt_saksnummer() {
        AktørId aktørId = AktørId.dummy();
        Saksnummer saksnummer  = new Saksnummer("200");

        opprettFagsak(saksnummer, aktørId);
        Optional<Fagsak> optional = fagsakRepository.hentSakGittSaksnummer(saksnummer);

        Assertions.assertThat(optional).isPresent();
    }

    @Test
    public void skal_finne_fagsak_gitt_aktør_id() {
        AktørId aktørId = AktørId.dummy();
        Saksnummer saksnummer  = new Saksnummer("200");

        opprettFagsak(saksnummer, aktørId);
        List<Fagsak> list = fagsakRepository.hentForBruker(aktørId);

        Assertions.assertThat(list).hasSize(1);
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

        Assertions.assertThat(list).hasSize(2);
    }

    @Test
    public void skal_finne_journalpost_gitt_journalpost_id() {
        AktørId aktørId = AktørId.dummy();
        Saksnummer saksnummer  = new Saksnummer("200");
        JournalpostId journalpostId = new JournalpostId("30000");

        opprettJournalpost(journalpostId, saksnummer, aktørId);

        Optional<Journalpost> journalpost = fagsakRepository.hentJournalpost(journalpostId);
        assertTrue(journalpost.isPresent());

    }

    private Fagsak opprettFagsak(Saksnummer saksnummer, AktørId aktørId) {
        NavBruker bruker = NavBruker.opprettNy(
            new Personinfo.Builder()
                .medAktørId(aktørId)
                .medPersonIdent(new PersonIdent("12345678901"))
                .medNavn("Kari Nordmann")
                .medFødselsdato(LocalDate.of(1990, JANUARY, 1))
                .medNavBrukerKjønn(KVINNE)
                .medForetrukketSpråk(Språkkode.NB)
                .build());

        // Opprett fagsak
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, bruker, null, saksnummer);
        repository.lagre(bruker);
        repository.lagre(fagsak);
        repository.flushAndClear();
        return fagsak;
    }

    private Journalpost opprettJournalpost(JournalpostId journalpostId, Saksnummer saksnummer, AktørId aktørId) {
        Fagsak fagsak = opprettFagsak(saksnummer, aktørId);

        Journalpost journalpost = new Journalpost(journalpostId, fagsak);
        repository.lagre(journalpost);
        repository.flushAndClear();
        return journalpost;
    }
}

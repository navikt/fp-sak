package no.nav.foreldrepenger.behandlingslager.fagsak;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.UtlandDokumentasjonStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.UtlandMarkering;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

class FagsakEgenskapRepositoryTest extends EntityManagerAwareTest {

    private FagsakRepository fagsakRepository;
    private FagsakEgenskapRepository fagsakEgenskapRepository;

    @BeforeEach
    void setUp() {
        fagsakRepository = new FagsakRepository(getEntityManager());
        fagsakEgenskapRepository = new FagsakEgenskapRepository(getEntityManager());
    }


    @Test
    void skal_lagre_to_egenskaper_og_finne_dem() {
        var aktørId = AktørId.dummy();
        var saksnummer  = new Saksnummer("9999");
        var fagsak = opprettFagsak(saksnummer, aktørId);

        fagsakEgenskapRepository.lagreEgenskapBeholdHistorikk(fagsak.getId(), UtlandMarkering.BOSATT_UTLAND);
        fagsakEgenskapRepository.lagreEgenskapBeholdHistorikk(fagsak.getId(), UtlandDokumentasjonStatus.DOKUMENTASJON_VIL_IKKE_BLI_INNHENTET);

        var resultat = fagsakEgenskapRepository.finnEgenskaper(fagsak.getId());
        assertThat(resultat).hasSize(2);
        assertThat(resultat.stream().map(FagsakEgenskap::getEgenskapNøkkel).toList()).contains(EgenskapNøkkel.UTLAND_DOKUMENTASJON);

        var markering = fagsakEgenskapRepository.finnUtlandMarkering(fagsak.getId());
        assertThat(markering).hasValueSatisfying(m -> assertThat(m).isEqualTo(UtlandMarkering.BOSATT_UTLAND));
    }

    @Test
    void skal_lagre_en_egenskap_og_fjerne_den() {
        var aktørId = AktørId.dummy();
        var saksnummer  = new Saksnummer("9999");
        var fagsak = opprettFagsak(saksnummer, aktørId);

        fagsakEgenskapRepository.lagreEgenskapUtenHistorikk(fagsak.getId(), UtlandMarkering.BOSATT_UTLAND);

        var resultat = fagsakEgenskapRepository.finnEgenskaper(fagsak.getId());
        assertThat(resultat).hasSize(1);

        fagsakEgenskapRepository.fjernEgenskapUtenHistorikk(fagsak.getId(), EgenskapNøkkel.UTLAND_MARKERING);

        var markering = fagsakEgenskapRepository.finnUtlandMarkering(fagsak.getId());
        assertThat(markering).isEmpty();
    }

    @Test
    void skal_lagre_en_egenskap_og_endre_den() {
        var aktørId = AktørId.dummy();
        var saksnummer  = new Saksnummer("9999");
        var fagsak = opprettFagsak(saksnummer, aktørId);

        fagsakEgenskapRepository.lagreEgenskapUtenHistorikk(fagsak.getId(), UtlandDokumentasjonStatus.DOKUMENTASJON_VIL_BLI_INNHENTET);

        var markering = fagsakEgenskapRepository.finnUtlandDokumentasjonStatus(fagsak.getId());
        assertThat(markering).hasValueSatisfying(m -> assertThat(m).isEqualTo(UtlandDokumentasjonStatus.DOKUMENTASJON_VIL_BLI_INNHENTET));

        fagsakEgenskapRepository.lagreEgenskapUtenHistorikk(fagsak.getId(), UtlandDokumentasjonStatus.DOKUMENTASJON_ER_INNHENTET);

        assertThat(fagsakEgenskapRepository.finnEgenskaper(fagsak.getId())).hasSize(1);
        markering = fagsakEgenskapRepository.finnUtlandDokumentasjonStatus(fagsak.getId());
        assertThat(markering).hasValueSatisfying(m -> assertThat(m).isEqualTo(UtlandDokumentasjonStatus.DOKUMENTASJON_ER_INNHENTET));
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

}

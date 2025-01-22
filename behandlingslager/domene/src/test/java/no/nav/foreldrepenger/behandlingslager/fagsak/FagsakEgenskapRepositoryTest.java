package no.nav.foreldrepenger.behandlingslager.fagsak;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.FagsakMarkering;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.UtlandDokumentasjonStatus;
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

        fagsakEgenskapRepository.lagreAlleFagsakMarkeringer(fagsak.getId(), Set.of(FagsakMarkering.BOSATT_UTLAND));
        fagsakEgenskapRepository.lagreUtlandDokumentasjonStatus(fagsak.getId(), UtlandDokumentasjonStatus.DOKUMENTASJON_VIL_IKKE_BLI_INNHENTET);

        var resultat = fagsakEgenskapRepository.finnUtlandDokumentasjonStatus(fagsak.getId());
        assertThat(resultat)
            .isPresent()
            .hasValueSatisfying(v -> assertThat(EgenskapNøkkel.UTLAND_DOKUMENTASJON).isEqualTo(v.getNøkkel()));

        var markering = fagsakEgenskapRepository.finnFagsakMarkeringer(fagsak.getId());
        assertThat(markering).contains(FagsakMarkering.BOSATT_UTLAND);
    }

    @Test
    void skal_lagre_og_oppdatere_egenskaper() {
        var aktørId = AktørId.dummy();
        var saksnummer  = new Saksnummer("9999");
        var fagsak = opprettFagsak(saksnummer, aktørId);

        fagsakEgenskapRepository.lagreAlleFagsakMarkeringer(fagsak.getId(), Set.of(FagsakMarkering.BOSATT_UTLAND));

        var markering = fagsakEgenskapRepository.finnFagsakMarkeringer(fagsak.getId());
        assertThat(markering).containsOnly(FagsakMarkering.BOSATT_UTLAND);

        fagsakEgenskapRepository.leggTilFagsakMarkering(fagsak.getId(), FagsakMarkering.SELVSTENDIG_NÆRING);

        markering = fagsakEgenskapRepository.finnFagsakMarkeringer(fagsak.getId());
        assertThat(markering).containsOnly(FagsakMarkering.BOSATT_UTLAND, FagsakMarkering.SELVSTENDIG_NÆRING);

        fagsakEgenskapRepository.lagreAlleFagsakMarkeringer(fagsak.getId(), Set.of(FagsakMarkering.EØS_BOSATT_NORGE));

        markering = fagsakEgenskapRepository.finnFagsakMarkeringer(fagsak.getId());
        assertThat(markering).containsOnly(FagsakMarkering.EØS_BOSATT_NORGE);
    }

    @Test
    void skal_lagre_en_egenskap_og_endre_den() {
        var aktørId = AktørId.dummy();
        var saksnummer  = new Saksnummer("9999");
        var fagsak = opprettFagsak(saksnummer, aktørId);

        fagsakEgenskapRepository.lagreUtlandDokumentasjonStatus(fagsak.getId(), UtlandDokumentasjonStatus.DOKUMENTASJON_VIL_BLI_INNHENTET);

        var markering = fagsakEgenskapRepository.finnUtlandDokumentasjonStatus(fagsak.getId());
        assertThat(markering).hasValueSatisfying(m -> assertThat(m).isEqualTo(UtlandDokumentasjonStatus.DOKUMENTASJON_VIL_BLI_INNHENTET));

        fagsakEgenskapRepository.lagreUtlandDokumentasjonStatus(fagsak.getId(), UtlandDokumentasjonStatus.DOKUMENTASJON_ER_INNHENTET);

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

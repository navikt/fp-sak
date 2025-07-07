package no.nav.foreldrepenger.behandlingslager.fagsak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.exception.VLException;

class FagsakRelasjonRepositoryTest extends EntityManagerAwareTest {

    private FagsakRepository fagsakRepository;
    private FagsakRelasjonRepository relasjonRepository;
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        entityManager = getEntityManager();
        fagsakRepository = new FagsakRepository(entityManager);
        relasjonRepository = new FagsakRelasjonRepository(entityManager, new FagsakLåsRepository(entityManager));
    }

    @Test
    void skal_ikke_kunne_kobles_med_seg_selv() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()));
        fagsakRepository.opprettNy(fagsak);
        relasjonRepository.opprettRelasjon(fagsak);
        assertThrows(VLException.class, () -> relasjonRepository.kobleFagsaker(fagsak, fagsak));
    }

    @Test
    void skal_ikke_kunne_kobles_med_fagsak_med_identisk_aktørid() {
        var bruker = NavBruker.opprettNyNB(AktørId.dummy());
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, bruker);
        fagsakRepository.opprettNy(fagsak);
        var fagsak2 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, bruker);
        fagsakRepository.opprettNy(fagsak2);

        relasjonRepository.opprettRelasjon(fagsak);
        assertThrows(VLException.class, () -> relasjonRepository.kobleFagsaker(fagsak, fagsak2));
    }

    @Test
    void skal_ikke_kunne_kobles_med_fagsak_med_ulik_ytelse() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()));
        fagsakRepository.opprettNy(fagsak);
        var fagsak2 = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, NavBruker.opprettNyNB(AktørId.dummy()));
        fagsakRepository.opprettNy(fagsak2);

        relasjonRepository.opprettRelasjon(fagsak);
        assertThrows(VLException.class, () -> relasjonRepository.kobleFagsaker(fagsak, fagsak2));
    }

    @Test
    void skal_koble_sammen_fagsak_med_lik_ytelse_type_og_ulik_aktør() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()));
        fagsakRepository.opprettNy(fagsak);
        var fagsak2 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()));
        fagsakRepository.opprettNy(fagsak2);

        relasjonRepository.opprettRelasjon(fagsak);
        relasjonRepository.kobleFagsaker(fagsak, fagsak2);
        var fagsakRelasjon = relasjonRepository.finnRelasjonFor(fagsak);
        var fagsakRelasjon1 = relasjonRepository.finnRelasjonFor(fagsak2);

        assertThat(fagsakRelasjon).isEqualTo(fagsakRelasjon1);
        assertThat(fagsakRelasjon.getFagsakNrEn()).isEqualTo(fagsak);
        assertThat(fagsakRelasjon.getFagsakNrTo()).hasValueSatisfying(fag -> assertThat(fag).isEqualTo(fagsak2));
    }

    @Test
    void skal_finne_relasjon_med_saksnummer(){
        // Arrange
        var saksnummer = new Saksnummer("1337");
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()), RelasjonsRolleType.MORA, saksnummer);
        fagsakRepository.opprettNy(fagsak);
        relasjonRepository.opprettRelasjon(fagsak);

        // Act
        var fagsakRelasjon = relasjonRepository.finnRelasjonFor(saksnummer);

        // Assert
        assertThat(fagsakRelasjon.getFagsakNrEn()).isEqualTo(fagsak);
    }

    @Test
    void skal_oppdatere_med_avslutningsdato() {
        // Arrange
        var fagsak1 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()));
        var fagsak2 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()));
        fagsakRepository.opprettNy(fagsak1);
        fagsakRepository.opprettNy(fagsak2);
        relasjonRepository.opprettRelasjon(fagsak1);
        relasjonRepository.kobleFagsaker(fagsak1, fagsak2);
        // Act
        var fagsakRelasjon = relasjonRepository.finnRelasjonFor(fagsak1);
        relasjonRepository.oppdaterMedAvsluttningsdato(fagsakRelasjon, LocalDate.now(), null, Optional.empty(), Optional.empty());
        var fagsakRelasjon1 = relasjonRepository.finnRelasjonFor(fagsak1);
        var fagsakRelasjon2 = relasjonRepository.finnRelasjonFor(fagsak2);
        // Assert
        assertThat(fagsakRelasjon1).isEqualTo(fagsakRelasjon2);
        assertThat(fagsakRelasjon1.getAvsluttningsdato()).isNotNull();
        assertThat(fagsakRelasjon2.getAvsluttningsdato()).isNotNull();
        assertThat(fagsakRelasjon1.getAvsluttningsdato()).isEqualTo(LocalDate.now());
        assertThat(fagsakRelasjon2.getAvsluttningsdato()).isEqualTo(LocalDate.now());
    }

    @Test
    void skal_hente_fagsakrel_aktiv_på_tidspunkt() {
        var bruker = NavBruker.opprettNyNB(AktørId.dummy());
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, bruker);
        fagsakRepository.opprettNy(fagsak);
        var rel1 = new FagsakRelasjon(fagsak, null, null, null, null);
        rel1.setOpprettetTidspunkt(LocalDate.of(2023, 1, 1).atStartOfDay());
        entityManager.persist(rel1);

        var rel2 = new FagsakRelasjon(fagsak, null, null, null, null);
        rel2.setOpprettetTidspunkt(LocalDate.of(2024, 1, 1).atStartOfDay());
        entityManager.persist(rel2);

        var rel3 = new FagsakRelasjon(fagsak, null, null, null, null);
        rel3.setOpprettetTidspunkt(LocalDate.of(2025, 1, 1).atStartOfDay());
        entityManager.persist(rel3);

        entityManager.flush();

        assertThat(relasjonRepository.finnRelasjonForHvisEksisterer(fagsak.getId(), LocalDate.of(2020, 1, 1).atStartOfDay())).isEmpty();
        assertThat(relasjonRepository.finnRelasjonForHvisEksisterer(fagsak.getId(), LocalDate.of(2023, 1, 1).atStartOfDay()).orElseThrow().getId()).isEqualTo(rel1.getId());
        assertThat(relasjonRepository.finnRelasjonForHvisEksisterer(fagsak.getId(), LocalDate.of(2024, 6, 6).atStartOfDay()).orElseThrow().getId()).isEqualTo(rel2.getId());
        assertThat(relasjonRepository.finnRelasjonForHvisEksisterer(fagsak.getId(), LocalDate.of(2025, 6, 6).atStartOfDay()).orElseThrow().getId()).isEqualTo(rel3.getId());
    }
}

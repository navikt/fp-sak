package no.nav.foreldrepenger.behandlingslager.behandling.verge;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class VergeRepositoryTest extends EntityManagerAwareTest {

    private static final LocalDate GYLDIG_FOM = LocalDate.now().minusYears(1);
    private static final LocalDate GYLDIG_TOM = LocalDate.now().plusYears(3);
    private static final String ORGANISASJONSNUMMER = "987654321";
    private static final String VERGE_ORGNAVN = "Advokat Advokatsen";

    private VergeRepository vergeRepository;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        vergeRepository = new VergeRepository(entityManager, new BehandlingLåsRepository(entityManager));
        fagsakRepository = new FagsakRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
    }

    @Test
    public void skal_lagre_og_hente_ut_vergeinformasjon() {
        // Arrange
        NavBruker bruker = opprettBruker();
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, bruker);
        fagsakRepository.opprettNy(fagsak);
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        VergeOrganisasjonEntitet vergeOrganisasjon = new VergeOrganisasjonBuilder()
                .medNavn(VERGE_ORGNAVN)
                .medOrganisasjonsnummer(ORGANISASJONSNUMMER)
                .build();
        VergeBuilder vergeBuilder = new VergeBuilder()
                .medVergeType(VergeType.BARN)
                .gyldigPeriode(GYLDIG_FOM, GYLDIG_TOM)
                .medVergeOrganisasjon(vergeOrganisasjon);

        // Act
        vergeRepository.lagreOgFlush(behandling.getId(), vergeBuilder);

        // Assert
        Optional<VergeAggregat> vergeAggregat = vergeRepository.hentAggregat(behandling.getId());
        assertThat(vergeAggregat).isPresent();
        assertThat(vergeAggregat.get().getVerge()).isPresent();
        VergeEntitet verge = vergeAggregat.get().getVerge().get();
        assertThat(verge.getGyldigFom()).isEqualTo(GYLDIG_FOM);
        assertThat(verge.getGyldigTom()).isEqualTo(GYLDIG_TOM);
        assertThat(verge.getVergeType()).isEqualTo(VergeType.BARN);
        assertThat(verge.getVergeOrganisasjon()).isPresent();
        VergeOrganisasjonEntitet vergeOrg = verge.getVergeOrganisasjon().get();
        assertThat(vergeOrg.getVerge()).isEqualTo(verge);
        assertThat(vergeOrg.getOrganisasjonsnummer()).isEqualTo(ORGANISASJONSNUMMER);
        assertThat(vergeOrg.getNavn()).isEqualTo(VERGE_ORGNAVN);
    }

    @Test
    public void skal_kopiere_vergegrunnlag_fra_tidligere_behandling() {
        // Arrange
        NavBruker bruker = opprettBruker();
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, bruker);
        fagsakRepository.opprettNy(fagsak);
        Behandling gammelBehandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(gammelBehandling, behandlingRepository.taSkriveLås(gammelBehandling));
        Behandling nyBehandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.REVURDERING).build();
        behandlingRepository.lagre(nyBehandling, behandlingRepository.taSkriveLås(nyBehandling));

        VergeOrganisasjonEntitet vergeOrganisasjon = new VergeOrganisasjonBuilder()
                .medNavn(VERGE_ORGNAVN)
                .medOrganisasjonsnummer(ORGANISASJONSNUMMER)
                .build();
        VergeBuilder vergeBuilder = new VergeBuilder()
                .medVergeType(VergeType.BARN)
                .gyldigPeriode(GYLDIG_FOM, GYLDIG_TOM)
                .medVergeOrganisasjon(vergeOrganisasjon);
        vergeRepository.lagreOgFlush(gammelBehandling.getId(), vergeBuilder);

        // Act
        vergeRepository.kopierGrunnlagFraEksisterendeBehandling(gammelBehandling.getId(), nyBehandling.getId());

        // Assert
        Optional<VergeAggregat> vergeAggregat = vergeRepository.hentAggregat(nyBehandling.getId());
        assertThat(vergeAggregat).isPresent();
        assertThat(vergeAggregat.get().getVerge()).isPresent();
        VergeEntitet verge = vergeAggregat.get().getVerge().get();
        assertThat(verge).isEqualTo(vergeRepository.hentAggregat(gammelBehandling.getId()).get().getVerge().get());
        assertThat(verge.getVergeOrganisasjon()).isPresent();
    }

    private NavBruker opprettBruker() {
        NavBruker navBruker = NavBruker.opprettNyNB(AktørId.dummy());
        new NavBrukerRepository(getEntityManager()).lagre(navBruker);
        return navBruker;
    }
}

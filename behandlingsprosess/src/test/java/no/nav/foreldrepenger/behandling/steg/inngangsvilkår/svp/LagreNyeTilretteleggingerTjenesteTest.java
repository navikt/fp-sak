package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.svp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;

class LagreNyeTilretteleggingerTjenesteTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;

    private SvangerskapspengerRepository svangerskapspengerRepository;

    private NyeTilretteleggingerTjeneste utledNyeTilretteleggingerTjeneste;

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        svangerskapspengerRepository = new SvangerskapspengerRepository(getEntityManager());
        utledNyeTilretteleggingerTjeneste = new NyeTilretteleggingerTjeneste(svangerskapspengerRepository, null);
    }

    @Test
    void skal_lagre_nye_tilrettelegginger_og_beholde_de_opprinnelige() {
        // Arrange
        var behandling = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger().lagre(repositoryProvider);

        var gammeltGrunnlag = new SvpGrunnlagEntitet.Builder()
            .medOpprinneligeTilrettelegginger(List.of(
            new SvpTilretteleggingEntitet.Builder()
                .medBehovForTilretteleggingFom(LocalDate.of(2019, 8, 1))
                .medMottattTidspunkt(LocalDateTime.of(LocalDate.of(2019, 4, 1), LocalTime.MIDNIGHT))
                .medKopiertFraTidligereBehandling(false)
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .build())).medOverstyrteTilrettelegginger(List.of()).medBehandlingId(behandling.getId()).build();
        svangerskapspengerRepository.lagreOgFlush(gammeltGrunnlag);

        var overstyrtTilrettelegging = new SvpTilretteleggingEntitet.Builder()
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medBehovForTilretteleggingFom(LocalDate.of(2019, 8, 1))
            .medMottattTidspunkt(LocalDateTime.of(LocalDate.of(2019, 4, 1), LocalTime.MIDNIGHT))
            .medKopiertFraTidligereBehandling(false)
            .build();

        // Act
        utledNyeTilretteleggingerTjeneste.lagre(behandling, List.of(overstyrtTilrettelegging));

        // Assert
        var nyttGrunnlag = svangerskapspengerRepository.hentGrunnlag(behandling.getId()).orElseThrow();
        assertThat(nyttGrunnlag.getOpprinneligeTilrettelegginger()).isNotNull();
        assertThat(nyttGrunnlag.getOpprinneligeTilrettelegginger().getTilretteleggingListe()).hasSize(1);
        assertThat(nyttGrunnlag.getOverstyrteTilrettelegginger()).isNotNull();
        assertThat(nyttGrunnlag.getOverstyrteTilrettelegginger().getTilretteleggingListe()).hasSize(1);

    }
}

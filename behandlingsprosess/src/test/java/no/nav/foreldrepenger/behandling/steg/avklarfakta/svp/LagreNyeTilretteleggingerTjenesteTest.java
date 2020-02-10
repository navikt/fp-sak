package no.nav.foreldrepenger.behandling.steg.avklarfakta.svp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;

public class LagreNyeTilretteleggingerTjenesteTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    private SvangerskapspengerRepository svangerskapspengerRepository = repositoryProvider.getSvangerskapspengerRepository();

    private LagreNyeTilretteleggingerTjeneste lagreNyeTilretteleggingerTjeneste =
        new LagreNyeTilretteleggingerTjeneste(svangerskapspengerRepository);

    @Test
    public void skal_lagre_nye_tilrettelegginger_og_beholde_de_opprinnelige_unleash_enabled(){

        // Arrange
        var behandling = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger().lagre(repositoryProvider);

        var gammeltGrunnlag = new SvpGrunnlagEntitet.Builder()
            .medOpprinneligeTilrettelegginger(List.of(new SvpTilretteleggingEntitet.Builder()
                .medBehovForTilretteleggingFom(LocalDate.of(2019,8,1))
                .medMottattTidspunkt(LocalDateTime.of(LocalDate.of(2019, 4, 1), LocalTime.MIDNIGHT))
                .medKopiertFraTidligereBehandling(false)
                .build()))
            .medOverstyrteTilrettelegginger(List.of())
            .medBehandlingId(behandling.getId()).build();
        svangerskapspengerRepository.lagreOgFlush(gammeltGrunnlag);

        var overstyrtTilrettelegging = new SvpTilretteleggingEntitet.Builder()
            .medBehovForTilretteleggingFom(LocalDate.of(2019,8,1))
            .medMottattTidspunkt(LocalDateTime.of(LocalDate.of(2019, 4, 1), LocalTime.MIDNIGHT))
            .medKopiertFraTidligereBehandling(false)
            .build();

        // Act
        lagreNyeTilretteleggingerTjeneste.lagre(behandling, List.of(overstyrtTilrettelegging));

        // Assert
        var nyttGrunnlag = svangerskapspengerRepository.hentGrunnlag(behandling.getId()).orElseThrow();
        assertThat(nyttGrunnlag.getOpprinneligeTilrettelegginger()).isNotNull();
        assertThat(nyttGrunnlag.getOpprinneligeTilrettelegginger().getTilretteleggingListe()).hasSize(1);
        assertThat(nyttGrunnlag.getOverstyrteTilrettelegginger()).isNotNull();
        assertThat(nyttGrunnlag.getOverstyrteTilrettelegginger().getTilretteleggingListe()).hasSize(1);

    }

}

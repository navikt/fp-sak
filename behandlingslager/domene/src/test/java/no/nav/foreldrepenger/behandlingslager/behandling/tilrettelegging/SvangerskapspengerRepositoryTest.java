package no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;

public class SvangerskapspengerRepositoryTest extends EntityManagerAwareTest {

    private static final LocalDateTime I_GÅR = LocalDateTime.now().minusDays(1);
    private static final LocalDate OM_TO_DAGER = LocalDate.now().plusDays(2);
    private static final LocalDate OM_TRE_DAGER = LocalDate.now().plusDays(3);

    private BasicBehandlingBuilder basicBehandlingBuilder;
    private SvangerskapspengerRepository repository;

    @BeforeEach
    void before() {
        var entityManager = getEntityManager();
        basicBehandlingBuilder = new BasicBehandlingBuilder(entityManager);
        repository = new SvangerskapspengerRepository(entityManager);
    }

    @Test
    public void skal_kopiere_grunnlaget_og_sette_kopiert_flagget() {
        // Arrange
        Fagsak fagsak = basicBehandlingBuilder.opprettFagsak(FagsakYtelseType.SVANGERSKAPSPENGER);
        Behandling gammelBehandling = basicBehandlingBuilder.opprettOgLagreFørstegangssøknad(fagsak);
        Behandling nyBehandling = basicBehandlingBuilder.opprettOgLagreFørstegangssøknad(fagsak);

        SvpTilretteleggingEntitet opprTilrettelegging = opprettTilrettelegging(OM_TO_DAGER);
        SvpTilretteleggingEntitet ovstTilrettelegging = opprettTilrettelegging(OM_TRE_DAGER);

        SvpGrunnlagEntitet svpGrunnlag = new SvpGrunnlagEntitet.Builder()
            .medBehandlingId(gammelBehandling.getId())
            .medOpprinneligeTilrettelegginger(List.of(opprTilrettelegging))
            .medOverstyrteTilrettelegginger(List.of(ovstTilrettelegging))
            .build();
        repository.lagreOgFlush(svpGrunnlag);

        // Act
        repository.kopierSvpGrunnlagFraEksisterendeBehandling(gammelBehandling.getId(), nyBehandling);

        // Assert
        Optional<SvpGrunnlagEntitet> kopiertGrunnlag = repository.hentGrunnlag(nyBehandling.getId());
        assertThat(kopiertGrunnlag).isPresent();
        assertThat(kopiertGrunnlag.get().getOpprinneligeTilrettelegginger().getTilretteleggingListe()).hasSize(1);
        assertThat(kopiertGrunnlag.get().getOpprinneligeTilrettelegginger().getTilretteleggingListe().get(0).getKopiertFraTidligereBehandling()).isTrue();
        assertThat(kopiertGrunnlag.get().getOpprinneligeTilrettelegginger().getTilretteleggingListe().get(0).getMottattTidspunkt()).isEqualTo(I_GÅR);
        assertThat(kopiertGrunnlag.get().getOpprinneligeTilrettelegginger().getTilretteleggingListe().get(0).getBehovForTilretteleggingFom()).isEqualTo(OM_TO_DAGER);
        assertThat(kopiertGrunnlag.get().getOverstyrteTilrettelegginger().getTilretteleggingListe().get(0).getKopiertFraTidligereBehandling()).isTrue();
        assertThat(kopiertGrunnlag.get().getOverstyrteTilrettelegginger().getTilretteleggingListe().get(0).getMottattTidspunkt()).isEqualTo(I_GÅR);
        assertThat(kopiertGrunnlag.get().getOverstyrteTilrettelegginger().getTilretteleggingListe().get(0).getBehovForTilretteleggingFom()).isEqualTo(OM_TRE_DAGER);
    }

    private SvpTilretteleggingEntitet opprettTilrettelegging(LocalDate fom) {
        return new SvpTilretteleggingEntitet.Builder()
            .medKopiertFraTidligereBehandling(false)
            .medMottattTidspunkt(I_GÅR)
            .medBehovForTilretteleggingFom(fom)
            .build();
    }
}

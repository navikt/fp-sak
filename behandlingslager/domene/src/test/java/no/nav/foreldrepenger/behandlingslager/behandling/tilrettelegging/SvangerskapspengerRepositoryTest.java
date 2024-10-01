package no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;

class SvangerskapspengerRepositoryTest extends EntityManagerAwareTest {

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
    void skal_kopiere_grunnlaget_og_sette_kopiert_flagget() {
        // Arrange
        var fagsak = basicBehandlingBuilder.opprettFagsak(FagsakYtelseType.SVANGERSKAPSPENGER);
        var gammelBehandling = basicBehandlingBuilder.opprettOgLagreFørstegangssøknad(fagsak);
        var nyBehandling = basicBehandlingBuilder.opprettOgLagreFørstegangssøknad(fagsak);

        var opprTilrettelegging = opprettTilrettelegging(OM_TO_DAGER);
        var ovstTilrettelegging = opprettTilrettelegging(OM_TRE_DAGER);

        var svpGrunnlag = new SvpGrunnlagEntitet.Builder()
            .medBehandlingId(gammelBehandling.getId())
            .medOpprinneligeTilrettelegginger(List.of(opprTilrettelegging))
            .medOverstyrteTilrettelegginger(List.of(ovstTilrettelegging))
            .build();
        repository.lagreOgFlush(svpGrunnlag);

        // Act
        repository.kopierSvpGrunnlagFraEksisterendeBehandling(gammelBehandling.getId(), nyBehandling);

        // Assert
        var kopiertGrunnlag = repository.hentGrunnlag(nyBehandling.getId());
        assertThat(kopiertGrunnlag).isPresent();
        assertThat(kopiertGrunnlag.get().getOpprinneligeTilrettelegginger().getTilretteleggingListe()).hasSize(1);
        assertThat(kopiertGrunnlag.get().getOpprinneligeTilrettelegginger().getTilretteleggingListe().get(0).getKopiertFraTidligereBehandling()).isTrue();
        assertThat(kopiertGrunnlag.get().getOpprinneligeTilrettelegginger().getTilretteleggingListe().get(0).getMottattTidspunkt()).isEqualTo(I_GÅR);
        assertThat(kopiertGrunnlag.get().getOpprinneligeTilrettelegginger().getTilretteleggingListe().get(0).getBehovForTilretteleggingFom()).isEqualTo(OM_TRE_DAGER);
        assertThat(kopiertGrunnlag.get().getOpprinneligeTilrettelegginger().getTilretteleggingListe().get(0).getTilretteleggingFOMListe().get(0).getKilde()).isEqualTo(SvpTilretteleggingFomKilde.TIDLIGERE_VEDTAK);
        assertThat(kopiertGrunnlag.get().getOverstyrteTilrettelegginger()).isNull();
    }

    private SvpTilretteleggingEntitet opprettTilrettelegging(LocalDate fom) {
        return new SvpTilretteleggingEntitet.Builder()
            .medKopiertFraTidligereBehandling(false)
            .medMottattTidspunkt(I_GÅR)
            .medBehovForTilretteleggingFom(fom)
            .medTilretteleggingFom(new TilretteleggingFOM.Builder()
                .medFomDato(LocalDate.now())
                .medTilretteleggingType(TilretteleggingType.INGEN_TILRETTELEGGING)
                .build())
            .medAvklartOpphold(SvpAvklartOpphold.Builder.nytt()
                .medOppholdPeriode(fom, fom.plusWeeks(1))
                .medOppholdÅrsak(SvpOppholdÅrsak.SYKEPENGER)
                .medKilde(SvpOppholdKilde.REGISTRERT_AV_SAKSBEHANDLER)
                .build())
            .build();
    }
}

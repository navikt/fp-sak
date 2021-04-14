package no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class TilretteleggingFilterTest {

    private SvpGrunnlagEntitet grunnlag = Mockito.mock(SvpGrunnlagEntitet.class);

    @Test
    public void skal_finne_opprinnelige_tilrettelegginger() {

        // Arrange
        var tilrettelegginger = new SvpTilretteleggingerEntitet.Builder()
                .medTilretteleggingListe(List.of(new SvpTilretteleggingEntitet.Builder().build()))
                .build();

        when(grunnlag.getOpprinneligeTilrettelegginger()).thenReturn(tilrettelegginger);
        when(grunnlag.getOverstyrteTilrettelegginger()).thenReturn(null);

        var tilretteleggingFilter = new TilretteleggingFilter(grunnlag);

        // Act
        var aktuelleTilretteleggingerUfiltrert = tilretteleggingFilter.getAktuelleTilretteleggingerUfiltrert();

        // Assert
        assertThat(aktuelleTilretteleggingerUfiltrert).hasSize(1);

    }

    @Test
    public void skal_finne_overstyrte_tilrettelegginger() {

        // Arrange
        var opprinnelig = new SvpTilretteleggingerEntitet.Builder()
                .medTilretteleggingListe(List.of(new SvpTilretteleggingEntitet.Builder().build()))
                .build();

        var overstyrt = new SvpTilretteleggingerEntitet.Builder()
                .medTilretteleggingListe(List.of(new SvpTilretteleggingEntitet.Builder()
                        .medSkalBrukes(true)
                        .build()))
                .build();

        when(grunnlag.getOpprinneligeTilrettelegginger()).thenReturn(opprinnelig);
        when(grunnlag.getOverstyrteTilrettelegginger()).thenReturn(overstyrt);

        var tilretteleggingFilter = new TilretteleggingFilter(grunnlag);

        // Act
        var aktuelleTilretteleggingerUfiltrert = tilretteleggingFilter.getAktuelleTilretteleggingerUfiltrert();

        // Assert
        assertThat(aktuelleTilretteleggingerUfiltrert).hasSize(1);
        assertThat(aktuelleTilretteleggingerUfiltrert.get(0).getSkalBrukes()).isTrue();

    }

    @Test
    public void skal_finne_overstyrte_tilrettelegginger_og_filtrer_bort_de_som_ikke_skal_brukes() {

        // Arrange
        var opprinnelig = new SvpTilretteleggingerEntitet.Builder()
                .medTilretteleggingListe(List.of(new SvpTilretteleggingEntitet.Builder().build()))
                .build();

        var tilrettelegging_1 = new SvpTilretteleggingEntitet.Builder()
                .medSkalBrukes(true)
                .build();
        var tilrettelegging_2 = new SvpTilretteleggingEntitet.Builder()
                .medSkalBrukes(false)
                .build();
        var tilrettelegging_3 = new SvpTilretteleggingEntitet.Builder()
                .medSkalBrukes(true)
                .build();
        var overstyrt = new SvpTilretteleggingerEntitet.Builder()
                .medTilretteleggingListe(List.of(tilrettelegging_1, tilrettelegging_2, tilrettelegging_3))
                .build();

        when(grunnlag.getOpprinneligeTilrettelegginger()).thenReturn(opprinnelig);
        when(grunnlag.getOverstyrteTilrettelegginger()).thenReturn(overstyrt);

        var tilretteleggingFilter = new TilretteleggingFilter(grunnlag);

        // Act
        var aktuelleTilretteleggingerFiltrert = tilretteleggingFilter.getAktuelleTilretteleggingerFiltrert();

        // Assert
        assertThat(aktuelleTilretteleggingerFiltrert).hasSize(2);
        assertThat(aktuelleTilretteleggingerFiltrert.get(0).getSkalBrukes()).isTrue();
        assertThat(aktuelleTilretteleggingerFiltrert.get(1).getSkalBrukes()).isTrue();

    }

    @Test
    public void skal_finne_finne_første_tilretteleggingsdato_for_tilrettelegging_som_skal_brukes() {

        // Arrange
        var opprinnelig = new SvpTilretteleggingerEntitet.Builder()
                .medTilretteleggingListe(List.of(new SvpTilretteleggingEntitet.Builder().build()))
                .build();

        var tilrettelegging_1 = new SvpTilretteleggingEntitet.Builder()
                .medSkalBrukes(false)
                .medBehovForTilretteleggingFom(LocalDate.of(2019, 8, 1))
                .build();
        var tilrettelegging_2 = new SvpTilretteleggingEntitet.Builder()
                .medBehovForTilretteleggingFom(LocalDate.of(2019, 8, 7))
                .medSkalBrukes(true)
                .build();
        var tilrettelegging_3 = new SvpTilretteleggingEntitet.Builder()
                .medSkalBrukes(true)
                .medBehovForTilretteleggingFom(LocalDate.of(2019, 8, 14))
                .build();
        var overstyrt = new SvpTilretteleggingerEntitet.Builder()
                .medTilretteleggingListe(List.of(tilrettelegging_1, tilrettelegging_2, tilrettelegging_3))
                .build();

        when(grunnlag.getOpprinneligeTilrettelegginger()).thenReturn(opprinnelig);
        when(grunnlag.getOverstyrteTilrettelegginger()).thenReturn(overstyrt);

        var tilretteleggingFilter = new TilretteleggingFilter(grunnlag);

        // Act
        var datoOpt = tilretteleggingFilter.getFørsteTilretteleggingsbehovdatoFiltrert();

        // Assert
        assertThat(datoOpt).hasValueSatisfying(dato -> assertThat(dato).isEqualTo(LocalDate.of(2019, 8, 7)));

    }

}

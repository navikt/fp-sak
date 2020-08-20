package no.nav.foreldrepenger.domene.arbeidsforhold.svp;

import static no.nav.foreldrepenger.domene.arbeidsforhold.svp.FinnAktivitetsavtalerForUtbetalingsgrad.finnAktivitetsavtalerSomSkalBrukes;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

public class FinnAktivitetsavtalerForUtbetalingsgradTest {

    @Test
    public void skal_returnere_ingen_avtaler_ved_ingen_avtaler() {
        // Arrange
        Collection<AktivitetsAvtale> avtaler = List.of();
        LocalDate jordmorsdato = LocalDate.now();
        LocalDate termindato = jordmorsdato.plusDays(15);

        // Act
        List<AktivitetsAvtale> aktivitetsAvtales = finnAktivitetsavtalerSomSkalBrukes(avtaler, jordmorsdato, termindato);

        // Assert
        assertThat(aktivitetsAvtales.size()).isEqualTo(0);
    }

    @Test
    public void skal_returnere_ingen_avtaler_ved_ingen_overlapp() {
        // Arrange
        LocalDate jordmorsdato = LocalDate.now();
        LocalDate termindato = jordmorsdato.plusDays(15);
        Collection<AktivitetsAvtale> avtaler = List.of(AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(termindato.plusDays(1), termindato.plusDays(10)))
        .medProsentsats(BigDecimal.valueOf(100))
        .build());

        // Act
        List<AktivitetsAvtale> aktivitetsAvtales = finnAktivitetsavtalerSomSkalBrukes(avtaler, jordmorsdato, termindato);

        // Assert
        assertThat(aktivitetsAvtales.size()).isEqualTo(0);
    }

    @Test
    public void skal_returnere_en_avtale_ved_overlapp_siste_dag() {
        // Arrange
        LocalDate jordmorsdato = LocalDate.now();
        LocalDate termindato = jordmorsdato.plusDays(15);
        Collection<AktivitetsAvtale> avtaler = List.of(AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(termindato, termindato.plusDays(10)))
            .medProsentsats(BigDecimal.valueOf(100))
            .build());

        // Act
        List<AktivitetsAvtale> aktivitetsAvtales = finnAktivitetsavtalerSomSkalBrukes(avtaler, jordmorsdato, termindato);

        // Assert
        assertThat(aktivitetsAvtales.size()).isEqualTo(1);
    }

    @Test
    public void skal_returnere_en_avtale_ved_overlapp_første_dag() {
        // Arrange
        LocalDate jordmorsdato = LocalDate.now();
        LocalDate termindato = jordmorsdato.plusDays(15);
        Collection<AktivitetsAvtale> avtaler = List.of(AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, jordmorsdato.plusDays(10)))
            .medProsentsats(BigDecimal.valueOf(100))
            .build());

        // Act
        List<AktivitetsAvtale> aktivitetsAvtales = finnAktivitetsavtalerSomSkalBrukes(avtaler, jordmorsdato, termindato);

        // Assert
        assertThat(aktivitetsAvtales.size()).isEqualTo(1);
    }

    @Test
    public void skal_returnere_en_avtale_ved_2_avtaler_overlapp_jordmorsdato_og_termindato() {
        // Arrange
        LocalDate jordmorsdato = LocalDate.now();
        LocalDate termindato = jordmorsdato.plusDays(15);
        Collection<AktivitetsAvtale> avtaler = List.of(AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, jordmorsdato.plusDays(10)))
            .medProsentsats(BigDecimal.valueOf(100))
            .build(),
            AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(termindato, termindato.plusDays(10)))
                .medProsentsats(BigDecimal.valueOf(100))
                .build()
            );

        // Act
        List<AktivitetsAvtale> aktivitetsAvtales = finnAktivitetsavtalerSomSkalBrukes(avtaler, jordmorsdato, termindato);

        // Assert
        assertThat(aktivitetsAvtales.size()).isEqualTo(1);
        assertThat(aktivitetsAvtales.get(0).getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, jordmorsdato.plusDays(10)));
    }

    @Test
    public void skal_returnere_2_avtaler_ved_2_avtaler_som_ligger_like_langt_fra_jordmorsdato() {
        // Arrange
        LocalDate jordmorsdato = LocalDate.now();
        LocalDate termindato = jordmorsdato.plusDays(15);
        Collection<AktivitetsAvtale> avtaler = List.of(AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato.plusDays(2), jordmorsdato.plusDays(10)))
                .medProsentsats(BigDecimal.valueOf(100))
                .build(),
            AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato.plusDays(2), termindato.plusDays(10)))
                .medProsentsats(BigDecimal.valueOf(100))
                .build()
        );

        // Act
        List<AktivitetsAvtale> aktivitetsAvtales = finnAktivitetsavtalerSomSkalBrukes(avtaler, jordmorsdato, termindato);

        // Assert
        assertThat(aktivitetsAvtales.size()).isEqualTo(2);
    }

    @Test
    public void skal_returnere_velge_avtaler_etter_jordmorsdato_foran_avtaler_før() {
        // Arrange
        LocalDate jordmorsdato = LocalDate.now();
        LocalDate termindato = jordmorsdato.plusDays(15);
        Collection<AktivitetsAvtale> avtaler = List.of(AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato.plusDays(2), jordmorsdato.plusDays(10)))
                .medProsentsats(BigDecimal.valueOf(100))
                .build(),
            AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato.plusDays(2), termindato.plusDays(10)))
                .medProsentsats(BigDecimal.valueOf(100))
                .build(),
            AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato.minusDays(10), jordmorsdato.minusDays(4)))
                .medProsentsats(BigDecimal.valueOf(100))
                .build(),
            AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato.minusDays(11), jordmorsdato.minusDays(4)))
                .medProsentsats(BigDecimal.valueOf(100))
                .build()
        );

        // Act
        List<AktivitetsAvtale> aktivitetsAvtales = finnAktivitetsavtalerSomSkalBrukes(avtaler, jordmorsdato, termindato);

        // Assert
        assertThat(aktivitetsAvtales.size()).isEqualTo(2);
        assertThat(aktivitetsAvtales.get(0).getPeriode().getFomDato()).isEqualTo(jordmorsdato.plusDays(2));
    }

}

package no.nav.foreldrepenger.domene.arbeidsforhold.svp;

import static no.nav.foreldrepenger.domene.arbeidsforhold.svp.FinnAktivitetsavtalerForUtbetalingsgrad.finnAktivitetsavtalerSomSkalBrukes;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

class FinnAktivitetsavtalerForUtbetalingsgradTest {

    @Test
    void skal_returnere_ingen_avtaler_ved_ingen_avtaler() {
        // Arrange
        Collection<AktivitetsAvtale> avtaler = List.of();
        var jordmorsdato = LocalDate.now();
        var termindato = jordmorsdato.plusDays(15);

        // Act
        var aktivitetsAvtales = finnAktivitetsavtalerSomSkalBrukes(avtaler, jordmorsdato, termindato);

        // Assert
        assertThat(aktivitetsAvtales).hasSize(0);
    }

    @Test
    void skal_returnere_ingen_avtaler_ved_ingen_overlapp() {
        // Arrange
        var jordmorsdato = LocalDate.now();
        var termindato = jordmorsdato.plusDays(15);
        Collection<AktivitetsAvtale> avtaler = List.of(AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(termindato.plusDays(1), termindato.plusDays(10)))
                .medProsentsats(BigDecimal.valueOf(100))
                .build());

        // Act
        var aktivitetsAvtales = finnAktivitetsavtalerSomSkalBrukes(avtaler, jordmorsdato, termindato);

        // Assert
        assertThat(aktivitetsAvtales).hasSize(0);
    }

    @Test
    void skal_returnere_en_avtale_ved_overlapp_siste_dag() {
        // Arrange
        var jordmorsdato = LocalDate.now();
        var termindato = jordmorsdato.plusDays(15);
        Collection<AktivitetsAvtale> avtaler = List.of(AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(termindato, termindato.plusDays(10)))
                .medProsentsats(BigDecimal.valueOf(100))
                .build());

        // Act
        var aktivitetsAvtales = finnAktivitetsavtalerSomSkalBrukes(avtaler, jordmorsdato, termindato);

        // Assert
        assertThat(aktivitetsAvtales).hasSize(1);
    }

    @Test
    void skal_returnere_en_avtale_ved_overlapp_første_dag() {
        // Arrange
        var jordmorsdato = LocalDate.now();
        var termindato = jordmorsdato.plusDays(15);
        Collection<AktivitetsAvtale> avtaler = List.of(AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, jordmorsdato.plusDays(10)))
                .medProsentsats(BigDecimal.valueOf(100))
                .build());

        // Act
        var aktivitetsAvtales = finnAktivitetsavtalerSomSkalBrukes(avtaler, jordmorsdato, termindato);

        // Assert
        assertThat(aktivitetsAvtales).hasSize(1);
    }

    @Test
    void skal_returnere_en_avtale_ved_2_avtaler_overlapp_jordmorsdato_og_termindato() {
        // Arrange
        var jordmorsdato = LocalDate.now();
        var termindato = jordmorsdato.plusDays(15);
        Collection<AktivitetsAvtale> avtaler = List.of(AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, jordmorsdato.plusDays(10)))
                .medProsentsats(BigDecimal.valueOf(100))
                .build(),
                AktivitetsAvtaleBuilder.ny()
                        .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(termindato, termindato.plusDays(10)))
                        .medProsentsats(BigDecimal.valueOf(100))
                        .build());

        // Act
        var aktivitetsAvtales = finnAktivitetsavtalerSomSkalBrukes(avtaler, jordmorsdato, termindato);

        // Assert
        assertThat(aktivitetsAvtales).hasSize(1);
        assertThat(aktivitetsAvtales.get(0).getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, jordmorsdato.plusDays(10)));
    }

    @Test
    void skal_returnere_2_avtaler_ved_2_avtaler_som_ligger_like_langt_fra_jordmorsdato() {
        // Arrange
        var jordmorsdato = LocalDate.now();
        var termindato = jordmorsdato.plusDays(15);
        Collection<AktivitetsAvtale> avtaler = List.of(AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato.plusDays(2), jordmorsdato.plusDays(10)))
                .medProsentsats(BigDecimal.valueOf(100))
                .build(),
                AktivitetsAvtaleBuilder.ny()
                        .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato.plusDays(2), termindato.plusDays(10)))
                        .medProsentsats(BigDecimal.valueOf(100))
                        .build());

        // Act
        var aktivitetsAvtales = finnAktivitetsavtalerSomSkalBrukes(avtaler, jordmorsdato, termindato);

        // Assert
        assertThat(aktivitetsAvtales).hasSize(2);
    }

    @Test
    void skal_returnere_velge_avtaler_etter_jordmorsdato_foran_avtaler_før() {
        // Arrange
        var jordmorsdato = LocalDate.now();
        var termindato = jordmorsdato.plusDays(15);
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
                        .build());

        // Act
        var aktivitetsAvtales = finnAktivitetsavtalerSomSkalBrukes(avtaler, jordmorsdato, termindato);

        // Assert
        assertThat(aktivitetsAvtales).hasSize(2);
        assertThat(aktivitetsAvtales.get(0).getPeriode().getFomDato()).isEqualTo(jordmorsdato.plusDays(2));
    }

}

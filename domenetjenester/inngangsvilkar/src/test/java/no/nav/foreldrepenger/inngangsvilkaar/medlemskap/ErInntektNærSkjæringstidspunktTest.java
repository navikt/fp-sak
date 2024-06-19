package no.nav.foreldrepenger.inngangsvilkaar.medlemskap;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.Inntekt;
import no.nav.foreldrepenger.domene.iay.modell.InntektBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektspostBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;

class ErInntektNærSkjæringstidspunktTest {

    private static final Arbeidsgiver VIRKSOMHET = Arbeidsgiver.virksomhet("987654321");

    @Test
    void skjæringstidspunkt_og_behandlingstidspunkt_01_i_samme_måned_inntekt_for_måneden_før() {
        // Arrange
        var skjæringstidspunkt = LocalDate.of(2019, Month.JANUARY, 15);
        var behandlingstidspunkt = LocalDate.of(2019, Month.JANUARY, 1);
        var inntekt = lagInntekt(LocalDate.of(2018, Month.DECEMBER, 1));

        // Act
        var resultat = ErInntektNærSkjæringstidspunkt.erNær(inntekt.getAlleInntektsposter(), skjæringstidspunkt, behandlingstidspunkt);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    void skjæringstidspunkt_og_behandlingstidspunkt_31_i_samme_måned_inntekt_for_måneden_før() {
        // Arrange
        var skjæringstidspunkt = LocalDate.of(2019, Month.JANUARY, 15);
        var behandlingstidspunkt = LocalDate.of(2019, Month.JANUARY, 31);
        var inntekt = lagInntekt(LocalDate.of(2018, Month.DECEMBER, 1));

        // Act
        var resultat = ErInntektNærSkjæringstidspunkt.erNær(inntekt.getAlleInntektsposter(), skjæringstidspunkt, behandlingstidspunkt);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    void skjæringstidspunkt_og_behandlingstidspunkt_01_i_samme_måned_inntekt_to_månedener_før() {
        // Arrange
        var skjæringstidspunkt = LocalDate.of(2019, Month.JANUARY, 15);
        var behandlingstidspunkt = LocalDate.of(2019, Month.JANUARY, 1);
        var inntekt = lagInntekt(LocalDate.of(2018, Month.NOVEMBER, 1));

        // Act
        var resultat = ErInntektNærSkjæringstidspunkt.erNær(inntekt.getAlleInntektsposter(), skjæringstidspunkt, behandlingstidspunkt);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skjæringstidspunkt_og_behandlingstidspunkt_31_i_samme_måned_inntekt_to_månedener_før() {
        // Arrange
        var skjæringstidspunkt = LocalDate.of(2019, Month.JANUARY, 15);
        var behandlingstidspunkt = LocalDate.of(2019, Month.JANUARY, 31);
        var inntekt = lagInntekt(LocalDate.of(2018, Month.NOVEMBER, 1));

        // Act
        var resultat = ErInntektNærSkjæringstidspunkt.erNær(inntekt.getAlleInntektsposter(), skjæringstidspunkt, behandlingstidspunkt);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void behandlingstidspunkt_første_dag_i_måneden_etter_stp_og_inntekt_i_måneden_med_stp() {
        // Arrange
        var skjæringstidspunkt = LocalDate.of(2019, Month.JANUARY, 15);
        var behandlingstidspunkt = LocalDate.of(2019, Month.FEBRUARY, 1);
        var inntekt = lagInntekt(LocalDate.of(2019, Month.JANUARY, 1));

        // Act
        var resultat = ErInntektNærSkjæringstidspunkt.erNær(inntekt.getAlleInntektsposter(), skjæringstidspunkt, behandlingstidspunkt);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    void behandlingstidspunkt_siste_dag_i_måneden_etter_inntekt_i_måneden_med_skjæringstidspunkt() {
        // Arrange
        var skjæringstidspunkt = LocalDate.of(2019, Month.JANUARY, 15);
        var behandlingstidspunkt = LocalDate.of(2019, Month.FEBRUARY, 28);
        var inntekt = lagInntekt(LocalDate.of(2019, Month.JANUARY, 1));

        // Act
        var resultat = ErInntektNærSkjæringstidspunkt.erNær(inntekt.getAlleInntektsposter(), skjæringstidspunkt, behandlingstidspunkt);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    void behandlingstidspunkt_første_dag_i_måneden_etter_inntekt_for_måneden_før_skjæringstidspunkt() {
        // Arrange
        var skjæringstidspunkt = LocalDate.of(2019, Month.JANUARY, 15);
        var behandlingstidspunkt = LocalDate.of(2019, Month.FEBRUARY, 1);
        var inntekt = lagInntekt(LocalDate.of(2018, Month.DECEMBER, 1));

        // Act
        var resultat = ErInntektNærSkjæringstidspunkt.erNær(inntekt.getAlleInntektsposter(), skjæringstidspunkt, behandlingstidspunkt);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void behandlingstidspunkt_siste_dag_i_måneden_etter_inntekt_for_måneden_før_skjæringstidspunkt() {
        // Arrange
        var skjæringstidspunkt = LocalDate.of(2019, Month.JANUARY, 15);
        var behandlingstidspunkt = LocalDate.of(2019, Month.FEBRUARY, 28);
        var inntekt = lagInntekt(LocalDate.of(2018, Month.DECEMBER, 1));

        // Act
        var resultat = ErInntektNærSkjæringstidspunkt.erNær(inntekt.getAlleInntektsposter(), skjæringstidspunkt, behandlingstidspunkt);

        // Assert
        assertThat(resultat).isFalse();
    }

    private Inntekt lagInntekt(LocalDate inntektmåned) {
        return InntektBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(VIRKSOMHET)
            .medInntektsKilde(InntektsKilde.INNTEKT_OPPTJENING)
            .leggTilInntektspost(InntektspostBuilder.ny()
                .medPeriode(inntektmåned, inntektmåned.with(TemporalAdjusters.lastDayOfMonth()))
                .medBeløp(BigDecimal.TEN)
                .medInntektspostType(InntektspostType.LØNN))
            .build();
    }
}

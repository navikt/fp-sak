package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

public class UtledStillingsprosentTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();

    @Test
    public void skal_utlede_stillingsprosent_lik_100_når_ingen_yrkesaktiviteter() {
        // Arrange
        List<Yrkesaktivitet> yrkesaktiviteter = Collections.emptyList();
        // Act
        var stillingsprosent = UtledStillingsprosent.utled(new YrkesaktivitetFilter(null, yrkesaktiviteter), yrkesaktiviteter,
                SKJÆRINGSTIDSPUNKT);
        // Assert
        assertThat(stillingsprosent).isEqualTo(BigDecimal.valueOf(100));
    }

    @Test
    public void skal_utlede_stillingsprosent_lik_75_når_en_yrkesaktivitet_overlapper_stp() {
        // Arrange
        var fom = SKJÆRINGSTIDSPUNKT.minusYears(2);
        var tom = SKJÆRINGSTIDSPUNKT.plusDays(1);
        var aktivitetsavtale = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
                .medProsentsats(BigDecimal.valueOf(75));
        var ansettelsesperiode = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        var yrkesaktivitet = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(aktivitetsavtale)
                .leggTilAktivitetsAvtale(ansettelsesperiode)
                .build();
        // Act
        var stillingsprosent = UtledStillingsprosent.utled(new YrkesaktivitetFilter(Optional.empty(), yrkesaktivitet), yrkesaktivitet,
                SKJÆRINGSTIDSPUNKT);
        // Assert
        assertThat(stillingsprosent).isEqualTo(BigDecimal.valueOf(75));
    }

    @Test
    public void skal_utlede_stillingsprosent_lik_35_når_en_yrkesaktivitet_starter_etter_stp() {
        // Arrange
        var fom = SKJÆRINGSTIDSPUNKT.plusDays(1);
        var tom = fom.plusYears(2);
        var aktivitetsavtale = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
                .medProsentsats(BigDecimal.valueOf(35));
        var ansettelsesperiode = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        var yrkesaktivitet = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(aktivitetsavtale)
                .leggTilAktivitetsAvtale(ansettelsesperiode)
                .build();
        // Act
        var stillingsprosent = UtledStillingsprosent.utled(new YrkesaktivitetFilter(Optional.empty(), yrkesaktivitet), yrkesaktivitet,
                SKJÆRINGSTIDSPUNKT);
        // Assert
        assertThat(stillingsprosent).isEqualTo(BigDecimal.valueOf(35));
    }

    @Test
    public void skal_utlede_stillingsprosent_for_yrkesaktivitet_med_seneste_fom_dato_når_flere_yrkesaktiviter_overlapper_stp() {

        // Arrange
        var fom1 = SKJÆRINGSTIDSPUNKT.minusYears(2);
        var tom1 = SKJÆRINGSTIDSPUNKT.plusDays(1);
        var aktivitetsavtale1 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1))
                .medProsentsats(BigDecimal.valueOf(10));
        var ansettelsesperiode1 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1));
        var yrkesaktivitet1 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(aktivitetsavtale1)
                .leggTilAktivitetsAvtale(ansettelsesperiode1)
                .build();

        var fom2 = SKJÆRINGSTIDSPUNKT.minusYears(1);
        var tom2 = SKJÆRINGSTIDSPUNKT.plusDays(1);
        var aktivitetsavtale2 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2))
                .medProsentsats(BigDecimal.valueOf(25));
        var ansettelsesperiode2 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2));
        var yrkesaktivitet2 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(aktivitetsavtale2)
                .leggTilAktivitetsAvtale(ansettelsesperiode2)
                .build();

        // Act
        var stillingsprosent = UtledStillingsprosent.utled(new YrkesaktivitetFilter(null, List.of(yrkesaktivitet1, yrkesaktivitet2)),
                List.of(yrkesaktivitet1, yrkesaktivitet2), SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(stillingsprosent).isEqualTo(BigDecimal.valueOf(25));

    }

    @Test
    public void skal_utlede_stillingsprosent_for_yrkesaktivitet_med_tidligst_fom_dato_når_flere_yrkesaktiviter_tilkommer_etter_stp() {

        // Arrange
        var fom1 = SKJÆRINGSTIDSPUNKT.plusDays(1);
        var tom1 = fom1.plusYears(1);
        var aktivitetsavtale1 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1))
                .medProsentsats(BigDecimal.valueOf(10));
        var ansettelsesperiode1 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1));
        var yrkesaktivitet1 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(aktivitetsavtale1)
                .leggTilAktivitetsAvtale(ansettelsesperiode1)
                .build();

        var fom2 = SKJÆRINGSTIDSPUNKT.plusDays(2);
        var tom2 = fom2.plusYears(1);
        var aktivitetsavtale2 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2))
                .medProsentsats(BigDecimal.valueOf(25));
        var ansettelsesperiode2 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2));
        var yrkesaktivitet2 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(aktivitetsavtale2)
                .leggTilAktivitetsAvtale(ansettelsesperiode2)
                .build();

        // Act
        var stillingsprosent = UtledStillingsprosent.utled(new YrkesaktivitetFilter(null, List.of(yrkesaktivitet1, yrkesaktivitet2)),
                List.of(yrkesaktivitet1, yrkesaktivitet2), SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(stillingsprosent).isEqualTo(BigDecimal.valueOf(10));

    }

}

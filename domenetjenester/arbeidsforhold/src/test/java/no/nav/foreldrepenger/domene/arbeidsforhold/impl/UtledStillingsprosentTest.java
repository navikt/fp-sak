package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

public class UtledStillingsprosentTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();

    @Test
    public void skal_utlede_stillingsprosent_lik_0_når_ingen_yrkesaktiviteter() {
        // Arrange
        List<Yrkesaktivitet> yrkesaktiviteter = Collections.emptyList();
        // Act
        BigDecimal stillingsprosent = UtledStillingsprosent.utled(new YrkesaktivitetFilter(null, yrkesaktiviteter), yrkesaktiviteter, SKJÆRINGSTIDSPUNKT);
        // Assert
        assertThat(stillingsprosent).isEqualTo(BigDecimal.valueOf(0));
    }

    @Test
    public void skal_utlede_stillingsprosent_lik_75_når_en_yrkesaktivitet_overlapper_stp() {
        // Arrange
        LocalDate fom = SKJÆRINGSTIDSPUNKT.minusYears(2);
        LocalDate tom = SKJÆRINGSTIDSPUNKT.plusDays(1);
        AktivitetsAvtaleBuilder aktivitetsavtale = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
            .medProsentsats(BigDecimal.valueOf(75))
            .medAntallTimer(BigDecimal.valueOf(40))
            .medAntallTimerFulltid(BigDecimal.valueOf(40));
        AktivitetsAvtaleBuilder ansettelsesperiode = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        Yrkesaktivitet yrkesaktivitet = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aktivitetsavtale)
            .leggTilAktivitetsAvtale(ansettelsesperiode)
            .build();
        // Act
        BigDecimal stillingsprosent = UtledStillingsprosent.utled(new YrkesaktivitetFilter(Optional.empty(), yrkesaktivitet), yrkesaktivitet, SKJÆRINGSTIDSPUNKT);
        // Assert
        assertThat(stillingsprosent).isEqualTo(BigDecimal.valueOf(75));
    }

    @Test
    public void skal_utlede_stillingsprosent_lik_35_når_en_yrkesaktivitet_starter_etter_stp() {
        // Arrange
        LocalDate fom = SKJÆRINGSTIDSPUNKT.plusDays(1);
        LocalDate tom = fom.plusYears(2);
        AktivitetsAvtaleBuilder aktivitetsavtale = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
            .medProsentsats(BigDecimal.valueOf(35))
            .medAntallTimer(BigDecimal.valueOf(40))
            .medAntallTimerFulltid(BigDecimal.valueOf(40));
        AktivitetsAvtaleBuilder ansettelsesperiode = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        Yrkesaktivitet yrkesaktivitet = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aktivitetsavtale)
            .leggTilAktivitetsAvtale(ansettelsesperiode)
            .build();
        // Act
        BigDecimal stillingsprosent = UtledStillingsprosent.utled(new YrkesaktivitetFilter(Optional.empty(), yrkesaktivitet), yrkesaktivitet, SKJÆRINGSTIDSPUNKT);
        // Assert
        assertThat(stillingsprosent).isEqualTo(BigDecimal.valueOf(35));
    }

    @Test
    public void skal_utlede_stillingsprosent_for_yrkesaktivitet_med_seneste_fom_dato_når_flere_yrkesaktiviter_overlapper_stp() {

        // Arrange
        LocalDate fom1 = SKJÆRINGSTIDSPUNKT.minusYears(2);
        LocalDate tom1 = SKJÆRINGSTIDSPUNKT.plusDays(1);
        AktivitetsAvtaleBuilder aktivitetsavtale1 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1))
            .medProsentsats(BigDecimal.valueOf(10))
            .medAntallTimer(BigDecimal.valueOf(40))
            .medAntallTimerFulltid(BigDecimal.valueOf(40));
        AktivitetsAvtaleBuilder ansettelsesperiode1 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1));
        Yrkesaktivitet yrkesaktivitet1 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aktivitetsavtale1)
            .leggTilAktivitetsAvtale(ansettelsesperiode1)
            .build();

        LocalDate fom2 = SKJÆRINGSTIDSPUNKT.minusYears(1);
        LocalDate tom2 = SKJÆRINGSTIDSPUNKT.plusDays(1);
        AktivitetsAvtaleBuilder aktivitetsavtale2 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2))
            .medProsentsats(BigDecimal.valueOf(25))
            .medAntallTimer(BigDecimal.valueOf(40))
            .medAntallTimerFulltid(BigDecimal.valueOf(40));
        AktivitetsAvtaleBuilder ansettelsesperiode2 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2));
        Yrkesaktivitet yrkesaktivitet2 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aktivitetsavtale2)
            .leggTilAktivitetsAvtale(ansettelsesperiode2)
            .build();

        // Act
        BigDecimal stillingsprosent = UtledStillingsprosent.utled(new YrkesaktivitetFilter(null, List.of(yrkesaktivitet1, yrkesaktivitet2)), List.of(yrkesaktivitet1, yrkesaktivitet2), SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(stillingsprosent).isEqualTo(BigDecimal.valueOf(25));

    }

    @Test
    public void skal_utlede_stillingsprosent_for_yrkesaktivitet_med_tidligst_fom_dato_når_flere_yrkesaktiviter_tilkommer_etter_stp() {

        // Arrange
        LocalDate fom1 = SKJÆRINGSTIDSPUNKT.plusDays(1);
        LocalDate tom1 = fom1.plusYears(1);
        AktivitetsAvtaleBuilder aktivitetsavtale1 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1))
            .medProsentsats(BigDecimal.valueOf(10))
            .medAntallTimer(BigDecimal.valueOf(40))
            .medAntallTimerFulltid(BigDecimal.valueOf(40));
        AktivitetsAvtaleBuilder ansettelsesperiode1 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1));
        Yrkesaktivitet yrkesaktivitet1 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aktivitetsavtale1)
            .leggTilAktivitetsAvtale(ansettelsesperiode1)
            .build();

        LocalDate fom2 = SKJÆRINGSTIDSPUNKT.plusDays(2);
        LocalDate tom2 = fom2.plusYears(1);
        AktivitetsAvtaleBuilder aktivitetsavtale2 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2))
            .medProsentsats(BigDecimal.valueOf(25))
            .medAntallTimer(BigDecimal.valueOf(40))
            .medAntallTimerFulltid(BigDecimal.valueOf(40));
        AktivitetsAvtaleBuilder ansettelsesperiode2 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2));
        Yrkesaktivitet yrkesaktivitet2 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aktivitetsavtale2)
            .leggTilAktivitetsAvtale(ansettelsesperiode2)
            .build();

        // Act
        BigDecimal stillingsprosent = UtledStillingsprosent.utled(new YrkesaktivitetFilter(null, List.of(yrkesaktivitet1, yrkesaktivitet2)), List.of(yrkesaktivitet1, yrkesaktivitet2), SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(stillingsprosent).isEqualTo(BigDecimal.valueOf(10));

    }


}

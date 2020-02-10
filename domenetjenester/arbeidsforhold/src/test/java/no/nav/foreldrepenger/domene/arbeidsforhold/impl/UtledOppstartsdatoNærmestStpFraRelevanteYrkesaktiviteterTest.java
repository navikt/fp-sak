package no.nav.foreldrepenger.domene.arbeidsforhold.impl;


import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

public class UtledOppstartsdatoNærmestStpFraRelevanteYrkesaktiviteterTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();

    @Test
    public void skal_finne_max_fom_dato_for_yrkesaktivteter_som_overlapper_stp() {

        // Arrange
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet("1");
        InternArbeidsforholdRef internArbeidsforholdRef = InternArbeidsforholdRef.nyRef();

        LocalDate fom1 = SKJÆRINGSTIDSPUNKT.minusYears(1);
        Yrkesaktivitet yrkesaktivitet1 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdId(internArbeidsforholdRef)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, SKJÆRINGSTIDSPUNKT))
                .medProsentsats(BigDecimal.valueOf(100))
                .medAntallTimer(BigDecimal.valueOf(40))
                .medAntallTimerFulltid(BigDecimal.valueOf(40)))
            .build();

        LocalDate fom2 = SKJÆRINGSTIDSPUNKT.minusYears(2);
        LocalDate tom2 = SKJÆRINGSTIDSPUNKT.plusDays(1);
        Yrkesaktivitet yrkesaktivitet2 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdId(internArbeidsforholdRef)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2))
                .medProsentsats(BigDecimal.valueOf(100))
                .medAntallTimer(BigDecimal.valueOf(40))
                .medAntallTimerFulltid(BigDecimal.valueOf(40)))
            .build();

        LocalDate fom3 = SKJÆRINGSTIDSPUNKT.minusYears(3);
        LocalDate tom3 = SKJÆRINGSTIDSPUNKT.minusDays(1);
        Yrkesaktivitet yrkesaktivitet3 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdId(internArbeidsforholdRef)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom3, tom3))
                .medProsentsats(BigDecimal.valueOf(100))
                .medAntallTimer(BigDecimal.valueOf(40))
                .medAntallTimerFulltid(BigDecimal.valueOf(40)))
            .build();

        LocalDate fom4 = SKJÆRINGSTIDSPUNKT.plusDays(3);
        LocalDate tom4 = SKJÆRINGSTIDSPUNKT.plusYears(1);
        Yrkesaktivitet yrkesaktivitet4 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdId(internArbeidsforholdRef)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom4, tom4))
                .medProsentsats(BigDecimal.valueOf(100))
                .medAntallTimer(BigDecimal.valueOf(40))
                .medAntallTimerFulltid(BigDecimal.valueOf(40)))
            .build();

        List<Yrkesaktivitet> yrkesaktiviteter = List.of(yrkesaktivitet1, yrkesaktivitet2, yrkesaktivitet3, yrkesaktivitet4);

        // Act
        LocalDate fomDato = UtledOppstartsdatoNærmestStpFraRelevanteYrkesaktiviteter.utled(new YrkesaktivitetFilter(null, yrkesaktiviteter), yrkesaktiviteter, SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(fomDato).isEqualTo(fom1);

    }


    @Test
    public void skal_finne_min_fom_dato_for_yrkesaktivteter_som_tilkommer_etter_stp_når_ingen_overlapper_stp() {

        // Arrange
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet("1");
        InternArbeidsforholdRef internArbeidsforholdRef = InternArbeidsforholdRef.nyRef();

        LocalDate fom1 = SKJÆRINGSTIDSPUNKT.minusYears(1);
        LocalDate tom1 = SKJÆRINGSTIDSPUNKT.minusDays(1);
        Yrkesaktivitet yrkesaktivitet1 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdId(internArbeidsforholdRef)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1))
                .medProsentsats(BigDecimal.valueOf(100))
                .medAntallTimer(BigDecimal.valueOf(40))
                .medAntallTimerFulltid(BigDecimal.valueOf(40)))
            .build();

        LocalDate fom2 = SKJÆRINGSTIDSPUNKT.plusDays(1);
        LocalDate tom2 = SKJÆRINGSTIDSPUNKT.plusYears(1);
        Yrkesaktivitet yrkesaktivitet2 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdId(internArbeidsforholdRef)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2))
                .medProsentsats(BigDecimal.valueOf(100))
                .medAntallTimer(BigDecimal.valueOf(40))
                .medAntallTimerFulltid(BigDecimal.valueOf(40)))
            .build();

        LocalDate fom3 = SKJÆRINGSTIDSPUNKT.plusDays(2);
        LocalDate tom3 = SKJÆRINGSTIDSPUNKT.plusYears(2);
        Yrkesaktivitet yrkesaktivitet3 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdId(internArbeidsforholdRef)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom3, tom3))
                .medProsentsats(BigDecimal.valueOf(100))
                .medAntallTimer(BigDecimal.valueOf(40))
                .medAntallTimerFulltid(BigDecimal.valueOf(40)))
            .build();

        List<Yrkesaktivitet> yrkesaktiviteter = List.of(yrkesaktivitet1, yrkesaktivitet2, yrkesaktivitet3);

        // Act
        LocalDate fomDato = UtledOppstartsdatoNærmestStpFraRelevanteYrkesaktiviteter.utled(new YrkesaktivitetFilter(null, yrkesaktiviteter), yrkesaktiviteter, SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(fomDato).isEqualTo(fom2);

    }

}

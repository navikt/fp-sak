package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class UtledOppstartsdatoNærmestStpFraRelevanteYrkesaktiviteterTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();

    @Test
    public void skal_finne_max_fom_dato_for_yrkesaktivteter_som_overlapper_stp() {

        // Arrange
        var arbeidsgiver = Arbeidsgiver.virksomhet("1");
        var internArbeidsforholdRef = InternArbeidsforholdRef.nyRef();

        var fom1 = SKJÆRINGSTIDSPUNKT.minusYears(1);
        var yrkesaktivitet1 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(arbeidsgiver)
                .medArbeidsforholdId(internArbeidsforholdRef)
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                        .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, SKJÆRINGSTIDSPUNKT))
                        .medProsentsats(BigDecimal.valueOf(100)))
                .build();

        var fom2 = SKJÆRINGSTIDSPUNKT.minusYears(2);
        var tom2 = SKJÆRINGSTIDSPUNKT.plusDays(1);
        var yrkesaktivitet2 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(arbeidsgiver)
                .medArbeidsforholdId(internArbeidsforholdRef)
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                        .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2))
                        .medProsentsats(BigDecimal.valueOf(100)))
                .build();

        var fom3 = SKJÆRINGSTIDSPUNKT.minusYears(3);
        var tom3 = SKJÆRINGSTIDSPUNKT.minusDays(1);
        var yrkesaktivitet3 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(arbeidsgiver)
                .medArbeidsforholdId(internArbeidsforholdRef)
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                        .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom3, tom3))
                        .medProsentsats(BigDecimal.valueOf(100)))
                .build();

        var fom4 = SKJÆRINGSTIDSPUNKT.plusDays(3);
        var tom4 = SKJÆRINGSTIDSPUNKT.plusYears(1);
        var yrkesaktivitet4 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(arbeidsgiver)
                .medArbeidsforholdId(internArbeidsforholdRef)
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                        .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom4, tom4))
                        .medProsentsats(BigDecimal.valueOf(100)))
                .build();

        var yrkesaktiviteter = List.of(yrkesaktivitet1, yrkesaktivitet2, yrkesaktivitet3, yrkesaktivitet4);

        // Act
        var fomDato = UtledOppstartsdatoNærmestStpFraRelevanteYrkesaktiviteter.utled(new YrkesaktivitetFilter(null, yrkesaktiviteter),
                yrkesaktiviteter, SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(fomDato).isEqualTo(fom1);

    }

    @Test
    public void skal_finne_min_fom_dato_for_yrkesaktivteter_som_tilkommer_etter_stp_når_ingen_overlapper_stp() {

        // Arrange
        var arbeidsgiver = Arbeidsgiver.virksomhet("1");
        var internArbeidsforholdRef = InternArbeidsforholdRef.nyRef();

        var fom1 = SKJÆRINGSTIDSPUNKT.minusYears(1);
        var tom1 = SKJÆRINGSTIDSPUNKT.minusDays(1);
        var yrkesaktivitet1 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(arbeidsgiver)
                .medArbeidsforholdId(internArbeidsforholdRef)
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                        .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1))
                        .medProsentsats(BigDecimal.valueOf(100)))
                .build();

        var fom2 = SKJÆRINGSTIDSPUNKT.plusDays(1);
        var tom2 = SKJÆRINGSTIDSPUNKT.plusYears(1);
        var yrkesaktivitet2 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(arbeidsgiver)
                .medArbeidsforholdId(internArbeidsforholdRef)
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                        .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2))
                        .medProsentsats(BigDecimal.valueOf(100)))
                .build();

        var fom3 = SKJÆRINGSTIDSPUNKT.plusDays(2);
        var tom3 = SKJÆRINGSTIDSPUNKT.plusYears(2);
        var yrkesaktivitet3 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(arbeidsgiver)
                .medArbeidsforholdId(internArbeidsforholdRef)
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                        .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom3, tom3))
                        .medProsentsats(BigDecimal.valueOf(100)))
                .build();

        var yrkesaktiviteter = List.of(yrkesaktivitet1, yrkesaktivitet2, yrkesaktivitet3);

        // Act
        var fomDato = UtledOppstartsdatoNærmestStpFraRelevanteYrkesaktiviteter.utled(new YrkesaktivitetFilter(null, yrkesaktiviteter),
                yrkesaktiviteter, SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(fomDato).isEqualTo(fom2);

    }

}

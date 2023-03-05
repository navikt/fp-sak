package no.nav.foreldrepenger.ytelse.beregning.regelmodell.fastsett;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.ytelse.beregning.regelmodell.Beregningsresultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatGrunnlag;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatRegler;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Beregningsgrunnlag;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPrArbeidsforhold;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPrStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.uttakresultat.UttakAktivitet;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.uttakresultat.UttakResultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.uttakresultat.UttakResultatPeriode;
import no.nav.fpsak.tidsserie.LocalDateInterval;

class RegelFastsettBeregningsresultatSVPTest {
    private static final LocalDate START = LocalDate.now();
    private static final LocalDate MIDT_1 = LocalDate.now().plusWeeks(3);
    private static final LocalDate MIDT_2 = LocalDate.now().plusWeeks(10);
    private static final LocalDate SLUTT = LocalDate.now().plusWeeks(20);
    private static final LocalDateInterval PERIODE_1 = new LocalDateInterval(START, MIDT_1);
    private static final LocalDateInterval PERIODE_2 = new LocalDateInterval(MIDT_1.plusDays(1), MIDT_2);
    private static final LocalDateInterval PERIODE_3 = new LocalDateInterval(MIDT_2.plusDays(1), SLUTT);
    private static final Arbeidsforhold ARBEIDSFORHOLD_1 = Arbeidsforhold.nyttArbeidsforholdHosVirksomhet("111", UUID.randomUUID().toString());
    private static final Arbeidsforhold ARBEIDSFORHOLD_2 = Arbeidsforhold.nyttArbeidsforholdHosVirksomhet("222", UUID.randomUUID().toString());
    private static final Arbeidsforhold ARBEIDSFORHOLD_3 = Arbeidsforhold.nyttArbeidsforholdHosVirksomhet("333", UUID.randomUUID().toString());

    @Test
    void skalTesteSVPUttaksperioderMedForskjelligeAntallAktiviteter() {
        // Arrange
        var periodeMap = Map.of(
                PERIODE_1, List.of(ARBEIDSFORHOLD_1),
                PERIODE_2, List.of(ARBEIDSFORHOLD_1, ARBEIDSFORHOLD_2),
                PERIODE_3, List.of(ARBEIDSFORHOLD_1, ARBEIDSFORHOLD_2, ARBEIDSFORHOLD_3));
        var modell = opprettRegelmodell(periodeMap);
        var output = Beregningsresultat.opprett();

        // Act
        var resultat = BeregningsresultatRegler.fastsettBeregningsresultat(modell);

        // Assert
        var perioder = resultat.beregningsresultat().getBeregningsresultatPerioder();
        assertThat(perioder).hasSize(3);
        assertThat(perioder.get(0).getBeregningsresultatAndelList()).hasSize(2);
        assertThat(perioder.get(1).getBeregningsresultatAndelList()).hasSize(4);
        assertThat(perioder.get(2).getBeregningsresultatAndelList()).hasSize(6);

    }

    private BeregningsresultatGrunnlag opprettRegelmodell(Map<LocalDateInterval, List<Arbeidsforhold>> periodeMap) {
        var arbeidsforhold = periodeMap.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream())
                .distinct()
                .map(arb -> lagPrArbeidsforhold(1000, 500, arb))
                .collect(Collectors.toList());
        var beregningsgrunnlag = opprettBeregningsgrunnlag(arbeidsforhold);
        var uttakResultat = opprettUttak(periodeMap);
        return new BeregningsresultatGrunnlag(beregningsgrunnlag, uttakResultat);
    }

    private BeregningsgrunnlagPrArbeidsforhold lagPrArbeidsforhold(double dagsatsBruker, double dagsatsArbeidsgiver, Arbeidsforhold arbeidsforhold) {
        return BeregningsgrunnlagPrArbeidsforhold.opprett(arbeidsforhold, null)
            .medRedusertRefusjonPrÅr(BigDecimal.valueOf(260 * dagsatsArbeidsgiver))
            .medRedusertBrukersAndelPrÅr(BigDecimal.valueOf(260 * dagsatsBruker));
    }

    private Beregningsgrunnlag opprettBeregningsgrunnlag(List<BeregningsgrunnlagPrArbeidsforhold> ekstraArbeidsforhold) {
        var prStatus = new BeregningsgrunnlagPrStatus(AktivitetStatus.ATFL, ekstraArbeidsforhold);
        var periode = new BeregningsgrunnlagPeriode(START, LocalDate.MAX, List.of(prStatus));
        return Beregningsgrunnlag.enkelPeriode(periode);
    }

    private UttakResultat opprettUttak(Map<LocalDateInterval, List<Arbeidsforhold>> arbeidsforholdPerioder) {
        List<UttakResultatPeriode> periodeListe = new ArrayList<>();
        for (var arbPeriode : arbeidsforholdPerioder.entrySet()) {
            var periode = arbPeriode.getKey();
            var arbeidsforhold = arbPeriode.getValue();
            var uttakAktiviteter = lagUttakAktiviteter(BigDecimal.valueOf(100), BigDecimal.valueOf(100), arbeidsforhold);
            periodeListe.add(new UttakResultatPeriode(periode.getFomDato(), periode.getTomDato(), uttakAktiviteter, false));
        }
        return new UttakResultat(periodeListe);
    }

    private List<UttakAktivitet> lagUttakAktiviteter(BigDecimal stillingsgrad, BigDecimal utbetalingsgrad, List<Arbeidsforhold> arbeidsforholdList) {
        return arbeidsforholdList.stream()
                .map(arb -> new UttakAktivitet(stillingsgrad, null, utbetalingsgrad, arb, AktivitetStatus.ATFL, false, stillingsgrad))
                .collect(Collectors.toList());
    }
}

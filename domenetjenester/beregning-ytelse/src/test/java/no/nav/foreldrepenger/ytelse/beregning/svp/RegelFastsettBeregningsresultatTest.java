package no.nav.foreldrepenger.ytelse.beregning.svp;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import org.junit.Before;
import org.junit.Test;

import no.nav.foreldrepenger.ytelse.beregning.regelmodell.Beregningsresultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatRegelmodell;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakAktivitet;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakResultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakResultatPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Beregningsgrunnlag;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPrArbeidsforhold;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPrStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Periode;
import no.nav.foreldrepenger.ytelse.beregning.regler.RegelFastsettBeregningsresultat;
import no.nav.fpsak.tidsserie.LocalDateInterval;

public class RegelFastsettBeregningsresultatTest {
    private static final LocalDate START = LocalDate.now();
    private static final LocalDate MIDT_1 = LocalDate.now().plusWeeks(3);
    private static final LocalDate MIDT_2 = LocalDate.now().plusWeeks(10);
    private static final LocalDate SLUTT = LocalDate.now().plusWeeks(20);
    private static final LocalDateInterval PERIODE_1 = new LocalDateInterval(START, MIDT_1);
    private static final LocalDateInterval PERIODE_2 = new LocalDateInterval(MIDT_1.plusDays(1), MIDT_2);
    private static final LocalDateInterval PERIODE_3 = new LocalDateInterval(MIDT_2.plusDays(1), SLUTT);
    private static final Arbeidsforhold ARBEIDSFORHOLD_1 = Arbeidsforhold.nyttArbeidsforholdHosVirksomhet("111", InternArbeidsforholdRef.nyRef().getReferanse());
    private static final Arbeidsforhold ARBEIDSFORHOLD_2 = Arbeidsforhold.nyttArbeidsforholdHosVirksomhet("222", InternArbeidsforholdRef.nyRef().getReferanse());
    private static final Arbeidsforhold ARBEIDSFORHOLD_3 = Arbeidsforhold.nyttArbeidsforholdHosVirksomhet("333", InternArbeidsforholdRef.nyRef().getReferanse());

    private RegelFastsettBeregningsresultat regel;

    @Before
    public void setup() {
        regel = new RegelFastsettBeregningsresultat();
    }

    @Test
    public void skalTesteSVPUttaksperioderMedForskjelligeAntallAktiviteter() {
        // Arrange
        Map<LocalDateInterval, List<Arbeidsforhold>> periodeMap = Map.of(
            PERIODE_1, List.of(ARBEIDSFORHOLD_1),
            PERIODE_2, List.of(ARBEIDSFORHOLD_1, ARBEIDSFORHOLD_2),
            PERIODE_3, List.of(ARBEIDSFORHOLD_1, ARBEIDSFORHOLD_2, ARBEIDSFORHOLD_3)
        );
        BeregningsresultatRegelmodell modell = opprettRegelmodell(periodeMap);
        Beregningsresultat output = new Beregningsresultat();

        // Act
        regel.evaluer(modell, output);

        // Assert
        List<BeregningsresultatPeriode> perioder = output.getBeregningsresultatPerioder();
        assertThat(perioder).hasSize(3);
        assertThat(perioder.get(0).getBeregningsresultatAndelList()).hasSize(2);
        assertThat(perioder.get(1).getBeregningsresultatAndelList()).hasSize(4);
        assertThat(perioder.get(2).getBeregningsresultatAndelList()).hasSize(6);

    }


    private BeregningsresultatRegelmodell opprettRegelmodell(Map<LocalDateInterval, List<Arbeidsforhold>> periodeMap) {
        List<BeregningsgrunnlagPrArbeidsforhold> arbeidsforhold = periodeMap.entrySet().stream()
            .flatMap(entry -> entry.getValue().stream())
            .distinct()
            .map(arb -> lagPrArbeidsforhold(1000, 500, arb))
            .collect(Collectors.toList());
        Beregningsgrunnlag beregningsgrunnlag = opprettBeregningsgrunnlag(arbeidsforhold);
        UttakResultat uttakResultat = opprettUttak(periodeMap);
        return new BeregningsresultatRegelmodell(beregningsgrunnlag, uttakResultat);
    }

    private BeregningsgrunnlagPrArbeidsforhold lagPrArbeidsforhold(double dagsatsBruker, double dagsatsArbeidsgiver, Arbeidsforhold arbeidsforhold) {
        return BeregningsgrunnlagPrArbeidsforhold.builder()
            .medArbeidsforhold(arbeidsforhold)
            .medRedusertRefusjonPrÅr(BigDecimal.valueOf(260 * dagsatsArbeidsgiver))
            .medRedusertBrukersAndelPrÅr(BigDecimal.valueOf(260 * dagsatsBruker))
            .build();
    }

    private Beregningsgrunnlag opprettBeregningsgrunnlag(List<BeregningsgrunnlagPrArbeidsforhold> ekstraArbeidsforhold) {
        BeregningsgrunnlagPrStatus.Builder prStatusBuilder = BeregningsgrunnlagPrStatus.builder()
            .medAktivitetStatus(AktivitetStatus.ATFL);
        for (BeregningsgrunnlagPrArbeidsforhold arbeidsforhold : ekstraArbeidsforhold) {
            prStatusBuilder.medArbeidsforhold(arbeidsforhold);
        }
        BeregningsgrunnlagPrStatus prStatus = prStatusBuilder.build();
        BeregningsgrunnlagPeriode periode = BeregningsgrunnlagPeriode.builder()
            .medPeriode(Periode.of(START, LocalDate.MAX))
            .medBeregningsgrunnlagPrStatus(prStatus)
            .build();
        return Beregningsgrunnlag.builder()
            .medAktivitetStatuser(Collections.singletonList(AktivitetStatus.ATFL))
            .medSkjæringstidspunkt(LocalDate.now())
            .medBeregningsgrunnlagPeriode(periode)
            .build();
    }


    private UttakResultat opprettUttak(Map<LocalDateInterval, List<Arbeidsforhold>> arbeidsforholdPerioder) {
        List<UttakResultatPeriode> periodeListe = new ArrayList<>();
        for (var arbPeriode : arbeidsforholdPerioder.entrySet()) {
            LocalDateInterval periode = arbPeriode.getKey();
            List<Arbeidsforhold> arbeidsforhold = arbPeriode.getValue();
            List<UttakAktivitet> uttakAktiviteter = lagUttakAktiviteter(BigDecimal.valueOf(100), BigDecimal.valueOf(100), arbeidsforhold);
            periodeListe.add(new UttakResultatPeriode(periode.getFomDato(), periode.getTomDato(), uttakAktiviteter, false));
        }
        return new UttakResultat(periodeListe);
    }

    private List<UttakAktivitet> lagUttakAktiviteter(BigDecimal stillingsgrad, BigDecimal utbetalingsgrad, List<Arbeidsforhold> arbeidsforholdList) {
        return arbeidsforholdList.stream()
            .map(arb -> new UttakAktivitet(stillingsgrad, null, utbetalingsgrad, arb, AktivitetStatus.ATFL, false))
            .collect(Collectors.toList());
    }
}

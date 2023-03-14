package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.fp.OpptjeningsvilkårForeldrepenger;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.Resultat;
import no.nav.fpsak.nare.evaluation.summary.EvaluationSummary;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

class OpptjeningVilkårBeregnetOpptjeningTest {

    private final String ARBEID = OpptjeningsvilkårForeldrepenger.ARBEID;
    private final Aktivitet aktivitet = new Aktivitet(ARBEID, "BigCorp", Aktivitet.ReferanseType.ORGNR);

    @Test
    void skal_få_ikke_godkjent_for_beregnet_opptjening_med_mellomliggende_periode_og_for_kort_varighet() {
        var maksMellomliggendeDager = 14;

        var dt1 = LocalDate.of(2017, 10, 01);
        var dt2 = LocalDate.of(2017, 11, 20);
        var dt3 = dt2.plusDays(maksMellomliggendeDager / 2);
        var dt4 = dt3.plusDays(15);
        var endOfInntekt = dt1.plusMonths(2).minusDays(1);

        // matcher antatt godkjent kun for dt3-dt4
        var behandlingstidspunkt = LocalDate.of(2018, 01, 18);

        // skal også med som antatt selv om ingen inntekter er rapportert
        var aktiviteter = List.of(
            AktivitetPeriode.periodeTilVurdering(new LocalDateInterval(dt1, dt2), aktivitet),
            AktivitetPeriode.periodeTilVurdering(new LocalDateInterval(dt3, dt4), aktivitet)
        );
        // inntekt
        var inntekter = List.of(new InntektPeriode(new LocalDateInterval(dt1, endOfInntekt), aktivitet.forInntekt(), 1L));

        var grunnlag = new Opptjeningsgrunnlag(behandlingstidspunkt, dt1, dt4, aktiviteter, inntekter);

        // Act
        var output = new OpptjeningsvilkårResultat();
        var evaluation = new OpptjeningsvilkårForeldrepenger().evaluer(grunnlag, output);

        // Assert

        // sjekk underkjente perioder og antatt godkjent er tomme
        assertThat(output.getUnderkjentePerioder()).isEmpty();
        assertThat(output.getAntattGodkjentePerioder()).hasSize(1).containsEntry(aktivitet, new LocalDateTimeline<>(endOfInntekt.plusDays(1), dt4, Boolean.TRUE));


        assertThat(output.getAkseptertMellomliggendePerioder()).containsEntry(aktivitet, new LocalDateTimeline<>(dt2.plusDays(1), dt3.minusDays(1), Boolean.TRUE));

        assertThat(output.getResultatTidslinje()).isEqualTo(new LocalDateTimeline<>(dt1, endOfInntekt, Boolean.TRUE));
        assertThat(output.getResultatOpptjent()).isEqualTo(Period.ofMonths(2));

        var forventet = Resultat.NEI;
        assertForventetResultat(evaluation, forventet);


    }

    private void assertForventetResultat(Evaluation evaluation, Resultat forventet) {
        var summary = new EvaluationSummary(evaluation);
        var total = summary.leafEvaluations();
        assertThat(total).hasSize(1);
        assertThat(total.stream().map(Evaluation::result)).containsOnly(forventet);
    }

    @Test
    void skal_få_avslag_når_bekreftet_opptjening_er_5mnd_og_antatt_opptjening_er_6mnd() {
        var maksMellomliggendeDager = 14;

        var behandlingstidspunkt = LocalDate.now();
        var dt1 = behandlingstidspunkt.minusMonths(6).minusDays(3);
        var dt2 = behandlingstidspunkt.minusMonths(2).plusDays(2);
        var dt3 = dt2.plusDays(maksMellomliggendeDager / 2);
        var dt4 = behandlingstidspunkt.plusDays(100);

        var endOfInntekt = behandlingstidspunkt.minusMonths(1);


        // matcher antatt godkjent kun for dt3-dt4
        var aktiviteter = List.of(
            AktivitetPeriode.periodeTilVurdering(new LocalDateInterval(dt1, dt2), aktivitet),
            AktivitetPeriode.periodeTilVurdering(new LocalDateInterval(dt3, dt4), aktivitet)
        );
        // inntekt
        var inntekter = List.of(new InntektPeriode(new LocalDateInterval(dt1, endOfInntekt), aktivitet.forInntekt(), 1L));

        var grunnlag = new Opptjeningsgrunnlag(behandlingstidspunkt, dt1, behandlingstidspunkt, aktiviteter, inntekter);

        // Act
        var output = new OpptjeningsvilkårResultat();
        var evaluation = new OpptjeningsvilkårForeldrepenger().evaluer(grunnlag, output);

        var forventet = Resultat.NEI;
        assertForventetResultat(evaluation, forventet);

        // Assert

        // sjekk underkjente perioder og antatt godkjent er tomme
        assertThat(output.getUnderkjentePerioder()).isEmpty();
        //assertThat(output.getUnderkjentePerioder()).containsEntry(aktivitet, new LocalDateTimeline<>(behandlingstidspunkt.plusDays(1), dt4, Boolean.TRUE));

        assertThat(output.getAntattGodkjentePerioder()).hasSize(1).containsEntry(aktivitet, new LocalDateTimeline<>(endOfInntekt.plusDays(1), behandlingstidspunkt, Boolean.TRUE));


        assertThat(output.getAkseptertMellomliggendePerioder()).containsEntry(aktivitet, new LocalDateTimeline<>(dt2.plusDays(1), dt3.minusDays(1), Boolean.TRUE));

        assertThat(output.getResultatTidslinje()).isEqualTo(new LocalDateTimeline<>(dt1, endOfInntekt, Boolean.TRUE));

    }

    @Test
    void skal_få_ikke_oppfylt_når_bekreftet_opptjening_er_4mnd_og_antatt_opptjening_er_5mnd() {
        var maksMellomliggendeDager = 14;

        var behandlingstidspunkt = LocalDate.of(2018, 01, 18);
        // datoer tunet for å gi akkurat 4 måneder opptjening
        var dt1 = LocalDate.of(2017, 8, 24);
        var dt2 = LocalDate.of(2017, 11, 20);
        var dt3 = dt2.plusDays(maksMellomliggendeDager / 2);
        var dt4 = behandlingstidspunkt.plusDays(100);

        // dato tunet for å sikre at en måned kan settes som antatt opptjent (men fortsatt ikke nok til å gi Ok)
        var endOfInntekt = behandlingstidspunkt.minusMonths(1);


        // matcher antatt godkjent kun for dt3-dt4
        var aktiviteter = List.of(
            AktivitetPeriode.periodeTilVurdering(new LocalDateInterval(dt1, dt2), aktivitet),
            AktivitetPeriode.periodeTilVurdering(new LocalDateInterval(dt3, dt4), aktivitet)
        );
        // inntekt
        var inntekter = List.of(new InntektPeriode(new LocalDateInterval(dt1, endOfInntekt), aktivitet.forInntekt(), 1L));

        var grunnlag = new Opptjeningsgrunnlag(behandlingstidspunkt, dt1, behandlingstidspunkt, aktiviteter, inntekter);

        // Act
        var output = new OpptjeningsvilkårResultat();
        var evaluation = new OpptjeningsvilkårForeldrepenger().evaluer(grunnlag, output);

        var forventet = Resultat.NEI;
        assertForventetResultat(evaluation, forventet);

        // Assert

        // sjekk underkjente perioder og antatt godkjent er tomme
        assertThat(output.getUnderkjentePerioder()).isEmpty();
        //assertThat(output.getUnderkjentePerioder()).containsEntry(aktivitet, new LocalDateTimeline<>(behandlingstidspunkt.plusDays(1), dt4, Boolean.TRUE));

        assertThat(output.getAntattGodkjentePerioder()).hasSize(1).containsEntry(aktivitet, new LocalDateTimeline<>(endOfInntekt.plusDays(1), behandlingstidspunkt, Boolean.TRUE));


        assertThat(output.getAkseptertMellomliggendePerioder()).containsEntry(aktivitet, new LocalDateTimeline<>(dt2.plusDays(1), dt3.minusDays(1), Boolean.TRUE));

        assertThat(output.getResultatTidslinje()).isEqualTo(new LocalDateTimeline<>(dt1, endOfInntekt, Boolean.TRUE));
        assertThat(output.getResultatOpptjent()).isEqualTo(Period.ofMonths(4));

    }

}

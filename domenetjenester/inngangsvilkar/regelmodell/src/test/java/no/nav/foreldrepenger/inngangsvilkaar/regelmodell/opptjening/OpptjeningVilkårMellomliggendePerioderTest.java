package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.fp.OpptjeningsvilkårForeldrepenger;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

class OpptjeningVilkårMellomliggendePerioderTest {

    private final String ARBEID = OpptjeningsvilkårForeldrepenger.ARBEID;
    private final Aktivitet aktivitet = new Aktivitet(ARBEID, "BigCorp", Aktivitet.ReferanseType.ORGNR);

    @Test
    void skal_anse_mellomliggende_periode_mindre_enn_angitt_maks_med_foregående_periode_lenger_enn_anngitt_min_for_godtatt() {
        var maksMellomliggendeDager = 14;
        var minForegåendeDager = 4*7;

        var dt1 = LocalDate.of(2017, 10, 2);
        var dt2 = LocalDate.of(2017, 11, 7);
        var dt3 = dt2.plusDays(maksMellomliggendeDager).plusDays(1); // pluss 1 vil fortsatt gi for kort mellomliggende pga fom/tom
        var dt4 = dt3.plusDays(1);


        // matcher antatt godkjent kun for dt3-dt4
        var behandlingstidspunkt = LocalDate.of(2018, 1, 18);
        var aktiviteter = List.of(
            AktivitetPeriode.periodeTilVurdering(new LocalDateInterval(dt1, dt2), aktivitet),
            AktivitetPeriode.periodeTilVurdering(new LocalDateInterval(dt3, dt4), aktivitet)
        );
        // inntekt
        var inntekter = List.of(new InntektPeriode(new LocalDateInterval(dt1, dt4), aktivitet.forInntekt(), 1L));

        var grunnlag = new Opptjeningsgrunnlag(behandlingstidspunkt, dt1, dt4, aktiviteter, inntekter);

        // Act
        var output = new OpptjeningsvilkårResultat();
        new OpptjeningsvilkårForeldrepenger().evaluer(grunnlag, output);

        // Assert

        // sjekk underkjente perioder og antatt godkjent er tomme
        assertThat(output.getUnderkjentePerioder()).isEmpty();
        assertThat(output.getAntattGodkjentePerioder()).isEmpty();

        assertThat(output.getAkseptertMellomliggendePerioder()).containsEntry(aktivitet, new LocalDateTimeline<>(dt2.plusDays(1), dt3.minusDays(1), Boolean.TRUE));

    }

    @Test
    void skal_anse_mellomliggende_periode_over_maks_mellomliggende_dager_med_foregående_periode_lenger_enn_min_forgående_dager_for_ikke_medregnet() {
        var maksMellomliggendeDager = 14;

        var dt1 = LocalDate.of(2017, 10, 2);
        var dt2 = LocalDate.of(2017, 11, 7);
        var dt3 = dt2.plusDays(maksMellomliggendeDager).plusDays(2); // pluss 2 kompenserer for fom/tom og gir mellomliggende 15 dager
        var dt4 = dt3.plusDays(1);

        // matcher antatt godkjent kun for dt3-dt4
        var behandlingstidspunkt = LocalDate.of(2018, 1, 18);
        var aktiviteter = List.of(
            AktivitetPeriode.periodeTilVurdering(new LocalDateInterval(dt1, dt2), aktivitet),
            AktivitetPeriode.periodeTilVurdering(new LocalDateInterval(dt3, dt4), aktivitet)
        );
        // inntekt
        var inntekter = List.of(new InntektPeriode(new LocalDateInterval(dt1, dt4), aktivitet.forInntekt(), 1L));

        var grunnlag = new Opptjeningsgrunnlag(behandlingstidspunkt, dt1, dt4, aktiviteter, inntekter);

        // Act
        var output = new OpptjeningsvilkårResultat();
        new OpptjeningsvilkårForeldrepenger().evaluer(grunnlag, output);

        // Assert

        // sjekk underkjente perioder og antatt godkjent er tomme
        assertThat(output.getUnderkjentePerioder()).isEmpty();
        assertThat(output.getAntattGodkjentePerioder()).isEmpty();

        assertThat(output.getAkseptertMellomliggendePerioder()).isEmpty();

    }
}

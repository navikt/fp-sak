package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.fp.OpptjeningsvilkårForeldrepenger;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

public class OpptjeningVilkårAntattGodkjentOgUnderkjentArbeidTest {

    private final String ARBEID = OpptjeningsvilkårForeldrepenger.ARBEID;

    private final Aktivitet bigCorp=new Aktivitet(ARBEID, "BigCorp", Aktivitet.ReferanseType.ORGNR);
    private final Aktivitet smallCorp = new Aktivitet(ARBEID, "SmallCorp", Aktivitet.ReferanseType.ORGNR);
    private final Aktivitet noCorp = new Aktivitet(ARBEID, "NoCorp", Aktivitet.ReferanseType.ORGNR);

    @Test
    public void skal_beregne_underkjente_perioder_med_arbeid_ved_sammenligning_med_inntekt_grunnlag() throws Exception {
        var dt1 = LocalDate.of(2017, 9, 02);
        var dt2 = LocalDate.of(2017, 9, 07);
        var dt3 = LocalDate.of(2017, 10, 10);
        var dt4 = LocalDate.of(2017, 10, 15);

        var o1 = LocalDate.of(2017, 9, 03);
        var o2 = LocalDate.of(2017, 9, 11);

        // unngå antatt godkjent
        var behandlingstidspunkt = LocalDate.of(2018, 01, 18);
        var grunnlag = new Opptjeningsgrunnlag(behandlingstidspunkt, dt1, behandlingstidspunkt);

        // arbeid aktivitet
        grunnlag.leggTil(new LocalDateInterval(dt1, dt2), bigCorp);
        grunnlag.leggTil(new LocalDateInterval(dt3, dt4), bigCorp);
        grunnlag.leggTil(new LocalDateInterval(dt2, dt4), noCorp);

        // inntekt
        grunnlag.leggTilRapportertInntekt(new LocalDateInterval(dt3, dt4), bigCorp.forInntekt(), 1L);
        grunnlag.leggTilRapportertInntekt(new LocalDateInterval(o1, o2), bigCorp.forInntekt(), 0L);

        var output = new OpptjeningsvilkårResultat();
        new OpptjeningsvilkårForeldrepenger().evaluer(grunnlag, output);

        assertThat(output.getAntattGodkjentePerioder()).isEmpty();

        assertThat(output.getUnderkjentePerioder())
                .containsEntry(bigCorp, new LocalDateTimeline<>(dt1, dt2, Boolean.TRUE))
                .containsEntry(noCorp, new LocalDateTimeline<>(dt2, dt4, Boolean.TRUE));

    }

    @Test
    public void skal_beregne_antatt_godkjent_arbeid() throws Exception {
        var dt1 = LocalDate.of(2017, 11, 02);
        var dt2 = LocalDate.of(2017, 11, 07);
        var dt3 = LocalDate.of(2017, 12, 10);
        var dt4 = LocalDate.of(2017, 12, 15);

        // matcher antatt godkjent kun for dt3-dt4
        var behandlingstidspunkt = LocalDate.of(2018, 01, 01);
        var grunnlag = new Opptjeningsgrunnlag(behandlingstidspunkt, dt1, dt4);

        // arbeid aktivitet
        grunnlag.leggTil(new LocalDateInterval(dt1, dt2), bigCorp);
        grunnlag.leggTil(new LocalDateInterval(dt3, dt4), bigCorp);

        // skal også med som antatt selv om ingen inntekter er rapportert
        var førsteArbeidsdagSmallCorp = dt3.withDayOfMonth(1);
        var sisteArbeidsdagSmallCorp = dt4;
        grunnlag.leggTil(new LocalDateInterval(førsteArbeidsdagSmallCorp, sisteArbeidsdagSmallCorp), smallCorp);

        grunnlag.leggTilRapportertInntekt(new LocalDateInterval(dt1, dt3), bigCorp.forInntekt(), 1L);

        var output = new OpptjeningsvilkårResultat();
        new OpptjeningsvilkårForeldrepenger().evaluer(grunnlag, output);

        assertThat(output.getUnderkjentePerioder()).isEmpty();

        assertThat(output.getAntattGodkjentePerioder())
                .containsEntry(bigCorp,
                        new LocalDateTimeline<>(dt3.plusDays(1), dt4, Boolean.TRUE))
                .containsEntry(smallCorp,
                        new LocalDateTimeline<>(førsteArbeidsdagSmallCorp, sisteArbeidsdagSmallCorp, Boolean.TRUE));

    }

    @Test
    public void skal_beregne_antatt_godkjent_over_underkjent_arbeid_der_de_overlapper() throws Exception {
        var dt1 = LocalDate.of(2017, 10, 02);
        var dt2 = LocalDate.of(2017, 10, 07);
        var dt3 = LocalDate.of(2017, 12, 10);
        var dt4 = LocalDate.of(2017, 12, 15);

        // matcher antatt godkjent kun for dt3-dt4
        var behandlingstidspunkt = LocalDate.of(2018, 01, 18);
        var grunnlag = new Opptjeningsgrunnlag(behandlingstidspunkt, dt1, behandlingstidspunkt);

        // arbeid aktivitet
        grunnlag.leggTil(new LocalDateInterval(dt1, dt2), bigCorp);
        grunnlag.leggTil(new LocalDateInterval(dt3, dt4), bigCorp);

        // skal også med som antatt selv om ingen inntekter er rapportert
        var førsteArbeidsdagSmallCorp = dt2;
        var sisteArbeidsdagSmallCorp = dt4;
        grunnlag.leggTil(new LocalDateInterval(førsteArbeidsdagSmallCorp, sisteArbeidsdagSmallCorp), smallCorp);

        // Act
        var output = new OpptjeningsvilkårResultat();
        new OpptjeningsvilkårForeldrepenger().evaluer(grunnlag, output);

        var førsteAntattGodkjenteDag = behandlingstidspunkt.plusMonths(1).minus(Period.ofMonths(2)).withDayOfMonth(1);

        // Assert

        // sjekk underkjente perioder
        assertThat(output.getUnderkjentePerioder())
                .containsEntry(bigCorp, new LocalDateTimeline<>(dt1, dt2, Boolean.TRUE))
                .containsEntry(smallCorp, new LocalDateTimeline<>(dt2, førsteAntattGodkjenteDag.minusDays(1), Boolean.TRUE))
                ;

        // sjekk antatt godkjente perioder
        assertThat(output.getAntattGodkjentePerioder())
                .containsEntry(bigCorp, new LocalDateTimeline<>(dt3, dt4, Boolean.TRUE))
                .containsEntry(smallCorp,
                        new LocalDateTimeline<>(førsteAntattGodkjenteDag, dt4, Boolean.TRUE));

        // sjekk at antatt og underkjent arbeid aldri overlapper
        for (var entry : output.getUnderkjentePerioder().entrySet()) {
            var other = output.getAntattGodkjentePerioder().get(entry.getKey());
            assertThat(entry.getValue().intersects(other)).as("Skal ikke intersecte for " + entry.getKey()).isFalse();
        }

    }
}

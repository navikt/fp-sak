package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.fp.OpptjeningsvilkårForeldrepenger;
import no.nav.fpsak.tidsserie.LocalDateInterval;

class OpptjeningsvilkårMellomregningTest {

    @Test
    void skal_håndtere_overlappende_perioder() {
        var aktivitet = new Aktivitet(OpptjeningsvilkårForeldrepenger.ARBEID, "123", Aktivitet.ReferanseType.ORGNR);

        var aktiviteter = List.of(
            AktivitetPeriode.periodeTilVurdering(LocalDateInterval.withPeriodAfterDate(LocalDate.now().minusMonths(8), Period.ofWeeks(6)), aktivitet),
            AktivitetPeriode.periodeTilVurdering(LocalDateInterval.withPeriodAfterDate(LocalDate.now().minusMonths(7), Period.ofMonths(6)), aktivitet),
            AktivitetPeriode.periodeTilVurdering(LocalDateInterval.withPeriodAfterDate(LocalDate.now().minusMonths(2), Period.ofWeeks(4)), aktivitet)
        );

        var grunnlag = new Opptjeningsgrunnlag(LocalDate.now(), LocalDate.now().minusMonths(10), LocalDate.now(), aktiviteter, List.of());

        var mellomregning = new OpptjeningsvilkårMellomregning(grunnlag, OpptjeningsvilkårParametre.opptjeningsparametreForeldrepenger());

        assertThat(mellomregning.getAktivitetTidslinjer(true, true)).isNotEmpty();
    }
}

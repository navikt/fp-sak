package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.fp.OpptjeningsvilkårForeldrepenger;
import no.nav.fpsak.tidsserie.LocalDateInterval;

public class OpptjeningsvilkårMellomregningTest {

    @Test
    public void skal_håndtere_overlappende_perioder() {
        final Opptjeningsgrunnlag grunnlag = new Opptjeningsgrunnlag(LocalDate.now(), LocalDate.now().minusMonths(10), LocalDate.now());
        final Aktivitet aktivitet = new Aktivitet(OpptjeningsvilkårForeldrepenger.ARBEID, "123", Aktivitet.ReferanseType.ORGNR);

        grunnlag.leggTil(LocalDateInterval.withPeriodAfterDate(LocalDate.now().minusMonths(8), Period.ofWeeks(6)), aktivitet);
        grunnlag.leggTil(LocalDateInterval.withPeriodAfterDate(LocalDate.now().minusMonths(7), Period.ofMonths(6)), aktivitet);
        grunnlag.leggTil(LocalDateInterval.withPeriodAfterDate(LocalDate.now().minusMonths(2), Period.ofWeeks(4)), aktivitet);

        final OpptjeningsvilkårMellomregning mellomregning = new OpptjeningsvilkårMellomregning(grunnlag);

        assertThat(mellomregning.getAktivitetTidslinjer(true, true)).isNotEmpty();
    }
}

package no.nav.foreldrepenger.ytelse.beregning;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;

public class FinnEndringsdatoMellomPeriodeListerTest_NoMock {

    private FinnEndringsdatoMellomPeriodeLister finnEndringsdatoMellomPeriodeLister = new FinnEndringsdatoMellomPeriodeLister(
            new SjekkForEndringMellomPerioder(
                    new SjekkForIngenAndelerOgAndelerUtenDagsats(),
                    new SjekkOmPerioderHarEndringIAndeler()));

    @Test
    public void splittet_perioder_uten_endring_i_andeler_skal_ikke_gi_endringsdato() {

        var originalEntitet = lagEntitet();
        var revurderingEntitet = lagEntitet();

        byggPeriodeOgAndel(originalEntitet, LocalDate.now(), LocalDate.now().plusMonths(2), 100);
        byggPeriodeOgAndel(revurderingEntitet, LocalDate.now(), LocalDate.now().plusMonths(1).minusDays(1), 100);
        byggPeriodeOgAndel(revurderingEntitet, LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(2), 100);

        var endringsdato = finnEndringsdatoMellomPeriodeLister.finnEndringsdato(revurderingEntitet.getBeregningsresultatPerioder(),
                originalEntitet.getBeregningsresultatPerioder());

        assertThat(endringsdato).isEmpty();
    }

    @Test
    public void splittet_perioder_med_endring_i_andeler_skal_gi_endringsdato() {

        var originalEntitet = lagEntitet();
        var revurderingEntitet = lagEntitet();

        byggPeriodeOgAndel(originalEntitet, LocalDate.now(), LocalDate.now().plusMonths(2), 100);
        byggPeriodeOgAndel(revurderingEntitet, LocalDate.now(), LocalDate.now().plusMonths(1).minusDays(1), 100);
        byggPeriodeOgAndel(revurderingEntitet, LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(2), 50);

        var endringsdato = finnEndringsdatoMellomPeriodeLister.finnEndringsdato(revurderingEntitet.getBeregningsresultatPerioder(),
                originalEntitet.getBeregningsresultatPerioder());

        assertThat(endringsdato.get()).isEqualTo(LocalDate.now().plusMonths(1));
    }

    @Test
    public void perioder_med_ulik_fom_uten_endring_i_andeler_skal_gi_endringsdato() {

        var originalEntitet = lagEntitet();
        var revurderingEntitet = lagEntitet();

        byggPeriodeOgAndel(originalEntitet, LocalDate.now(), LocalDate.now().plusMonths(2), 100);
        byggPeriodeOgAndel(revurderingEntitet, LocalDate.now().minusDays(1), LocalDate.now().plusMonths(2), 100);

        var endringsdato = finnEndringsdatoMellomPeriodeLister.finnEndringsdato(revurderingEntitet.getBeregningsresultatPerioder(),
                originalEntitet.getBeregningsresultatPerioder());

        assertThat(endringsdato.get()).isEqualTo(LocalDate.now().minusDays(1));
    }

    private BeregningsresultatEntitet lagEntitet() {
        return BeregningsresultatEntitet.builder()
                .medRegelInput("regelinput")
                .medRegelSporing("regelsporing")
                .build();
    }

    private void byggPeriodeOgAndel(BeregningsresultatEntitet originalEntitet, LocalDate fom, LocalDate tom, int dagsats) {
        var originalPeriode = BeregningsresultatPeriode.builder()
                .medBeregningsresultatPeriodeFomOgTom(fom, tom)
                .build(originalEntitet);
        BeregningsresultatAndel.builder()
                .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
                .medArbeidsgiver(Arbeidsgiver.virksomhet("126387123"))
                .medDagsats(dagsats)
                .medStillingsprosent(BigDecimal.valueOf(100))
                .medUtbetalingsgrad(BigDecimal.valueOf(100))
                .medBrukerErMottaker(false)
                .medDagsatsFraBg(100)
                .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                .build(originalPeriode);
    }
}

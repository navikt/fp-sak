package no.nav.foreldrepenger.datavarehus.v2;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;

class StønadsstatistikkUtbetalingPeriodeMapperTest {

    @Test
    void mapper_tilkjent_henter_dagsats_fra_andel_med_dagsats() {
        var beregningsresultatPeriode = new BeregningsresultatPeriode.Builder()
            .medBeregningsresultatPeriodeFomOgTom(LocalDate.of(2023, 12, 9), LocalDate.of(2023, 12, 20))
            .build(new BeregningsresultatEntitet());
        var orgnr = "123";
        new BeregningsresultatAndel.Builder().medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr))
            .medDagsats(0)
            .medBrukerErMottaker(true)
            .medDagsatsFraBg(0)
            .medArbeidsforholdType(OpptjeningAktivitetType.ARBEID)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(50))
            .build(beregningsresultatPeriode);
        var andel2 = new BeregningsresultatAndel.Builder().medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr))
            .medDagsats(1000)
            .medBrukerErMottaker(false)
            .medDagsatsFraBg(2000)
            .medArbeidsforholdType(OpptjeningAktivitetType.ARBEID)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(50))
            .build(beregningsresultatPeriode);
        var utbetalingsperioder = StønadsstatistikkUtbetalingPeriodeMapper.mapTilkjent(
            StønadsstatistikkVedtak.YtelseType.FORELDREPENGER, StønadsstatistikkVedtak.HendelseType.FØDSEL, List.of(beregningsresultatPeriode));

        assertThat(utbetalingsperioder).hasSize(1);
        var stønadsstatistikkUtbetalingPeriode = utbetalingsperioder.get(0);
        assertThat(stønadsstatistikkUtbetalingPeriode.fom()).isEqualTo(beregningsresultatPeriode.getBeregningsresultatPeriodeFom());
        assertThat(stønadsstatistikkUtbetalingPeriode.tom()).isEqualTo(beregningsresultatPeriode.getBeregningsresultatPeriodeTom());
        assertThat(stønadsstatistikkUtbetalingPeriode.klasseKode()).isEqualTo(KodeKlassifik.FPF_REFUSJON_AG.getKode());
        assertThat(stønadsstatistikkUtbetalingPeriode.arbeidsgiver()).isEqualTo(orgnr);
        assertThat(stønadsstatistikkUtbetalingPeriode.dagsats()).isEqualTo(andel2.getDagsats());
        assertThat(stønadsstatistikkUtbetalingPeriode.dagsatsFraBeregningsgrunnlag()).isEqualTo(andel2.getDagsatsFraBg());
        assertThat(stønadsstatistikkUtbetalingPeriode.utbetalingsgrad()).isEqualTo(andel2.getUtbetalingsgrad());

    }

    @Test
    void mapper_slår_sammen_like_perioder_gjennom_helg() {
        var beregningsresultatPeriode1 = new BeregningsresultatPeriode.Builder()
            .medBeregningsresultatPeriodeFomOgTom(LocalDate.of(2023, 12, 9), LocalDate.of(2023, 12, 15))
            .build(new BeregningsresultatEntitet());
        var orgnr = "123";
        new BeregningsresultatAndel.Builder().medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr))
            .medDagsats(1000)
            .medBrukerErMottaker(true)
            .medDagsatsFraBg(2000)
            .medArbeidsforholdType(OpptjeningAktivitetType.ARBEID)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(50))
            .build(beregningsresultatPeriode1);
        var beregningsresultatPeriode2 = new BeregningsresultatPeriode.Builder()
            .medBeregningsresultatPeriodeFomOgTom(LocalDate.of(2023, 12, 18), LocalDate.of(2023, 12, 22))
            .build(new BeregningsresultatEntitet());
        var andel2 = new BeregningsresultatAndel.Builder().medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr))
            .medDagsats(1000)
            .medBrukerErMottaker(true)
            .medDagsatsFraBg(2000)
            .medArbeidsforholdType(OpptjeningAktivitetType.ARBEID)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(50))
            .build(beregningsresultatPeriode2);
        var utbetalingsperioder = StønadsstatistikkUtbetalingPeriodeMapper.mapTilkjent(
            StønadsstatistikkVedtak.YtelseType.FORELDREPENGER, StønadsstatistikkVedtak.HendelseType.FØDSEL,
            List.of(beregningsresultatPeriode1, beregningsresultatPeriode2));

        assertThat(utbetalingsperioder).hasSize(1);
        var stønadsstatistikkUtbetalingPeriode = utbetalingsperioder.get(0);
        assertThat(stønadsstatistikkUtbetalingPeriode.fom()).isEqualTo(beregningsresultatPeriode1.getBeregningsresultatPeriodeFom());
        assertThat(stønadsstatistikkUtbetalingPeriode.tom()).isEqualTo(beregningsresultatPeriode2.getBeregningsresultatPeriodeTom());
        assertThat(stønadsstatistikkUtbetalingPeriode.klasseKode()).isEqualTo(KodeKlassifik.FPF_ARBEIDSTAKER.getKode());
        assertThat(stønadsstatistikkUtbetalingPeriode.arbeidsgiver()).isEqualTo(orgnr);
        assertThat(stønadsstatistikkUtbetalingPeriode.dagsats()).isEqualTo(andel2.getDagsats());
        assertThat(stønadsstatistikkUtbetalingPeriode.dagsatsFraBeregningsgrunnlag()).isEqualTo(andel2.getDagsatsFraBg());
        assertThat(stønadsstatistikkUtbetalingPeriode.utbetalingsgrad()).isEqualTo(andel2.getUtbetalingsgrad());

    }
}

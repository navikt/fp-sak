package no.nav.foreldrepenger.ytelse.beregning.endringsdato;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.ytelse.beregning.endringsdato.regelmodell.BeregningsresultatAndelEndringModell;
import no.nav.foreldrepenger.ytelse.beregning.endringsdato.regelmodell.BeregningsresultatEndringModell;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MapBeregningsresultatTilEndringsmodellTest {

    @Test
    public void skal_teste_mapping_uten_arbeidsgiver() {
        BeregningsresultatEntitet entitet = lagGrunnlag(null);

        BeregningsresultatEndringModell regelmodell = new MapBeregningsresultatTilEndringsmodell(entitet).map();

        assertThat(regelmodell.getBeregningsresultatperioder()).hasSize(entitet.getBeregningsresultatPerioder().size());
        assertThat(regelmodell.getBeregningsresultatperioder().get(0).getFom()).isEqualTo(entitet.getBeregningsresultatPerioder().get(0).getBeregningsresultatPeriodeFom());
        assertThat(regelmodell.getBeregningsresultatperioder().get(0).getTom()).isEqualTo(entitet.getBeregningsresultatPerioder().get(0).getBeregningsresultatPeriodeTom());
        List<BeregningsresultatAndelEndringModell> regelAndeler = regelmodell.getBeregningsresultatperioder().get(0).getAndeler();
        List<BeregningsresultatAndel> andeler = entitet.getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList();
        assertThat(regelAndeler).hasSize(andeler.size());
        assertThat(regelAndeler.get(0).getArbeidsgiver()).isEqualTo(andeler.get(0).getArbeidsgiver().orElse(null));
        assertThat(regelAndeler.get(0).getDagsats()).isEqualTo(andeler.get(0).getDagsats());
        assertThat(regelAndeler.get(0).getAktivitetStatus()).isEqualTo(andeler.get(0).getAktivitetStatus());
        assertThat(regelAndeler.get(0).getInntektskategori()).isEqualTo(andeler.get(0).getInntektskategori());
        assertThat(regelAndeler.get(0).getArbeidsforholdReferanse()).isEqualTo(andeler.get(0).getArbeidsforholdRef());
        assertThat(regelAndeler.get(0).erBrukerMottaker()).isEqualTo(andeler.get(0).erBrukerMottaker());

        assertThat(regelmodell.getFeriepenger()).isPresent();
        assertThat(regelmodell.getFeriepenger().get().getFeriepengeperiodeFom()).isEqualTo(entitet.getBeregningsresultatFeriepenger().get().getFeriepengerPeriodeFom());
        assertThat(regelmodell.getFeriepenger().get().getFeriepengerperiodeTom()).isEqualTo(entitet.getBeregningsresultatFeriepenger().get().getFeriepengerPeriodeTom());
        assertThat(regelmodell.getFeriepenger().get().getFeriepengerPrÅrListe()).hasSize(entitet.getBeregningsresultatFeriepenger().get().getBeregningsresultatFeriepengerPrÅrListe().size());
        assertThat(regelmodell.getFeriepenger().get().getFeriepengerPrÅrListe().get(0).getOpptjeningsår().getValue())
            .isEqualTo(entitet.getBeregningsresultatFeriepenger().get().getBeregningsresultatFeriepengerPrÅrListe().get(0).getOpptjeningsår().getYear());
        assertThat(regelmodell.getFeriepenger().get().getFeriepengerPrÅrListe().get(0).getÅrsbeløp())
            .isEqualTo(entitet.getBeregningsresultatFeriepenger().get().getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp());

    }

    @Test
    public void skal_teste_mapping_med_arbeidsgiver() {
        BeregningsresultatEntitet entitet = lagGrunnlag(Arbeidsgiver.virksomhet("999999999"));

        BeregningsresultatEndringModell regelmodell = new MapBeregningsresultatTilEndringsmodell(entitet).map();

        assertThat(regelmodell.getBeregningsresultatperioder()).hasSize(entitet.getBeregningsresultatPerioder().size());
        assertThat(regelmodell.getBeregningsresultatperioder().get(0).getFom()).isEqualTo(entitet.getBeregningsresultatPerioder().get(0).getBeregningsresultatPeriodeFom());
        assertThat(regelmodell.getBeregningsresultatperioder().get(0).getTom()).isEqualTo(entitet.getBeregningsresultatPerioder().get(0).getBeregningsresultatPeriodeTom());
        List<BeregningsresultatAndelEndringModell> regelAndeler = regelmodell.getBeregningsresultatperioder().get(0).getAndeler();
        List<BeregningsresultatAndel> andeler = entitet.getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList();
        assertThat(regelAndeler).hasSize(andeler.size());
        assertThat(regelAndeler.get(0).getArbeidsgiver()).isEqualTo(andeler.get(0).getArbeidsgiver().orElse(null));
        assertThat(regelAndeler.get(0).getDagsats()).isEqualTo(andeler.get(0).getDagsats());
        assertThat(regelAndeler.get(0).getAktivitetStatus()).isEqualTo(andeler.get(0).getAktivitetStatus());
        assertThat(regelAndeler.get(0).getInntektskategori()).isEqualTo(andeler.get(0).getInntektskategori());
        assertThat(regelAndeler.get(0).getArbeidsforholdReferanse()).isEqualTo(andeler.get(0).getArbeidsforholdRef());
        assertThat(regelAndeler.get(0).erBrukerMottaker()).isEqualTo(andeler.get(0).erBrukerMottaker());

        assertThat(regelmodell.getFeriepenger()).isPresent();
        assertThat(regelmodell.getFeriepenger().get().getFeriepengeperiodeFom()).isEqualTo(entitet.getBeregningsresultatFeriepenger().get().getFeriepengerPeriodeFom());
        assertThat(regelmodell.getFeriepenger().get().getFeriepengerperiodeTom()).isEqualTo(entitet.getBeregningsresultatFeriepenger().get().getFeriepengerPeriodeTom());
        assertThat(regelmodell.getFeriepenger().get().getFeriepengerPrÅrListe()).hasSize(entitet.getBeregningsresultatFeriepenger().get().getBeregningsresultatFeriepengerPrÅrListe().size());
        assertThat(regelmodell.getFeriepenger().get().getFeriepengerPrÅrListe().get(0).getOpptjeningsår().getValue())
            .isEqualTo(entitet.getBeregningsresultatFeriepenger().get().getBeregningsresultatFeriepengerPrÅrListe().get(0).getOpptjeningsår().getYear());
        assertThat(regelmodell.getFeriepenger().get().getFeriepengerPrÅrListe().get(0).getÅrsbeløp())
            .isEqualTo(entitet.getBeregningsresultatFeriepenger().get().getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp());
    }


    private BeregningsresultatEntitet lagGrunnlag(Arbeidsgiver arbeidsgiver) {
        BeregningsresultatEntitet grunnlag = BeregningsresultatEntitet.builder()
            .medRegelInput("regelinput")
            .medRegelSporing("Regelsporing")
            .build();
        BeregningsresultatPeriode periode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(LocalDate.of(2020,1,1), LocalDate.of(2021,1,1))
            .build(grunnlag);
        BeregningsresultatAndel andel = BeregningsresultatAndel.builder()
            .medDagsats(500)
            .medBrukerErMottaker(true)
            .medArbeidsgiver(arbeidsgiver)
            .medAktivitetStatus(arbeidsgiver == null ? AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE : AktivitetStatus.ARBEIDSTAKER)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medInntektskategori(arbeidsgiver == null ? Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE : Inntektskategori.ARBEIDSTAKER)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medArbeidsforholdType(OpptjeningAktivitetType.ARBEID)
            .medDagsatsFraBg(500)
            .build(periode);

        BeregningsresultatFeriepenger feriepenger = BeregningsresultatFeriepenger.builder()
            .medFeriepengerRegelInput("")
            .medFeriepengerRegelSporing("")
            .build(grunnlag);
        BeregningsresultatFeriepengerPrÅr.builder()
            .medOpptjeningsår(2020)
            .medÅrsbeløp(45000)
            .build(feriepenger, andel);

        return grunnlag;
    }
}
